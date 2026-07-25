<div align="center">

<img src="docs/assets/couchlink-icon.png" alt="CouchLink Android" width="140" />

# CouchLink Android Remote

**Bluetooth HID input plus the local Windows launcher-host client.**

Version `1.1-dev.1` · Android 9+ · Protocol `1`

</div>

## Responsibilities

- Present the phone as a real Bluetooth keyboard and mouse.
- Provide touchpad, keyboard, media, navigation, and Windows shortcut controls.
- Continue working at the Windows sign-in screen without the host.
- Discover and pair with the optional Windows launcher host over the LAN.
- Prefer host-backed launch/focus/close actions with real state feedback.
- Fall back to Bluetooth launcher commands when the host is unavailable.

## Build

```powershell
.\gradlew.bat clean :app:assembleDebug
```

Open this directory directly in Android Studio for normal test builds.

## Connection model

Bluetooth HID and Windows launcher-host connectivity are independent. A host outage must not disable touchpad or keyboard input, and a Bluetooth outage must not be displayed as a host outage.

## Documentation

- [Repository overview](../../README.md)
- [Architecture](../../docs/architecture/ARCHITECTURE.md)
- [Integration testing](../../docs/building/TESTING.md)
- [Android signing](../../docs/release/ANDROID-SIGNING.md)
- [Current changelog](../../CHANGELOG.md)
- [Historical Android notes](../../docs/history/android/)
- [Privacy](../../PRIVACY.md)
- [Apache 2.0 license](../../LICENSE)
