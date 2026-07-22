#Requires -RunAsAdministrator
[CmdletBinding()]
param(
    [ValidateSet('Debug','Release')]
    [string]$Configuration = 'Debug'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$project = Join-Path $repoRoot 'src\windows\CouchLink.BootService\CouchLink.BootService.csproj'
$exe = Join-Path $repoRoot "src\windows\CouchLink.BootService\bin\$Configuration\net8.0-windows\CouchLink.BootService.exe"
$serviceName = 'CouchLinkBootService'
$programDataDirectory = Join-Path $env:ProgramData 'CouchLink'
$userTrustStore = Join-Path $env:LOCALAPPDATA 'CouchLink\trusted-devices.json'
$machineTrustStore = Join-Path $programDataDirectory 'trusted-devices.json'
$machinePermissionStore = Join-Path $programDataDirectory 'machine-permissions.json'

New-Item -ItemType Directory -Path $programDataDirectory -Force | Out-Null

# Permit the installing desktop user to keep the machine trust mirror synchronized.
& icacls.exe $programDataDirectory /grant "${env:USERNAME}:(OI)(CI)M" /T /C | Out-Null
if (Test-Path $userTrustStore) {
    Copy-Item $userTrustStore $machineTrustStore -Force
    Write-Host 'Mirrored trusted devices for pre-login authentication.' -ForegroundColor DarkCyan
} else {
    Write-Warning 'No desktop trusted-device store was found. Pair Android after login, then reinstall the service.'
}

if (-not (Test-Path $machinePermissionStore)) {
    @{
        RemoteInputEnabled = $false
        PreLoginControlEnabled = $false
        UpdatedAtUtc = [DateTimeOffset]::UtcNow
    } | ConvertTo-Json | Set-Content -Path $machinePermissionStore -Encoding UTF8
}
Write-Host 'Verified machine-level pre-login permission store.' -ForegroundColor DarkCyan

Write-Host "Building CouchLink Boot Service ($Configuration)..." -ForegroundColor Cyan
dotnet build $project -c $Configuration
if ($LASTEXITCODE -ne 0) { throw 'Boot Service build failed.' }
if (-not (Test-Path $exe)) { throw "Boot Service executable not found: $exe" }

$firewallRuleName = 'CouchLink Pre-Login Broker TCP 45822'
Get-NetFirewallRule -DisplayName $firewallRuleName -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName $firewallRuleName -Direction Inbound -Action Allow -Protocol TCP -LocalPort 45822 -Profile Private | Out-Null
Write-Host 'Verified Windows Firewall access for TCP 45822.' -ForegroundColor DarkCyan

$existing = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if ($existing) {
    if ($existing.Status -ne 'Stopped') {
        Stop-Service -Name $serviceName -Force
        $existing.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(15))
    }
    & sc.exe delete $serviceName | Out-Null
    Start-Sleep -Seconds 1
}

$quotedPath = '"' + $exe + '"'
& sc.exe create $serviceName binPath= $quotedPath start= auto DisplayName= 'CouchLink Boot Service' | Out-Host
if ($LASTEXITCODE -ne 0) { throw 'Could not create the CouchLink Boot Service.' }
& sc.exe description $serviceName 'Provides early-boot discovery, trusted status connectivity, and pre-login coordination for CouchLink.' | Out-Null
& sc.exe failure $serviceName reset= 86400 actions= restart/5000/restart/15000/none/0 | Out-Null
Start-Service -Name $serviceName

Write-Host 'CouchLink Boot Service installed and started.' -ForegroundColor Green
Get-Service -Name $serviceName | Format-Table Name, Status, StartType
