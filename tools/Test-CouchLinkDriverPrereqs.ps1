$ErrorActionPreference = 'Stop'
Write-Host 'CouchLink Virtual HID prerequisite check' -ForegroundColor Cyan
$vswhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
if (-not (Test-Path $vswhere)) { Write-Warning 'Visual Studio Installer / vswhere not found.'; exit 1 }
$vs = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (-not $vs) { Write-Warning 'Visual Studio C++ workload not found.'; exit 1 }
Write-Host "Visual Studio: $vs" -ForegroundColor Green
$kits = Get-ChildItem 'HKLM:\SOFTWARE\Microsoft\Windows Kits\Installed Roots' -ErrorAction SilentlyContinue
if (-not $kits) { Write-Warning 'Windows Driver Kit registry roots not found.'; exit 1 }
Write-Host 'Windows Kits registry roots detected.' -ForegroundColor Green
Write-Host 'Prerequisites detected. Build and test only inside a disposable Windows VM.' -ForegroundColor Yellow

