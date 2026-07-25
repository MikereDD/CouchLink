[CmdletBinding()]
param(
    [string]$ApkPath = (Join-Path $PSScriptRoot '..\release\CouchLink-v1.1.apk'),

    [ValidatePattern('^[0-9a-fA-F]{64}$')]
    [string]$ExpectedCertificateSha256 = 'a3d6a2b2a81c3ee0c96c911f31b3e46be663e5cf81ebfe1d5deccfc39d5b96bb'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$apk = [System.IO.Path]::GetFullPath($ApkPath)
if (-not (Test-Path -LiteralPath $apk)) {
    throw "APK not found: $apk"
}

$apksigner = Get-Command apksigner -ErrorAction SilentlyContinue
if (-not $apksigner) {
    $sdkCandidates = @(
        $env:ANDROID_SDK_ROOT
        $env:ANDROID_HOME
    )
    if (-not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        $sdkCandidates += Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    }
    $sdkCandidates = $sdkCandidates |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path -LiteralPath $_) }

    foreach ($sdkRoot in $sdkCandidates) {
        $buildToolsRoot = Join-Path $sdkRoot 'build-tools'
        if (-not (Test-Path -LiteralPath $buildToolsRoot)) {
            continue
        }

        $candidate = Get-ChildItem -LiteralPath $buildToolsRoot -Directory |
            Sort-Object Name -Descending |
            ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
            Where-Object { Test-Path -LiteralPath $_ } |
            Select-Object -First 1

        if ($candidate) {
            $apksigner = $candidate
            break
        }
    }
}

if (-not $apksigner) {
    throw 'apksigner was not found. Install Android SDK Build Tools or add apksigner to PATH.'
}

$verificationOutput = & $apksigner verify --verbose --print-certs $apk 2>&1
$exitCode = $LASTEXITCODE
$verificationOutput | ForEach-Object { Write-Host $_ }
if ($exitCode -ne 0) {
    throw "Signature verification failed with code $exitCode"
}

$outputText = ($verificationOutput | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine
$digestMatch = [regex]::Match(
    $outputText,
    'certificate SHA-256 digest:\s*([0-9a-fA-F]{64})',
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)
if (-not $digestMatch.Success) {
    throw 'Could not read the signer certificate SHA-256 digest from apksigner output.'
}

$actualCertificateSha256 = $digestMatch.Groups[1].Value.ToLowerInvariant()
$expectedCertificate = $ExpectedCertificateSha256.ToLowerInvariant()
if ($actualCertificateSha256 -ne $expectedCertificate) {
    throw "Unexpected Android signing certificate. Expected $expectedCertificate but found $actualCertificateSha256."
}

if ($outputText -notmatch 'Number of signers:\s*1') {
    throw 'Expected exactly one Android APK signer.'
}

Write-Host "Android signing certificate verified: $actualCertificateSha256" -ForegroundColor Green
Get-FileHash -LiteralPath $apk -Algorithm SHA256
