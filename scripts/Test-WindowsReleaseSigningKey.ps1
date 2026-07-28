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

$privateKeyPath = [System.IO.Path]::GetFullPath($PrivateKeyPath)
$publicKeySourcePath = [System.IO.Path]::GetFullPath($PublicKeySourcePath)

if (-not (Test-Path -LiteralPath $privateKeyPath -PathType Leaf)) {
    throw "Private key not found: $privateKeyPath"
}

if (-not (Test-Path -LiteralPath $publicKeySourcePath -PathType Leaf)) {
    throw "Pinned public-key source not found: $publicKeySourcePath"
}

$source = [System.IO.File]::ReadAllText($publicKeySourcePath)
$match = [regex]::Match(
    $source,
    'PublicKeyPem\s*=\s*@"(?<pem>[\s\S]*?)";'
)

if (-not $match.Success) {
    throw 'Could not read the pinned public key from PinnedReleaseKey.cs.'
}

$publicPem = $match.Groups['pem'].Value.Replace('""', '"')

$fingerprintMatch = [regex]::Match(
    $source,
    'PublicKeySha256Fingerprint\s*=\s*"(?<fingerprint>[0-9a-fA-F]{64})"'
)

if (-not $fingerprintMatch.Success) {
    throw 'Could not read the pinned public-key fingerprint.'
}

$expectedFingerprint =
    $fingerprintMatch.Groups['fingerprint'].Value.ToLowerInvariant()

$privateKey = [System.Security.Cryptography.ECDsa]::Create()
$publicKey = [System.Security.Cryptography.ECDsa]::Create()

try {
    $privateKey.ImportFromPem(
        [System.IO.File]::ReadAllText($privateKeyPath)
    )
    $publicKey.ImportFromPem($publicPem)

    $publicKeyDer = $publicKey.ExportSubjectPublicKeyInfo()
    $actualFingerprint = [Convert]::ToHexString(
        [System.Security.Cryptography.SHA256]::HashData($publicKeyDer)
    ).ToLowerInvariant()

    if ($actualFingerprint -ne $expectedFingerprint) {
        throw 'The pinned public-key fingerprint does not match the public key.'
    }

    $probe = [System.Text.Encoding]::UTF8.GetBytes(
        'CouchLink signing-key validation'
    )
    $signature = $privateKey.SignData(
        $probe,
        [System.Security.Cryptography.HashAlgorithmName]::SHA256
    )

    if (-not $publicKey.VerifyData(
            $probe,
            $signature,
            [System.Security.Cryptography.HashAlgorithmName]::SHA256
        )) {
        throw 'The private key does not match the pinned public key.'
    }

    Write-Host 'CouchLink Windows signing key: valid and matched.' `
        -ForegroundColor Green
}
finally {
    $privateKey.Dispose()
    $publicKey.Dispose()
}
