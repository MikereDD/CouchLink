# CouchLink Changelog

## v1.2-dev.3.3 — post-fix hardening

- invalidate the active connection epoch whenever the TV remote closes so stale reader threads cannot report late errors
- restrict persistent wire tracing to debug builds and cap the trace file at approximately 256 KB
- report the active package name and build version during Google TV configuration
- regenerate the source integrity manifest after the TV remote command-delivery fixes

## v1.2-dev.3.3

- Changed Google TV taps from `SHORT` to explicit key-down/key-up delivery for firmware compatibility.
- Corrected remote feature negotiation and `RemoteSetActive` replies.
- Added visible command transmission counters and remote-error reporting.

## v1.2-dev.3 — First live Google TV controls

- moved TV discovery, manual addressing, diagnostics, pairing, reconnect, and forget actions into Settings
- replaced the development diagnostics tab with the approved CouchLink TV Remote control layout
- added the pinned TLS remote-control connection on port 6466
- added automatic reconnect for the remembered paired TV
- added D-pad, OK, Home, Back, Menu, Input, power, volume, mute, channel, Guide, and playback commands
- added Android TV Remote v2 protobuf messages and ping/configuration handling
- preserved the TV private identity using Android Keystore-backed encrypted storage

## v1.2-dev.2 — Google TV certificate pairing

- added the real Android TV Remote v2 pairing exchange over TLS on port 6467
- added a CouchLink RSA client identity stored in Android Keystore
- added the TV-displayed six-character hexadecimal pairing-code workflow
- added pairing-state, cancellation, validation, and error reporting to the TV Remote screen
- stored the paired TV host and server-certificate fingerprint for the upcoming pinned remote connection
- added protobuf-lite generation for the Google Polo pairing protocol

## v1.2-dev.1 — TV Remote discovery prototype

- added a dedicated TV Remote destination to the Android bottom navigation
- added Android/Google TV discovery through NSD service browsing
- added manual TV IP or hostname selection
- added remembered TV selection
- added reachability tests for the Android TV pairing and remote-control services
- added the approved TV Remote visual reference and phased implementation roadmap

This changelog tracks the combined Android remote and Windows launcher host. Historical one-build notes are preserved under `docs/history/`.

## 1.1 — 2026-07-24

### Android 1.1

- Promoted the tested Android 1.1 development line to stable version metadata (`versionCode 106`).
- Preserved direct Bluetooth HID input for keyboard, mouse, touchpad, shortcuts, media controls, and Windows sign-in.
- Preserved the process-wide Launcher Host runtime, explicit pairing, trusted reconnect, heartbeat, and single-shot Bluetooth launcher fallback.

### Windows Launcher Host 1.1

- Promoted the optional local Windows Launcher Host to the unified CouchLink 1.1 product version.
- Preserved protocol version 1, local discovery, six-digit trusted pairing, launcher actions, state feedback, tray behavior, startup preferences, and diagnostics.

### Stable release

- Reworked root and component READMEs for a public stable release.
- Added installation, 1.1 release notes, and final release checklist documents.
- Added `SECURITY.md`, `CONTRIBUTING.md`, and `NOTICE`.
- Corrected privacy documentation to cover both Bluetooth HID and local Launcher Host networking.
- Expanded build and integration validation instructions.
- Added a repository-level `Build-Release.ps1` that builds the signed Android APK and single-file Windows EXE in one run.
- Fixed the combined builder to pass Android helper options with named hashtable splatting instead of positional array splatting.
- Added an explicit red failure summary and `release\Build-Release-error.log` so release failures cannot return silently.
- Added secure keystore prompting, automatic APK signature verification, individual APK/EXE SHA-256 files, and a combined checksum list.
- Updated the Android signing helper so the keystore path can be entered interactively instead of being a mandatory parameter.
- Preserved existing release output by creating a timestamped directory when the selected destination already contains files.
- Validated the combined signed release build on Windows: Android `BUILD SUCCESSFUL`, APK signature verified, and Windows single-file publish completed without warnings.
- Pinned the Android Gradle wrapper to supported Gradle `8.13` for Android Gradle Plugin `8.11.1`.
- Removed the Kotlin annotation-target and deprecated path-drawing warnings and explicitly preserved the already-unstrippable AndroidX native library.
- Pinned release verification to the official CouchLink signing-certificate SHA-256 fingerprint.
- Added deterministic source-manifest update/verification tools and clean source-archive packaging to the combined release builder.
- Added final release validation and publishing documentation.
- Runtime behavior remains based on the tested dev.15.6 reliability baseline.

## Documentation update — Trademark and brand policy

