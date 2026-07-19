# CouchLink v0.1-dev.11.1 — Startup Registration Hotfix Test

## Build

```powershell
cd .\src\windows
dotnet clean
dotnet restore
dotnet build
```

## Repair the current startup entry

Launch the new host once from the build output or with `dotnet run`. Because **Start CouchLink with Windows** is already enabled, dev.11.1 automatically rewrites the stale registry entry.

Confirm it from PowerShell:

```powershell
Get-ItemProperty 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run' |
    Select-Object -ExpandProperty 'CouchLink Host'
```

Expected: the command points to `CouchLink.Host.exe`, not `dotnet.exe`.

## Reboot test

1. Leave **Start CouchLink with Windows** and **Start minimized to tray** enabled.
2. Reboot Windows.
3. Sign in.
4. Confirm CouchLink appears only in the tray with no dashboard or black ghost window.
5. Open the tray menu and choose **Open CouchLink**.
6. Confirm Boot Service reports `Running · heartbeat live`.
7. Confirm Android reconnects and Big Picture launches when that preference is enabled.

## Service verification

```powershell
.\tools\Get-CouchLinkServiceStatus.ps1
```

The Boot Service should be `Running / Automatic` independently of the desktop host.
