[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$KeystorePath,
    [string]$Alias = 'couchlink-release'
)

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$keystore = [System.IO.Path]::GetFullPath($KeystorePath)

if (-not (Test-Path $keystore)) {
    throw "Keystore not found: $keystore"
}

$storePass = Read-Host 'Keystore password' -AsSecureString
$keyPass = Read-Host 'Key password' -AsSecureString

$env:COUCHLINK_KEYSTORE_FILE = $keystore
$env:COUCHLINK_KEY_ALIAS = $Alias
$env:COUCHLINK_KEYSTORE_PASSWORD = [System.Net.NetworkCredential]::new('', $storePass).Password
$env:COUCHLINK_KEY_PASSWORD = [System.Net.NetworkCredential]::new('', $keyPass).Password

try {
    Push-Location $projectRoot
    & .\gradlew.bat --no-daemon clean :app:assembleRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle exited with code $LASTEXITCODE"
    }

    $apk = Join-Path $projectRoot 'app\build\outputs\apk\release\app-release.apk'
    if (-not (Test-Path $apk)) {
        throw "Expected APK was not produced: $apk"
    }

    $releaseDir = Join-Path $projectRoot 'release'
    New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null
    $finalApk = Join-Path $releaseDir 'CouchLink-v1.0.apk'
    Copy-Item -Force $apk $finalApk
    Get-FileHash -Algorithm SHA256 $finalApk |
        ForEach-Object { "$($_.Hash.ToLower())  CouchLink-v1.0.apk" } |
        Set-Content -Encoding ascii (Join-Path $releaseDir 'CouchLink-v1.0.apk.sha256')

    Write-Host "Signed APK: $finalApk"
    Write-Host "Checksum: $(Join-Path $releaseDir 'CouchLink-v1.0.apk.sha256')"
}
finally {
    Pop-Location -ErrorAction SilentlyContinue
    Remove-Item Env:COUCHLINK_KEYSTORE_FILE -ErrorAction SilentlyContinue
    Remove-Item Env:COUCHLINK_KEY_ALIAS -ErrorAction SilentlyContinue
    Remove-Item Env:COUCHLINK_KEYSTORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:COUCHLINK_KEY_PASSWORD -ErrorAction SilentlyContinue
}
