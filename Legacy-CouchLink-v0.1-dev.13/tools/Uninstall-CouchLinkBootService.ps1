#Requires -RunAsAdministrator
$ErrorActionPreference = 'Stop'
$serviceName = 'CouchLinkBootService'
$service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
if (-not $service) {
    Write-Host 'CouchLink Boot Service is not installed.' -ForegroundColor Yellow
    exit 0
}
if ($service.Status -ne 'Stopped') {
    Stop-Service -Name $serviceName -Force
    $service.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(15))
}
& sc.exe delete $serviceName | Out-Host
Write-Host 'CouchLink Boot Service removed.' -ForegroundColor Green
