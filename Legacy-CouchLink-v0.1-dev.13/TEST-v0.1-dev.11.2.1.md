# CouchLink v0.1-dev.11.2.1 — Pre-Login Connectivity Foundation Test

This build proves that a previously paired Android remote can discover and authenticate with Netzach before Windows login. It is intentionally status-only: secure-desktop keyboard/password input is not enabled yet.

## Upgrade

1. From the old build, open Administrator PowerShell and uninstall the old service:

```powershell
.\tools\Uninstall-CouchLinkBootService.ps1
```

2. Extract dev.11.2, then build both Windows and Android.
3. Launch the desktop host once while signed in. This refreshes the machine trust mirror.
4. In Administrator PowerShell from the dev.11.2 root:

```powershell
.\tools\Install-CouchLinkBootService.ps1
.\tools\Get-CouchLinkServiceStatus.ps1
```

The installer should report that trusted devices were mirrored.

## Windows build

```powershell
cd .\src\windows
dotnet clean
dotnet restore
dotnet build
```

## Android build

```powershell
cd .\src\android
$env:JAVA_HOME = 'G:\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

Install `app\build\outputs\apk\debug\app-debug.apk`.

## Pre-login test

1. Fully reboot Netzach.
2. Stop at the Windows sign-in screen. Do not log in.
3. Open CouchLink Remote on Android.
4. Expected:
   - NETZACH is discovered.
   - Host state is `SignInRequired`.
   - The app reports a trusted connection before login.
   - The dedicated “Connected before login” panel appears.
   - Input remains locked and no password/keyboard controls are offered.
5. Log in normally at the PC.
6. Expected:
   - The Boot Service status broker goes to standby.
   - CouchLink Host launches minimized to tray.
   - Android automatically switches from port 45822 to the desktop session on port 45821.
   - Steam Big Picture launches when configured.

## Diagnostics

```powershell
Get-Content "$env:ProgramData\CouchLink\boot-service-status.json"
Get-NetTCPConnection -LocalPort 45822 -ErrorAction SilentlyContinue
Get-Content "$env:ProgramData\CouchLink\trusted-devices.json"
```

At the sign-in screen the service listens on TCP 45822 and advertises over UDP 45820. After login, TCP 45822 should close and the normal desktop host owns TCP 45821.
