# CouchLink Remote

CouchLink turns an Android phone into a persistent Bluetooth keyboard, mouse, and living-room launcher remote for Windows. It works at the Windows sign-in screen through the standard Windows Bluetooth HID stack and does not require a Windows companion app, custom driver, cloud account, or telemetry.

## Current build

- Version: `0.2-dev.12.1`
- versionCode: `44`
- Minimum Android: Android 9 / API 28
- Target / compile SDK: 36
- Java toolchain: JDK 17

## Features

- Persistent Bluetooth HID connection with automatic reconnect
- Windows sign-in keyboard and mouse input
- Touchpad, mouse buttons, drag lock, and scrolling
- Keyboard text, Windows keys, and command shortcuts
- Steam Big Picture launch through `steam://open/bigpicture`
- Direct EA desktop launcher command
- Start Search launch fallback for other supported game launchers
- Premium metallic living-room interface based on the approved CouchLink visual design
- Modular Compose UI split into theme, shared components, and dedicated screen files
- Type-safe launcher, keyboard, shortcut, and mouse command models
- Full-width Steam and GOG feature launchers with cyan and violet accent treatments
- 3-column launcher deck, premium command deck, and fixed segmented bottom navigation
- No Windows Host, Boot Service, virtual HID driver, Test Mode, or disabled Secure Boot

## Build

Open this directory in Android Studio, use JDK 17, and build the `app` configuration.

Command line:

```powershell
.\gradlew.bat clean :app:assembleDebug
```

APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Validation gates

1. Reconnects automatically after Windows reboot.
2. Touchpad and keyboard work with Android Wi-Fi disabled.
3. Input works at the Windows sign-in screen with Secure Boot enabled.
4. Leaving CouchLink open in the background does not disconnect HID.
5. Every launcher tile opens the intended Windows application.
6. Explicit disconnect does not display a false reconnecting state.
7. Drag lock releases when leaving the Touchpad screen or losing the HID connection.
