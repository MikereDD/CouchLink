[CmdletBinding()]
param(
    [string]$KeystorePath,

    [string]$Alias = 'couchlink-release',

    [string]$Version = '1.3.3',

    [string]$OutputDirectory
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

if ([string]::IsNullOrWhiteSpace($KeystorePath)) {
    $defaultKeystore = Join-Path $projectRoot 'signing\couchlink-release.jks'
    $enteredPath = Read-Host "Android keystore path [$defaultKeystore]"
    $KeystorePath = if ([string]::IsNullOrWhiteSpace($enteredPath)) {
        $defaultKeystore
    }
    else {
        $enteredPath.Trim().Trim('"')
    }
}

$keystore = [System.IO.Path]::GetFullPath($KeystorePath)
if (-not (Test-Path -LiteralPath $keystore)) {
    throw "Keystore not found: $keystore"
}

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $projectRoot 'release'
}
else {
    $OutputDirectory = [System.IO.Path]::GetFullPath($OutputDirectory)
}

$storePass = Read-Host 'Keystore password' -AsSecureString
$keyPass = Read-Host 'Key password' -AsSecureString
$storePlain = [System.Net.NetworkCredential]::new('', $storePass).Password
$keyPlain = [System.Net.NetworkCredential]::new('', $keyPass).Password
$pushedLocation = $false
$signingPropertiesPath = Join-Path $projectRoot 'keystore.properties'

function ConvertTo-Base64Utf8 {
    param([Parameter(Mandatory)][string]$Value)

    return [Convert]::ToBase64String(
        [System.Text.Encoding]::UTF8.GetBytes($Value)
    )
}

@(
    "storeFileB64=$(ConvertTo-Base64Utf8 -Value $keystore)"
    "storePasswordB64=$(ConvertTo-Base64Utf8 -Value $storePlain)"
    "keyAliasB64=$(ConvertTo-Base64Utf8 -Value $Alias)"
    "keyPasswordB64=$(ConvertTo-Base64Utf8 -Value $keyPlain)"
) | Set-Content -LiteralPath $signingPropertiesPath -Encoding ascii

$env:COUCHLINK_KEYSTORE_FILE = $keystore
$env:COUCHLINK_KEY_ALIAS = $Alias
$env:COUCHLINK_KEYSTORE_PASSWORD = $storePlain
$env:COUCHLINK_KEY_PASSWORD = $keyPlain

try {
    Push-Location $projectRoot
    $pushedLocation = $true

    & .\gradlew.bat --no-daemon clean :app:assembleRelease
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle exited with code $LASTEXITCODE"
    }

    $apkDirectory = Join-Path $projectRoot 'app\build\outputs\apk\release'
    $apk = Join-Path $apkDirectory 'app-release.apk'

    if (-not (Test-Path -LiteralPath $apk)) {
        $producedApks = @(
            Get-ChildItem -LiteralPath $apkDirectory -Filter '*.apk' -File -ErrorAction SilentlyContinue |
                Select-Object -ExpandProperty Name
        )

        $details = if ($producedApks.Count -gt 0) {
            $producedApks -join ', '
        }
        else {
            'no APK files were found'
        }

        throw "A signed release APK was not produced in ${apkDirectory}. Produced: $details"
    }

    New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
    $finalApk = Join-Path $OutputDirectory "CouchLink-v$Version.apk"
    Copy-Item -LiteralPath $apk -Destination $finalApk -Force

    $hash = (Get-FileHash -LiteralPath $finalApk -Algorithm SHA256).Hash.ToLowerInvariant()
    $checksumPath = "$finalApk.sha256"
    "$hash  $([System.IO.Path]::GetFileName($finalApk))" |
        Set-Content -LiteralPath $checksumPath -Encoding ascii

    Write-Host "Signed APK: $finalApk"
    Write-Host "Checksum: $checksumPath"
}
finally {
    Remove-Item -LiteralPath $signingPropertiesPath -Force -ErrorAction SilentlyContinue

    if ($pushedLocation) {
        Pop-Location
    }

    Remove-Item Env:COUCHLINK_KEYSTORE_FILE -ErrorAction SilentlyContinue
    Remove-Item Env:COUCHLINK_KEY_ALIAS -ErrorAction SilentlyContinue
    Remove-Item Env:COUCHLINK_KEYSTORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:COUCHLINK_KEY_PASSWORD -ErrorAction SilentlyContinue

    $storePlain = $null
    $keyPlain = $null
    $storePass = $null
    $keyPass = $null
}
