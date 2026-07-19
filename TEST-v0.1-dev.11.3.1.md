# CouchLink v0.1-dev.11.3.1 Test Checklist

## Purpose

Validate the corrected status-only pre-login baseline and driver-development prerequisite checks. This build does not install a driver and does not provide lock-screen pointer control.

## Build

- Windows solution builds successfully.
- Android `assembleDebug` reports `0.1-dev.11.3.1` / versionCode `27`.
- Boot Service installs and reports version `0.1-dev.11.3.1`.

## Pre-login

- Android discovers Netzach before login.
- Trusted status connection succeeds on TCP 45822.
- App shows `SignInRequired`.
- Touchpad and keyboard controls are disabled or rejected with `prelogin_status_only`.
- No Session 0 user32 input is attempted.

## Handoff

- After normal Windows login, Android reconnects to TCP 45821.
- Desktop touchpad, keyboard, launchers, and command deck work normally.
- Steam Big Picture launches no more than once per Windows host session.

## Driver prerequisites

Run in Administrator PowerShell:

```powershell
.\tools\Test-CouchLinkDriverPrereqs.ps1
```

This only checks Visual Studio C++ and WDK availability. Do not install an unsigned driver on Netzach.
