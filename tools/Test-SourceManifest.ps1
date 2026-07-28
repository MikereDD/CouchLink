[CmdletBinding()]
param(
    [ValidateNotNullOrEmpty()]
    [string]$Version = '1.3.1-dev.6.4',

    [string]$RootPath = (Join-Path $PSScriptRoot '..')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = [System.IO.Path]::GetFullPath($RootPath)
$manifestPath = Join-Path $root "SOURCE-MANIFEST-v$Version.sha256"

if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
    throw "Source manifest not found: $manifestPath"
}

$failures = [System.Collections.Generic.List[string]]::new()
$entryCount = 0
foreach ($line in Get-Content -LiteralPath $manifestPath) {
    if ([string]::IsNullOrWhiteSpace($line)) {
        continue
    }
    if ($line -notmatch '^([0-9a-fA-F]{64})  (.+)$') {
        $failures.Add("Malformed manifest line: $line")
        continue
    }

    $entryCount++
    $expected = $Matches[1].ToLowerInvariant()
    $relative = $Matches[2] -replace '^\./', ''
    $path = Join-Path $root ($relative.Replace('/', [System.IO.Path]::DirectorySeparatorChar))
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        $failures.Add("Missing file: $relative")
        continue
    }

    $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
        $failures.Add("Hash mismatch: $relative")
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    throw "Source manifest verification failed with $($failures.Count) error(s)."
}

Write-Host "Source manifest verified: $entryCount files" -ForegroundColor Green
