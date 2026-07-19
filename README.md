# CouchLink

**Your Windows PC, from the couch.**

CouchLink is a premium, local-first control system for a Windows living-room gaming PC. It pairs a Windows host with an Android phone or tablet that acts as a touchpad, keyboard, launcher deck, media remote, and recovery console.

Steam is the primary launcher. CouchLink also targets GOG Galaxy, EA app, Ubisoft Connect, Rockstar Games Launcher, Amazon Games, and Epic Games Launcher.

## Current milestone

`v0.1-dev.12.1 — Virtual HID Safety Review`

CouchLink now provides a trusted persistent Android-to-Windows session, remote mouse and keyboard control, Steam-first launcher actions, support for the major secondary launchers, a customizable control foundation, and the approved premium identity direction. Remote input remains protected by the local Windows host lock.

## Repository layout

```text
CouchLink/
├── docs/
│   ├── ARCHITECTURE.md
│   ├── DECISIONS.md
│   ├── PROTOCOL.md
│   └── ROADMAP.md
├── src/
│   ├── android/
│   └── windows/
│       ├── CouchLink.Host.Core/
│       ├── CouchLink.Host.Wpf/
│       ├── CouchLink.Protocol/
│       └── CouchLink.sln
└── tests/
```

## Product boundaries

- No mandatory account
- No cloud dependency
- No subscription requirement
- No telemetry by default
- New devices require explicit pairing approval
- Windows host continues operating when its dashboard is closed
- Android is the first remote platform
- Steam receives primary placement without making other launchers second-class technically

## Initial development host

Development begins on **Netzach**, the Windows laptop. Final living-room and 4K validation will happen on the custom Steam PC after assembly.


## Windows architecture

CouchLink now separates its Windows responsibilities into a Boot Service foundation, Session Host coordinator, host core, protocol library, and premium WPF dashboard. The Boot Service starts independently of the signed-in desktop and currently publishes health/state information only. See `docs/BOOT-SERVICE.md`.


Current development milestone: **v0.1-dev.12.1 — Virtual HID Safety Review**.
