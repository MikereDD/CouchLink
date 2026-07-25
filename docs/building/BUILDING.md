# Building CouchLink 1.1

## Complete signed release build

Run the repository-level release builder from PowerShell on Windows:

```powershell
.\Build-Release.ps1
```

The script:

1. Prompts for the Android keystore path when one was not supplied.
2. Securely prompts for the keystore and key passwords.
3. Builds and verifies the signed Android release APK.
4. Publishes the Windows x64 host as a single-file executable.
5. Generates individual SHA-256 files and one combined checksum list.

Default output:

```text
release\CouchLink-v1.1\
├── CouchLink-v1.1.apk
├── CouchLink-v1.1.apk.sha256
├── CouchLink-Host-v1.1-win-x64.exe
├── CouchLink-Host-v1.1-win-x64.exe.sha256
└── SHA256SUMS-v1.1.txt
```

The default Windows build is framework-dependent and requires the .NET 8 Desktop Runtime. Add `-SelfContained` to embed the runtime:

```powershell
.\Build-Release.ps1 -SelfContained
```

You may supply the keystore path and alias on the command line while keeping both passwords out of command history:

```powershell
.\Build-Release.ps1 `
  -KeystorePath 'G:\secure\couchlink-release.jks' `
  -KeyAlias 'couchlink-release'
```

If the target release directory already contains files, the script preserves it and creates a timestamped build directory instead.

If any stage fails, the script prints a red failure summary and writes full details to `release\Build-Release-error.log`.

## Android remote

Open `src/android` in Android Studio, or build a debug APK from PowerShell:

```powershell
cd .\src\android
.\gradlew.bat clean :app:assembleDebug
```

The debug APK is written under `app\build\outputs\apk\debug\`.

### Signed Android release only

The Android-only helper also prompts for the keystore path when it is omitted:

```powershell
cd .\src\android
.\tools\Build-SignedRelease.ps1
.\tools\Verify-SignedRelease.ps1
```

See [ANDROID-SIGNING.md](../release/ANDROID-SIGNING.md) for key creation and backup requirements.

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

Publish the same framework-dependent single-file Windows x64 package used by the combined release script:

```powershell
dotnet publish .\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj `
  -c Release `
  -r win-x64 `
  --self-contained false `
  -p:PublishSingleFile=true `
  -p:DebugType=None `
  -p:DebugSymbols=false `
  -o .\publish\CouchLink-Host-v1.1-win-x64
```

Generate a checksum for the published executable:

```powershell
Get-FileHash `
  .\publish\CouchLink-Host-v1.1-win-x64\CouchLink.Host.exe `
  -Algorithm SHA256
```

The root `Build-Release.ps1` script performs this publish and writes the checksum automatically.

## Release integrity

Checksums must be generated from the exact APK and EXE files attached to the release. The combined builder creates both per-file checksum files and `SHA256SUMS-v1.1.txt` from the final renamed artifacts.
