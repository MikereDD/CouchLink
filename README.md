# CouchLink Remote 1.0

CouchLink turns an Android phone into a persistent Bluetooth keyboard, mouse, and living-room launcher remote for Windows. It works at the Windows sign-in screen through the standard Windows Bluetooth HID stack and does not require a Windows companion app, custom driver, cloud account, or telemetry.

## Release configuration

- Version: `1.0`
- versionCode: `100`
- Application ID: `dev.typezero.couchlink.remote`
- Debug application ID: `dev.typezero.couchlink.remote.debug`
- Minimum Android: Android 9 / API 28
- Target / compile SDK: 36
- Java toolchain: JDK 17

## Signed release

Read [SIGNING.md](SIGNING.md), then run:

```powershell
.\tools\Build-SignedRelease.ps1 -KeystorePath .\signing\couchlink-release.jks
.\tools\Verify-SignedRelease.ps1
```

The signed APK and checksum are written to `release\`.

## Debug build

```powershell
.\gradlew.bat clean :app:assembleDebug
```

The debug package uses the `.debug` application ID suffix so it can coexist with the signed stable release without creating signing conflicts.

## Release gate

Complete [RELEASE-CHECKLIST-v1.0.md](RELEASE-CHECKLIST-v1.0.md) before tagging or publishing version 1.0.
