# CouchLink Remote — v0.1-dev.8.1.1 Premium Control Client

This directory contains the Android client for CouchLink. The current build provides the premium three-screen control surface, trusted persistent sessions, remote mouse and keyboard input, launcher controls, and the canonical CouchLink identity assets.

## Open in Android Studio

1. Open `src/android` as the project directory.
2. Allow Android Studio to install the requested SDK and Gradle components.
3. Use the bundled or configured JDK 17 toolchain.
4. Connect an Android phone with USB debugging enabled, or build the debug APK and install it manually.
5. Run the `app` configuration.

## Build from PowerShell

```powershell
$env:JAVA_HOME = 'G:\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Current capabilities

- Discovers CouchLink hosts through UDP advertisements on port `45820`.
- Connects to the host session service on TCP port `45821`.
- Uses approval-code pairing and persistent trusted-device identity.
- Maintains a live session with heartbeat monitoring and automatic reconnection.
- Provides touchpad movement, clicking, drag lock, sensitivity control, and two-finger scrolling.
- Sends text and essential Windows keyboard commands.
- Launches Steam Big Picture as the primary action.
- Launches GOG Galaxy, EA app, Ubisoft Connect, Rockstar Games Launcher, Amazon Games, and Epic Games Launcher.
- Provides Command Deck shortcuts such as Alt+Tab, Task Manager, Show Desktop, and Close Window.
- Uses the approved graphite, orange, and metallic CouchLink visual direction.

Remote input remains protected by the local enable/disable control in CouchLink Host.