- Added a root-level `TRADEMARKS.md` separating Apache-licensed source code from reserved CouchLink branding.
- Reserved the CouchLink name, CL identity, icons, logos, artwork, screenshots, and official visual identity.
- Clarified rename-and-rebrand requirements for modified distributions and independent forks.
- Added third-party trademark attribution and linked the policy from the root, Android, and Windows READMEs.
- No application behavior or component version changed.

## 0.1-dev.15.6 — Launcher-host reliability hardening

### Android 1.1-dev.5

- Preserved the process-wide launcher-host session across Activity recreation and rotation.
- Added per-command host-send failure reporting with immediate Bluetooth HID launcher fallback.
- Made missing or replaced output streams explicit send failures instead of silent returns.
- Added pairing-send failure feedback without relying on `Socket.isConnected`.
- Gave each TCP connection job ownership of its own socket and stream to prevent an older job from closing a newer session.
- Prevented discovery timeout copy from overwriting a useful discovered/disconnected status.
- Preserved opt-in pairing, trusted-host reconnect, and the Windows Host 0.2-dev.1 source unchanged.

## 0.1-dev.15.5 — Pairing opt-in and host-client lifecycle groundwork

### Android 1.1-dev.4

- Changed untrusted host discovery to explicit user-initiated pairing.
- Added cancelable pairing and stale-token cleanup.
- Introduced a process-wide launcher-host runtime and reconnect backoff.
- Improved host write-failure visibility.

## 0.1-dev.15.4 — Bluetooth HID status copy fix

### Android 1.1-dev.3

- Prevented repeated controller startup calls from overwriting an established Bluetooth HID connection message.
- Connected and already-registered HID sessions now report **Bluetooth HID profile active.**
- Preserved the opening and registration messages only while those operations are actually in progress.
- No Windows host behavior changed.

## 0.1-dev.15.3 — Windows Launcher Host settings

### Android 1.1-dev.2

- Restored a dedicated **Windows Launcher Host** management panel in Living-room settings.
- Added current host status, PC name, endpoint, host version, protocol message, and Bluetooth HID fallback information.
- Added **Reconnect / Find Host** and **Forget Host** controls.
- Extended copied diagnostics with launcher-host discovery and connection details.
- Updated About copy to describe the current dual-path architecture accurately.
- Fixed a duplicated TCP frame-length read in the launcher-host client.
- Preserved Bluetooth HID as the independent keyboard, mouse, touchpad, shortcut, and sign-in route.

## 0.1-dev.15.2 — Documentation and repository cleanup

- Added a repository-level README describing the current dual-path architecture.
- Consolidated current Android and Windows changes into one changelog.
- Moved legacy Android-only build notes, validation reports, checklists, and manifests to `docs/history/android/`.
- Moved the Windows host refactor note to `docs/history/windows/`.
- Moved integration snapshot notes to `docs/history/integration/`.
- Promoted Apache 2.0 and the privacy statement to repository-wide root documents.
- Added current architecture, build, testing, and documentation-index pages.
- Updated component READMEs to describe their present responsibilities and cross-links.
- No runtime behavior changed in this cleanup build.

## 0.1-dev.15.1 — Windows XAML build fix

- Corrected the unescaped ampersand in `Devices & pairing` within `MainWindow.xaml`.
- Restored a successful Windows WPF build without changing host behavior.

## 0.1-dev.15 — Android/Windows integration baseline

### Android 1.1-dev.1

- Added UDP discovery for CouchLink Host on port `45820`.
- Added framed TCP sessions on port `45821` using protocol version `1`.
- Added persistent client identity and trusted-host pairing-token storage.
- Added six-digit pairing using the code displayed by the Windows host.
- Added heartbeat, reconnect, launcher-result feedback, and launcher-state tracking.
- Launcher tiles prefer the Windows host and retain Bluetooth HID fallback.
- Bluetooth keyboard, mouse, touchpad, shortcuts, and sign-in input remain independent of the host.
- Added a dedicated Windows launcher-host status panel on the Android Home screen.

### Windows Host 0.2-dev.1

- Reframed the Windows application as an optional post-login launcher host.
- Preserved LAN discovery, trusted pairing, launcher control, tray integration, startup preferences, diagnostics, and About.
- Removed Windows-side input injection, virtual HID, boot service, and pre-login broker responsibilities.
- Added launch/focus/close result states for all eight supported launchers.

## Android 1.0 stable

- Delivered signed Bluetooth HID remote functionality with touchpad, keyboard, shortcuts, launcher fallback, premium UI, and Apache 2.0 licensing.
- The original release notes and checklists are preserved in `docs/history/android/`.

## v1.2-dev.3.3

- correct Android TV Remote v2 feature negotiation to the canonical 622 mask
- add on-screen and Logcat wire traces for inbound and outbound protobuf frames
- preserve the last wire frame when Abaddon closes the socket
- keep normal button taps on the canonical SHORT key event
