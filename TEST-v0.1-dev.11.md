# CouchLink v0.1-dev.11 — Boot Service Foundation Test

This milestone separates the desktop-session runtime from the future early-boot service boundary. It does **not** add pre-login keyboard or mouse control.

## 1. Build the complete Windows solution

```powershell
cd .\CouchLink\src\windows
dotnet clean
dotnet restore
dotnet build
```

Expected projects:

- CouchLink.Protocol
- CouchLink.Host.Core
- CouchLink.SessionHost
- CouchLink.BootService
- CouchLink.Host.Wpf

## 2. Run without the service installed

```powershell
dotnet run --project .\CouchLink.Host.Wpf
```

Expected dashboard state:

- Boot Service: Not installed
- Session Host: Desktop session active
- Existing Android pairing, input, launcher, tray, and reconnect features still work

## 3. Install the Boot Service

Open **elevated PowerShell** from the repository root:

```powershell
.\tools\Install-CouchLinkBootService.ps1
```

Expected:

- Service name: `CouchLinkBootService`
- Startup type: Automatic
- Status: Running
- Heartbeat file: `%ProgramData%\CouchLink\boot-service-status.json`

## 4. Verify service health

```powershell
.\tools\Get-CouchLinkServiceStatus.ps1
```

Within several seconds the dashboard should update to show:

- Boot Service: Running · heartbeat live
- Machine state: DesktopAvailable
- Session Host: Desktop session active

## 5. Service independence

Exit the CouchLink dashboard completely. Then run:

```powershell
.\tools\Get-CouchLinkServiceStatus.ps1
```

The Boot Service heartbeat should continue updating even while the desktop host is stopped.

Restart CouchLink Host and confirm the existing Android device reconnects.

## 6. Restart test

Restart Windows. After sign-in:

```powershell
Get-Service CouchLinkBootService
```

Expected: Running.

Open CouchLink Host and confirm the dashboard reports a live Boot Service heartbeat.

## 7. Uninstall test

From elevated PowerShell:

```powershell
.\tools\Uninstall-CouchLinkBootService.ps1
```

The dashboard should fall back to `Not installed` after the next status refresh.

## Regression checks

- Headless tray startup has no black window
- Android trusted reconnect works
- Touchpad and keyboard work
- Steam Big Picture startup still works
- Launcher buttons still work
- Trusted-device management still works
- Closing or exiting CouchLink behaves correctly
