$runKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run'
Remove-ItemProperty -Path $runKey -Name 'CouchLink Host' -ErrorAction SilentlyContinue
Write-Host "CouchLink startup entry removed." -ForegroundColor Yellow
