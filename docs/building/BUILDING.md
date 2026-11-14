# Building CouchLink

## Complete signed release build

Run the repository-level release builder from PowerShell on Windows:

```powershell
.\Build-Release.ps1
```

The script:

1. Prompts for the Android keystore path when one was not supplied.
2. Securely prompts for the keystore and key passwords.
3. Builds and verifies the signed Android release APK.
4. Publishes the Windows Host and Updater as single-file executables.
5. Verifies the Android signing certificate, signs both Windows executables, and tests valid and tampered signatures.
6. Regenerates and verifies the source manifest.
7. Packages a clean source ZIP and release documentation.
8. Generates individual SHA-256 files and one combined checksum list.

Default output:

```text
release\CouchLink-v1.4.0\
├── CouchLink-v1.4.0.apk
├── CouchLink-v1.4.0.apk.sha256
├── CouchLink-Host-v1.4.0-win-x64.exe
├── CouchLink-Host-v1.4.0-win-x64.exe.sha256
├── CouchLink-Host-v1.4.0-win-x64.exe.sig
├── CouchLink-Host-v1.4.0-win-x64.exe.sig.sha256
├── CouchLink-Updater-v1.4.0-win-x64.exe
├── CouchLink-Updater-v1.4.0-win-x64.exe.sha256
├── CouchLink-Updater-v1.4.0-win-x64.exe.sig
├── CouchLink-Updater-v1.4.0-win-x64.exe.sig.sha256
├── CouchLink-v1.4.0-source.zip
├── CouchLink-v1.4.0-source.zip.sha256
├── SHA256SUMS-v1.4.0.txt
├── RELEASE-INFO-v1.4.0.txt
├── RELEASE-NOTES-v1.4.0.md
├── RELEASE-CHECKLIST-v1.4.0.md
├── RELEASE-VALIDATION-v1.4.0.md
├── INSTALLATION-v1.4.0.md
├── README.md
├── CHANGELOG.md
├── CONTRIBUTING.md
├── SECURITY.md
├── SOURCE-MANIFEST-v1.4.0.sha256
├── LICENSE
├── NOTICE
├── PRIVACY.md
└── TRADEMARKS.md
```

The default Windows build is framework-dependent and requires the .NET 8 Desktop Runtime. Add `-SelfContained` to embed the runtime:

```powershell
.\Build-Release.ps1 -SelfContained
```

You may supply the keystore path and alias on the command line while keeping both passwords out of command history:

```powershell
.\Build-Release.ps1 `
  -KeystorePath "$HOME\Documents\Android\Keys\couchlink-release.jks" `
  -KeyAlias 'couchlink-release'
```

If the target release directory already contains files, the script preserves it and creates a timestamped build directory instead.

If any stage fails, the script prints a red failure summary and writes full details to `release\Build-Release-error.log`.

## Release source and signing safeguards

A normal `Build-Release.ps1` invocation creates a test release package
from the current working tree. It does not require a Git tag.

For an official stable release:

1. Complete Android and Windows validation.
2. Generate and verify the versioned source manifest.
3. Commit the approved release source and confirm a clean working tree.
4. Tag that exact commit as `v1.4.0`.
5. Run `.\Build-Release.ps1 -RequireCleanTaggedSource`.
6. Verify the generated artifacts, signatures, checksums, and source archive.
7. Confirm the worktree and release tag still match the approved source.

The `-RequireCleanTaggedSource` option checks that the working tree
is clean and HEAD matches the release tag before building.

Official releases require the established Android signing keystore
and Windows ECDSA release-signing private key.

Do not use `-SkipAndroidSignatureVerification` or
`-SkipWindowsReleaseSigning` for public release artifacts.

The combined release builder does not replace the complete Android
and Windows regression test suites. Run those separately.

Never commit signing keys, passwords, or other private credentials.

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
  -o .\publish\CouchLink-Host-v1.4.0-win-x64
```

Generate a checksum for the published executable:

```powershell
Get-FileHash `
  .\publish\CouchLink-Host-v1.4.0-win-x64\CouchLink.Host.exe `
  -Algorithm SHA256
```

The root `Build-Release.ps1` script performs this publish and writes the checksum automatically.

## Release integrity

Checksums must be generated from the exact APK, EXE, and source ZIP attached to the release. The combined builder creates per-file checksum files and `SHA256SUMS-v1.4.0.txt` from the final renamed artifacts.

The source archive is built from the current repository tree after `SOURCE-MANIFEST-v1.4.0.sha256` is regenerated and verified. Build output, signing material, IDE state, local properties, logs, and machine-specific release folders are excluded.
