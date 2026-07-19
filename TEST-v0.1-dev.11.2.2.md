# CouchLink v0.1-dev.11.2.3 — Pre-Login Permission Sync Test

This hotfix keeps the Boot Service TCP broker on port 45822 alive for the full service lifetime, repairs its firewall rule, records listener diagnostics, and forces Android to reconnect to the desktop Session Host after login.

## Upgrade

1. In Administrator PowerShell, uninstall the service from the previous build before deleting its folder:

```powershell
.\tools\Uninstall-CouchLinkBootService.ps1
```

2. Build Windows while the service is stopped:

```powershell
cd .\src\windows
dotnet clean
dotnet restore
dotnet build
```

3. Build and install Android:

```powershell
cd ..\android
$env:JAVA_HOME = 'G:\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

4. Launch the desktop host once so the trusted-device mirror is current, then reinstall the service from the repository root:

```powershell
.\tools\Install-CouchLinkBootService.ps1
```

## Signed-in diagnostics

Even after login, TCP 45822 should remain listening:

```powershell
Get-NetTCPConnection -LocalPort 45822 -State Listen
Get-Content "$env:ProgramData\CouchLink\boot-service-status.json"
```

Expected heartbeat fields:

```text
PreLoginBroker : Listening
BrokerEndpoint : 0.0.0.0:45822
BrokerLastError: null
```

## Pre-login test

1. Reboot Netzach and stop at the Windows sign-in screen.
2. Open CouchLink Remote on Android.
3. Expected:
   - NETZACH is discovered as `SignInRequired`.
   - Android authenticates on TCP 45822.
   - The pre-login status panel appears.
   - Input remains locked.
4. Sign in normally.
5. Expected:
   - The broker closes the status-only client connection once the desktop exists.
   - Android automatically reconnects to TCP 45821.
   - CouchLink Host remains minimized to tray.
   - Steam Big Picture launches after the trusted desktop connection when enabled.

## Firewall check

```powershell
Get-NetFirewallRule -DisplayName 'CouchLink Pre-Login Broker TCP 45822'
```
