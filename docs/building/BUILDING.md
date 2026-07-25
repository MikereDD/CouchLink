# Building CouchLink 1.1

## Android remote

Open `src/android` in Android Studio, or build from PowerShell:

```powershell
cd .\src\android
.\gradlew.bat clean :app:assembleDebug
```

The debug APK is written under `app\build\outputs\apk\debug\`.

### Signed Android release

Use the provided release helper after configuring the private keystore environment described in [ANDROID-SIGNING.md](../release/ANDROID-SIGNING.md):

```powershell
cd .\src\android
.\tools\Build-SignedRelease.ps1
.\tools\Verify-SignedRelease.ps1
```

Never commit the `.jks` signing key, passwords, `local.properties`, generated APKs, or release output.

## Windows Launcher Host

Requirements: Windows and the .NET 8 SDK.

```powershell
cd .\src\windows
dotnet restore .\CouchLink.sln
dotnet build .\CouchLink.sln -c Release
```

Run the host from source:

```powershell
dotnet run --project .\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj -c Release
```

Create a self-contained Windows x64 package:

```powershell
dotnet publish .\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj `
  -c Release `
  -r win-x64 `
  --self-contained true `
  -p:PublishSingleFile=true `
  -o .\publish\CouchLink-Host-v1.1-win-x64
```

## Release integrity

Create checksums after final artifacts are generated:

```powershell
Get-FileHash .\path\to\artifact -Algorithm SHA256
```

Checksums must be generated from the exact artifacts attached to the release.
