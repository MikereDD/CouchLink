# CouchLink Changelog

This changelog tracks the combined Android remote and Windows launcher host. Historical one-build notes are preserved under `docs/history/`.

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
