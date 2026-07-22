# CouchLink Remote

<p align="center">
  <img src="app/src/main/res/drawable-nodpi/couchlink_logo.png" alt="CouchLink Remote" width="160">
</p>

<p align="center">
  <strong>Android control surface for CouchLink</strong><br>
  Version <code>0.1-dev.13</code> · versionCode <code>29</code> · Protocol <code>1</code>
</p>

---

## Overview

CouchLink Remote is the premium Android client for controlling a trusted Windows living-room PC over the local network. The app is built with Kotlin and Jetpack Compose and provides Home, Touchpad, Keyboard, Settings, About, and diagnostics experiences.

## Capabilities

- Discovers hosts through UDP advertisements on port `45820`.
- Pairs through an approval code and persists a trusted-device identity.
- Maintains a heartbeat-backed session with automatic reconnection.
- Moves cleanly between desktop port `45821` and pre-login port `45822`.
- Provides touchpad movement, clicks, drag lock, sensitivity, and two-finger scrolling.
- Sends text, essential Windows keys, and Command Deck shortcuts.
- Launches or focuses Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar Games Launcher, Epic Games Launcher, and Amazon Games.
- Displays structured launcher results from the Windows host.
- Controls the lock and sign-in screens when the trusted Virtual HID path is enabled.
- Exposes app, host, protocol, connection, and privacy diagnostics in About.

## Requirements

| Requirement | Value |
|---|---|
| Minimum Android | API 26 |
| Target / compile SDK | 36 |
| Java toolchain | JDK 17 |
| UI | Jetpack Compose + Material 3 |
| Network | Same private LAN as the CouchLink host |

## Build

From the repository root:

```powershell
.\src\android\gradlew.bat `
    -p .\src\android `
    :app:assembleDebug
```

Or from this directory:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is written to:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Install over an existing debug build:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

## Open in Android Studio

1. Open `src/android` as the project.
2. Allow Gradle and SDK synchronization to complete.
3. Use the JDK 17 toolchain.
4. Select the `app` run configuration.
5. Run on a USB-debugging device or emulator with LAN access to the host.

## Security model

The Android client does not store Windows credentials or bypass Windows authentication. It presents trusted remote input to the host; Windows remains responsible for validating the local PIN or password. Pairing is unavailable before login, and only previously trusted devices can use the pre-login endpoint.
