$serviceName = 'CouchLinkBootService'
$statusPath = Join-Path $env:ProgramData 'CouchLink\boot-service-status.json'

Get-Service -Name $serviceName -ErrorAction SilentlyContinue |
    Format-Table Name, Status, StartType

if (Test-Path $statusPath) {
    Write-Host "`nHeartbeat: $statusPath" -ForegroundColor Cyan
    Get-Content $statusPath -Raw | ConvertFrom-Json | Format-List
} else {
    Write-Host "`nNo Boot Service heartbeat found yet." -ForegroundColor Yellow
}
