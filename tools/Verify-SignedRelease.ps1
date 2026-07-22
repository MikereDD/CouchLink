[CmdletBinding()]
param(
    [string]$ApkPath = (Join-Path $PSScriptRoot '..\release\CouchLink-v1.0.apk')
)

$ErrorActionPreference = 'Stop'
$apk = [System.IO.Path]::GetFullPath($ApkPath)
if (-not (Test-Path $apk)) { throw "APK not found: $apk" }

$apksigner = Get-Command apksigner -ErrorAction SilentlyContinue
if (-not $apksigner) {
    $sdkRoot = $env:ANDROID_SDK_ROOT
    if (-not $sdkRoot) { $sdkRoot = $env:ANDROID_HOME }
    if ($sdkRoot) {
        $candidate = Get-ChildItem (Join-Path $sdkRoot 'build-tools') -Directory |
            Sort-Object Name -Descending |
            ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
            Where-Object { Test-Path $_ } |
            Select-Object -First 1
        if ($candidate) { $apksigner = $candidate }
    }
}
if (-not $apksigner) { throw 'apksigner was not found. Install Android SDK Build Tools or add apksigner to PATH.' }

& $apksigner verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw "Signature verification failed with code $LASTEXITCODE" }
Get-FileHash -Algorithm SHA256 $apk
