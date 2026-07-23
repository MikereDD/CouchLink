[CmdletBinding()]
param(
    [string]$OutputPath = (Join-Path $PSScriptRoot '..\signing\couchlink-release.jks'),
    [string]$Alias = 'couchlink-release',
    [int]$ValidityDays = 10000
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command keytool -ErrorAction SilentlyContinue)) {
    throw 'keytool was not found. Run this from a JDK 17 terminal or add the JDK bin directory to PATH.'
}

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
$parent = Split-Path -Parent $resolvedOutput
New-Item -ItemType Directory -Force -Path $parent | Out-Null

if (Test-Path $resolvedOutput) {
    throw "Refusing to overwrite existing keystore: $resolvedOutput"
}

$storePass = Read-Host 'Create keystore password' -AsSecureString
$keyPass = Read-Host 'Create key password' -AsSecureString
$storePlain = [System.Net.NetworkCredential]::new('', $storePass).Password
$keyPlain = [System.Net.NetworkCredential]::new('', $keyPass).Password

try {
    & keytool -genkeypair `
        -keystore $resolvedOutput `
        -storepass $storePlain `
        -keypass $keyPlain `
        -alias $Alias `
        -keyalg RSA `
        -keysize 4096 `
        -sigalg SHA256withRSA `
        -validity $ValidityDays `
        -dname 'CN=Typezer0, OU=CouchLink, O=Typezer0 Studio, C=US'

    if ($LASTEXITCODE -ne 0) {
        throw "keytool exited with code $LASTEXITCODE"
    }

    Write-Host "Created release keystore: $resolvedOutput"
    Write-Host 'Back up this file and both passwords. Losing it prevents future updates signed with the same identity.'
}
finally {
    $storePlain = $null
    $keyPlain = $null
}
