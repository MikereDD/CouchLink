<div align="center">

<img src="src/android/docs/assets/couchlink-icon.png" alt="CouchLink" width="150" />

# CouchLink

### A living-room Windows launcher with an Android Bluetooth remote.

![Android](https://img.shields.io/badge/Android-1.1--dev.2-ff8617?style=for-the-badge&logo=android&logoColor=white)
![Windows Host](https://img.shields.io/badge/Windows_Host-0.2--dev.1-0078D4?style=for-the-badge&logo=windows11&logoColor=white)
![Protocol](https://img.shields.io/badge/Protocol-1-18181B?style=for-the-badge)
![License](https://img.shields.io/badge/License-Apache_2.0-ff8617?style=for-the-badge)

**Bluetooth input at sign-in. Rich launcher control after sign-in. No cloud relay.**

</div>

---

## Overview

CouchLink uses two independent connection paths so each part can do what it does best:

| Path | Responsibility | Works at Windows sign-in? |
|---|---|---|
| **Android Bluetooth HID** | Keyboard, mouse, touchpad, media keys, shortcuts, and secure sign-in input | **Yes** |
| **Windows launcher host** | Local discovery, trusted pairing, launch/focus/close actions, and real launcher-state feedback | After desktop login |

The Android remote remains useful when the Windows host is closed or unavailable. Launcher tiles prefer the host when connected and retain Bluetooth-command fallback behavior.

## Repository layout

```text
CouchLink/
├── src/
│   ├── android/              Android Bluetooth HID remote and launcher client
│   └── windows/              Optional Windows launcher host
├── docs/
│   ├── architecture/         Current system design and protocol boundaries
│   ├── building/             Build and test instructions
│   ├── release/              Signing and release procedures
│   └── history/              Preserved development notes and old manifests
├── CHANGELOG.md              Current cross-platform changelog
├── PRIVACY.md                Repository-wide privacy statement
└── LICENSE                   Apache License 2.0
```

## Supported launchers

Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar Games Launcher, Epic Games Launcher, and Amazon Games.

## Build

### Android

```powershell
cd .\src\android
.\gradlew.bat clean :app:assembleDebug
```

Or open `src/android` directly in Android Studio.

### Windows host

```powershell
cd .\src\windows
dotnet build .\CouchLink.sln -c Debug
dotnet run --project .\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj
```

See [BUILDING.md](docs/building/BUILDING.md) for release and publish commands.

## Current development baseline

| Component | Version | Status |
|---|---:|---|
| Android remote | `1.1-dev.3` | Bluetooth HID + Windows launcher-host integration |
| Windows host | `0.2-dev.1` | Local-first launcher host |
| Shared protocol | `1` | UDP discovery + framed TCP session |
| Working integration archive | `0.1-dev.15.4` | Windows Launcher Host settings restored |

## Documentation

- [Architecture](docs/architecture/ARCHITECTURE.md)
- [Build instructions](docs/building/BUILDING.md)
- [Test checklist](docs/building/TESTING.md)
- [Android signing](docs/release/ANDROID-SIGNING.md)
- [Changelog](CHANGELOG.md)
- [Privacy](PRIVACY.md)
- [Development history](docs/history/README.md)

## License

CouchLink is licensed under the [Apache License 2.0](LICENSE).
