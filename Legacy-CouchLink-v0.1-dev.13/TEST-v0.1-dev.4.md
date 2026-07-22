# CouchLink v0.1-dev.4 — Persistent Session Test

## Windows

```powershell
cd .\CouchLink\src\windows
dotnet restore
dotnet build
dotnet run --project .\CouchLink.Host.Wpf
```

The previously paired Android device should remain trusted because the host trust store is kept in the user's local CouchLink data folder.

## Android

```powershell
cd .\CouchLink\src\android
$env:JAVA_HOME = 'G:\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

Install `app\build\outputs\apk\debug\app-debug.apk`.

## Expected result

1. Android discovers Netzach.
2. The trusted token is recognized automatically.
3. Android opens a persistent TCP session without requesting another code.
4. Windows shows `Connected clients: 1` and the Android device model.
5. Android reports an advancing heartbeat counter.
6. Turn Wi-Fi off for several seconds, then back on. Android should retry and reconnect.
7. Windows returns to `Connected clients: 1` after reconnection.
8. Tap **Disconnect Session** to close the session intentionally.

Remote input remains disabled in this milestone.
