[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$PayloadPath,

    [Parameter(Mandatory)]
    [string]$PrivateKeyPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$payloadPath = [System.IO.Path]::GetFullPath($PayloadPath)
$privateKeyPath = [System.IO.Path]::GetFullPath($PrivateKeyPath)

if (-not (Test-Path -LiteralPath $payloadPath -PathType Leaf)) {
    throw "Payload not found: $payloadPath"
}
if (-not (Test-Path -LiteralPath $privateKeyPath -PathType Leaf)) {
    throw "Private key not found: $privateKeyPath"
}

$ecdsa = [System.Security.Cryptography.ECDsa]::Create()
try {
    $ecdsa.ImportFromPem((Get-Content -LiteralPath $privateKeyPath -Raw))
    $stream = [System.IO.File]::OpenRead($payloadPath)
    try {
        $signature = $ecdsa.SignData(
            $stream,
            [System.Security.Cryptography.HashAlgorithmName]::SHA256)
    }
    finally {
        $stream.Dispose()
    }

    $signaturePath = "$payloadPath.sig"
    [Convert]::ToBase64String($signature) |
        Set-Content -LiteralPath $signaturePath -Encoding ascii -NoNewline

    Write-Output $signaturePath
}
finally {
    $ecdsa.Dispose()
}
