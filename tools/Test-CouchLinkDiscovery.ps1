#Requires -Version 7.0
[CmdletBinding()]
param(
    [int]$DiscoveryPort = 45820,
    [int]$TimeoutSeconds = 10,
    [switch]$CompleteHello
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Read-ExactBytes {
    param([System.IO.Stream]$Stream, [int]$Count)
    $buffer = [byte[]]::new($Count)
    $offset = 0
    while ($offset -lt $Count) {
        $read = $Stream.Read($buffer, $offset, $Count - $offset)
        if ($read -eq 0) { throw 'Connection ended while reading a frame.' }
        $offset += $read
    }
    return $buffer
}

$udp = [System.Net.Sockets.UdpClient]::new($DiscoveryPort)
$udp.Client.ReceiveTimeout = $TimeoutSeconds * 1000
$remote = [System.Net.IPEndPoint]::new([System.Net.IPAddress]::Any, 0)

try {
    Write-Host "Listening for CouchLink on UDP $DiscoveryPort for up to $TimeoutSeconds seconds..." -ForegroundColor Cyan
    $bytes = $udp.Receive([ref]$remote)
    $json = [System.Text.Encoding]::UTF8.GetString($bytes)
    $hostInfo = $json | ConvertFrom-Json

    Write-Host "`nDiscovered $($hostInfo.hostName)" -ForegroundColor Green
    $hostInfo | Format-List

    if (-not $CompleteHello) { return }

    $tcp = [System.Net.Sockets.TcpClient]::new()
    try {
        $tcp.Connect([string]$hostInfo.address, [int]$hostInfo.sessionPort)
        $stream = $tcp.GetStream()
        $envelope = @{
            protocolVersion = 1
            messageId = [guid]::NewGuid()
            type = 'hello'
            sentAtUtc = [datetimeoffset]::UtcNow
            payload = @{
                clientName = $env:COMPUTERNAME
                clientPlatform = 'PowerShell'
                clientVersion = '0.1-dev.2'
            }
        } | ConvertTo-Json -Depth 5 -Compress

        $payload = [System.Text.Encoding]::UTF8.GetBytes($envelope)
        $length = [System.Net.IPAddress]::HostToNetworkOrder([int]$payload.Length)
        $header = [BitConverter]::GetBytes($length)
        $stream.Write($header, 0, $header.Length)
        $stream.Write($payload, 0, $payload.Length)
        $stream.Flush()

        $responseHeader = Read-ExactBytes -Stream $stream -Count 4
        $responseLength = [System.Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($responseHeader, 0))
        $responsePayload = Read-ExactBytes -Stream $stream -Count $responseLength
        $responseJson = [System.Text.Encoding]::UTF8.GetString($responsePayload)

        Write-Host "`nHello exchange complete:" -ForegroundColor Green
        $responseJson | ConvertFrom-Json | ConvertTo-Json -Depth 6
    }
    finally {
        if ($null -ne $tcp) { $tcp.Dispose() }
    }
}
finally {
    $udp.Dispose()
}
