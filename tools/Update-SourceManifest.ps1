[CmdletBinding()]
param(
    [ValidateNotNullOrEmpty()]
    [string]$Version = '1.2-dev.6',

    [string]$RootPath = (Join-Path $PSScriptRoot '..')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$root = [System.IO.Path]::GetFullPath($RootPath)
$manifestPath = Join-Path $root "SOURCE-MANIFEST-v$Version.sha256"
$excludedDirectories = @('.git', '.gradle', '.idea', '.vs', 'bin', 'obj', 'build', 'publish', 'signing')

function Test-ManifestFileExcluded {
    param([Parameter(Mandatory)][string]$RelativePath)

    $normalized = $RelativePath.Replace('\', '/')
    $segments = $normalized.Split('/', [System.StringSplitOptions]::RemoveEmptyEntries)
    if ($segments.Length -gt 1) {
        foreach ($index in 0..($segments.Length - 2)) {
            $segment = $segments[$index]
            if ($excludedDirectories -contains $segment) {
                return $true
            }
            if ($segment -eq 'release' -and ($index -eq 0 -or $segments[$index - 1] -ne 'docs')) {
                return $true
            }
        }
    }

    $name = [System.IO.Path]::GetFileName($normalized)
    if ($name -eq [System.IO.Path]::GetFileName($manifestPath)) {
        return $true
    }
    if ($name -in @('local.properties', 'keystore.properties', 'Build-Release-error.log', 'Thumbs.db', '.DS_Store')) {
        return $true
    }
    foreach ($pattern in @('*.apk', '*.aab', '*.jks', '*.keystore', '*.pfx', '*.p12', '*.user', '*.suo', '*.zip')) {
        if ($name -like $pattern) {
            return $true
        }
    }
    return $false
}

$lines = Get-ChildItem -LiteralPath $root -File -Recurse -Force |
    ForEach-Object {
        $relativePath = [System.IO.Path]::GetRelativePath($root, $_.FullName)
        if (-not (Test-ManifestFileExcluded -RelativePath $relativePath)) {
            $normalizedPath = './' + $relativePath.Replace('\', '/')
            $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
            "$hash  $normalizedPath"
        }
    } |
    Sort-Object

$lines | Set-Content -LiteralPath $manifestPath -Encoding ascii
Write-Host "Source manifest updated: $manifestPath" -ForegroundColor Green
Write-Host "Entries: $($lines.Count)"
