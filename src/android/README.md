<div align="center">

<img src="docs/assets/couchlink-icon.png" alt="CouchLink Android" width="140" />

# CouchLink Android Remote

**Bluetooth HID input plus the local Windows Launcher Host client.**

Version `1.1` · Version code `106` · Android 9+ · Protocol `1`

</div>

## Responsibilities

- Present the phone as a real Bluetooth keyboard and mouse.
- Provide touchpad, keyboard, media, navigation, and Windows shortcut controls.
- Continue working at the Windows sign-in screen without the host.
- Discover and pair with the optional Windows Launcher Host over the LAN.
- Prefer host-backed launch/focus/close actions with live state feedback.
- Fall back to Bluetooth launcher commands when the host is unavailable.

## Build

```powershell
.\gradlew.bat clean :app:assembleDebug
```

Open this directory directly in Android Studio for normal test builds. For a signed APK only, run `.\tools\Build-SignedRelease.ps1`; for the signed APK, Windows EXE, and all SHA-256 files together, run `.\Build-Release.ps1` from the repository root. Signing details are covered by [ANDROID-SIGNING.md](../../docs/release/ANDROID-SIGNING.md).

## Connection model

Bluetooth HID and Windows Launcher Host connectivity are independent. A host outage must not disable touchpad or keyboard input, and a Bluetooth outage must not be displayed as a host outage.

## Documentation

- [Repository overview](../../README.md)
- [Installation and pairing](../../docs/release/INSTALLATION.md)
- [Architecture](../../docs/architecture/ARCHITECTURE.md)
- [Integration testing](../../docs/building/TESTING.md)
- [Release notes](../../docs/release/RELEASE-NOTES-v1.1.md)
- [Privacy](../../PRIVACY.md)
- [Apache 2.0 license](../../LICENSE)
- [Trademark and brand policy](../../TRADEMARKS.md)

## License and branding

The Android source is licensed under the [Apache License 2.0](../../LICENSE). CouchLink names, logos, icons, artwork, screenshots, and official branding are covered separately by the [CouchLink Trademark and Brand Policy](../../TRADEMARKS.md).
