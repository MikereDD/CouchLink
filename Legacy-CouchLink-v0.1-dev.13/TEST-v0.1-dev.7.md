# CouchLink v0.1-dev.7 — Premium Control Surface Test

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

## Test checklist

1. Confirm the Pixel reconnects and the host shows one connected client.
2. Confirm the new CL header, host card, and bottom navigation render correctly.
3. Open Home and tap Steam. Steam Big Picture should open or come forward.
4. Test every installed secondary launcher. Missing launchers should fail safely and leave a host event.
5. Enable remote input on Windows and test Alt+Tab, Task Manager, Show Desktop, and Close Window.
6. Open Touchpad and verify movement, clicks, two-finger scrolling, sensitivity, and Drag Lock.
7. Open Keyboard and verify text, special keys, arrows, and shortcuts.
8. Disable remote input. Touchpad, keyboard, and Command Deck controls should stop; launcher buttons remain available to the trusted device.
9. Interrupt Wi-Fi and confirm the trusted session reconnects.
