[CmdletBinding()]
param(
    [string]$ApkPath = (Join-Path $PSScriptRoot '..\release\CouchLink-v1.1.apk')
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

& $apksigner verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) {
    throw "Signature verification failed with code $LASTEXITCODE"
}

Get-FileHash -LiteralPath $apk -Algorithm SHA256
