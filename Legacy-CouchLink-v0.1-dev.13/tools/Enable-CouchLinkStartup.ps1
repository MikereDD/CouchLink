# Run from an extracted CouchLink release after building the Windows host.
$ErrorActionPreference = 'Stop'
$exe = Resolve-Path "$PSScriptRoot\..\src\windows\CouchLink.Host.Wpf\bin\Debug\net8.0-windows\CouchLink.Host.exe"
$runKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run'
New-Item -Path $runKey -Force | Out-Null
New-ItemProperty -Path $runKey -Name 'CouchLink Host' -Value ('"{0}" --minimized' -f $exe.Path) -PropertyType String -Force | Out-Null
Write-Host "CouchLink will start minimized to the tray at sign-in." -ForegroundColor Green
Write-Host $exe.Path
