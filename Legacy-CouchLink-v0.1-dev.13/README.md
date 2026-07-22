# CouchLink

<p align="center">
  <img src="docs/assets/couchlink-cl-mark-reference.png" alt="CouchLink logo" width="180">
</p>

<h3 align="center">Your Windows PC, from the couch.</h3>

<p align="center">
  <strong>Local-first Android remote control for a Windows living-room gaming PC.</strong><br>
  Touchpad · Keyboard · Launcher deck · Secure pre-login control · No cloud account
</p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-0.1--dev.13-f97316">
  <img alt="Protocol" src="https://img.shields.io/badge/protocol-1-64748b">
  <img alt="Windows" src="https://img.shields.io/badge/host-Windows%2011-0078d4">
  <img alt="Android" src="https://img.shields.io/badge/remote-Android-3ddc84">
  <img alt="Privacy" src="https://img.shields.io/badge/privacy-local--first-22c55e">
</p>

---

## Current milestone

### **v0.1-dev.13 — Launcher Behavior & Product Polish**

CouchLink now feels and behaves like a complete product prototype: a polished Android control surface, trusted persistent sessions, Windows launch-or-focus actions, structured launcher feedback, secure lock-screen and sign-in input through Virtual HID, automatic session handoff after login, and live diagnostics on both platforms.

## What works today

| Area | Capability |
|---|---|
| **Connection** | LAN discovery, explicit pairing, trusted-device identity, persistent sessions, heartbeat monitoring, automatic reconnection |
| **Remote input** | Touchpad, clicks, drag lock, scrolling, text entry, keyboard commands, media/system shortcuts |
| **Launchers** | Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar Games Launcher, Epic Games Launcher, Amazon Games |
| **Launcher behavior** | Launch closed apps, focus running apps, detect unavailable apps, return clear status to Android |
| **Pre-login** | Lock-screen and sign-in mouse/keyboard control through the signed KMDF/VHF Virtual HID path |
| **Handoff** | Automatic transition between Boot Service port `45822` and desktop Session Host port `45821` |
| **Windows host** | WPF dashboard, tray operation, startup support, trusted-device management, live About/diagnostics |
| **Android client** | Premium Home, Touchpad, Keyboard, Settings, About, and diagnostics views |

## Product principles

- **Local-first:** operation stays on the private network.
- **No mandatory account, subscription, or cloud dependency.**
- **No telemetry by default.**
- **Explicit trust:** new devices require pairing approval.
- **Windows remains in control:** remote input can be disabled locally.
- **Authentication is never bypassed:** CouchLink supplies HID input; Windows still validates the PIN or password.

## Architecture at a glance

```text
Android Remote
    │
    ├── UDP 45820 ─────────────── Host discovery
    │
    ├── TCP 45821 ─────────────── Desktop Session Host
    │                               ├── trusted sessions
    │                               ├── launcher control
    │                               └── standard desktop input
    │
    └── TCP 45822 ─────────────── Boot Service / pre-login broker
                                    └── signed Virtual HID bridge
                                            └── Windows HID stack
```

The Windows side is deliberately split into a user-session host, a LocalSystem Boot Service, shared protocol contracts, and the Virtual HID driver. See [Architecture](docs/ARCHITECTURE.md), [Boot Service](docs/BOOT-SERVICE.md), and [Pre-Login Connectivity](docs/PRELOGIN-CONNECTIVITY.md).

## Repository layout

```text
CouchLink/
├── docs/                         Architecture, protocol, decisions, and roadmap
├── src/
│   ├── android/                  Kotlin + Jetpack Compose remote
│   └── windows/                  WPF host, services, protocol, and Virtual HID
├── tools/                        Development, firewall, service, and diagnostic scripts
├── CHANGELOG.md                  Milestone history
└── TEST-v*.md                    Historical validation checklists
```

## Quick build

### Windows host

```powershell
dotnet build .\src\windows\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj
```

### Android remote

```powershell
.\src\android\gradlew.bat -p .\src\android :app:assembleDebug
```

APK output:

```text
src\android\app\build\outputs\apk\debug\app-debug.apk
```

> The Virtual HID driver uses the Windows Driver Kit and must be built, signed, and installed separately. Follow [BUILD-SIGN-TEST.md](src/windows/CouchLink.VirtualHid/BUILD-SIGN-TEST.md).

## Development target

Primary development and validation currently run on **Netzach**. Final living-room and 4K validation will move to the dedicated custom Steam PC after assembly.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Protocol](docs/PROTOCOL.md)
- [Boot Service](docs/BOOT-SERVICE.md)
- [Pre-Login Connectivity](docs/PRELOGIN-CONNECTIVITY.md)
- [Virtual HID Foundation](docs/VIRTUAL-HID-FOUNDATION.md)
- [Design Decisions](docs/DECISIONS.md)
- [Roadmap](docs/ROADMAP.md)
- [Changelog](CHANGELOG.md)
