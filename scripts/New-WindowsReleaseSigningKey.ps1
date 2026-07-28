[CmdletBinding()]
param(
    [string]$PrivateKeyPath = (
        Join-Path $HOME 'Documents\CouchLink\Keys\windows-release-private.pem'
    ),

    [string]$PublicKeySourcePath = (
        Join-Path $PSScriptRoot '..\src\windows\CouchLink.ReleaseSecurity\PinnedReleaseKey.cs'
    )
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-Utf8NoBom {
    param(
        [Parameter(Mandatory)]
        [string]$Path,

        [Parameter(Mandatory)]
        [string]$Content
    )

    $encoding = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

$resolvedPrivateKeyPath = [System.IO.Path]::GetFullPath($PrivateKeyPath)
$resolvedPublicKeySourcePath = [System.IO.Path]::GetFullPath(
    $PublicKeySourcePath
)

if (Test-Path -LiteralPath $resolvedPrivateKeyPath) {
    throw "Private key already exists: $resolvedPrivateKeyPath"
}

$privateDirectory = Split-Path -Parent $resolvedPrivateKeyPath
$publicSourceDirectory = Split-Path -Parent $resolvedPublicKeySourcePath

New-Item -ItemType Directory -Force -Path $privateDirectory | Out-Null
New-Item -ItemType Directory -Force -Path $publicSourceDirectory | Out-Null

Write-Host 'Generating an ECDSA P-256 CouchLink release-signing key...'

$curve = [System.Security.Cryptography.ECCurve]::CreateFromFriendlyName(
    'nistP256'
)
$ecdsa = [System.Security.Cryptography.ECDsa]::Create($curve)

try {

    $privatePem = $ecdsa.ExportPkcs8PrivateKeyPem()
    $publicPem = $ecdsa.ExportSubjectPublicKeyInfoPem()

    [System.IO.File]::WriteAllText(
        $resolvedPrivateKeyPath,
        $privatePem,
        [System.Text.Encoding]::ASCII
    )

    $publicKeyDer = $ecdsa.ExportSubjectPublicKeyInfo()
    $fingerprintBytes = [System.Security.Cryptography.SHA256]::HashData(
        $publicKeyDer
    )
    $fingerprint = [Convert]::ToHexString(
        $fingerprintBytes
    ).ToLowerInvariant()

    $escapedPublicPem = $publicPem.Replace('"', '""')
    $publicKeySource = @"
namespace CouchLink.ReleaseSecurity;

public static class PinnedReleaseKey
{
    public const string PublicKeySha256Fingerprint = `"$fingerprint`";

    public const string PublicKeyPem = @`"$escapedPublicPem`";
}
"@

    Write-Utf8NoBom `
        -Path $resolvedPublicKeySourcePath `
        -Content $publicKeySource

    if (-not (Test-Path -LiteralPath $resolvedPrivateKeyPath -PathType Leaf)) {
        throw 'The private key file was not created.'
    }

    if ((Get-Item -LiteralPath $resolvedPrivateKeyPath).Length -le 0) {
        throw 'The private key file is empty.'
    }

    if (-not (Test-Path -LiteralPath $resolvedPublicKeySourcePath -PathType Leaf)) {
        throw 'The pinned public-key source file was not created.'
    }

    $privateVerifier = [System.Security.Cryptography.ECDsa]::Create()
    $publicVerifier = [System.Security.Cryptography.ECDsa]::Create()

    try {
        $privateVerifier.ImportFromPem(
            [System.IO.File]::ReadAllText($resolvedPrivateKeyPath)
        )

        $publicVerifier.ImportFromPem($publicPem)

        $probe = [System.Text.Encoding]::UTF8.GetBytes(
            'CouchLink release-signing key verification'
        )

        $probeSignature = $privateVerifier.SignData(
            $probe,
            [System.Security.Cryptography.HashAlgorithmName]::SHA256
        )

        if (-not $publicVerifier.VerifyData(
                $probe,
                $probeSignature,
                [System.Security.Cryptography.HashAlgorithmName]::SHA256
            )) {
            throw 'Generated signing-key self-test failed.'
        }
    }
    finally {
        $privateVerifier.Dispose()
        $publicVerifier.Dispose()
    }

    Write-Host ''
    Write-Host 'CouchLink Windows release-signing key created.' `
        -ForegroundColor Green
    Write-Host "Private key: $resolvedPrivateKeyPath" `
        -ForegroundColor Yellow
    Write-Host "Pinned public key: $resolvedPublicKeySourcePath"
    Write-Host "Public-key SHA-256 fingerprint: $fingerprint"
    Write-Host ''
    Write-Host 'The private key passed a sign/verify self-test.' `
        -ForegroundColor Green
    Write-Host 'Back it up securely. Never commit or publish it.' `
        -ForegroundColor Yellow
}
catch {
    if (Test-Path -LiteralPath $resolvedPrivateKeyPath) {
        Remove-Item -LiteralPath $resolvedPrivateKeyPath -Force `
            -ErrorAction SilentlyContinue
    }

    throw
}
finally {
    $ecdsa.Dispose()
}
