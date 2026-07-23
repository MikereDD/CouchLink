# Building CouchLink

## Android remote

Open `src/android` in Android Studio, or build from PowerShell:

```powershell
cd .\src\android
.\gradlew.bat clean :app:assembleDebug
```

The debug APK is normally written under `app\build\outputs\apk\debug\`.

## Windows host

Requirements: .NET 8 SDK and Windows.

```powershell
cd .\src\windows
dotnet restore .\CouchLink.sln
dotnet build .\CouchLink.sln -c Debug
```

Run the host:

```powershell
dotnet run --project .\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj
```

Create a release build:

```powershell
dotnet build .\CouchLink.sln -c Release
```

Create a self-contained Windows x64 package:

```powershell
dotnet publish .\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj `
  -c Release `
  -r win-x64 `
  --self-contained true `
  -p:PublishSingleFile=true `
  -o .\publish\CouchLink-Host
```

## Android signed releases

See [ANDROID-SIGNING.md](../release/ANDROID-SIGNING.md). Never commit the `.jks` signing key, passwords, `local.properties`, generated APKs, or release output.
