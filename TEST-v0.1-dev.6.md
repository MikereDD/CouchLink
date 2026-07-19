# CouchLink v0.1-dev.6 — Input Refinement Test

## Build

### Windows

```powershell
cd .\CouchLink\src\windows
dotnet restore
dotnet build
dotnet run --project .\CouchLink.Host.Wpf
```

### Android

```powershell
cd .\CouchLink\src\android
$env:JAVA_HOME = 'G:\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

APK: `app\build\outputs\apk\debug\app-debug.apk`

## Test checklist

1. Confirm the trusted Pixel reconnects and Windows shows one connected client.
2. Confirm Android initially displays `INPUT LOCKED`.
3. Enable remote input in the Windows host.
4. Wait for the next heartbeat and confirm Android changes to `INPUT ON`.
5. Move the pointer at several sensitivity settings.
6. Test tap, double-tap, long-press, left click, and right click.
7. Test two-finger vertical scrolling on the touchpad.
8. Enable Drag Lock, move an item/window, then press Release.
9. Open Keyboard Controls, send ordinary text into Notepad.
10. Test Escape, Tab, Backspace, Enter, and arrow keys.
11. Disable remote input in Windows and confirm Android locks after the next heartbeat.
12. Disconnect Wi-Fi while Drag Lock is active and confirm Windows releases the left button.

## Expected limitation

This is still a development input surface. Gesture tuning, modifier keys, clipboard integration, and final premium visual polish are not complete.
