# CouchLink 1.1 Release Notes

**Stable release:** July 24, 2026

CouchLink 1.1 turns the original Bluetooth remote into a complete dual-path couch-gaming companion while preserving the reliability of direct Bluetooth HID input.

## Major additions

- Added the optional Windows Launcher Host for local discovery, trusted pairing, and real launcher-state feedback.
- Added launch, focus, and close control for Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar Games Launcher, Epic Games Launcher, and Amazon Games.
- Added automatic trusted reconnect, heartbeat monitoring, and clear host diagnostics.
- Added a dedicated Windows Launcher Host settings panel on Android.
- Preserved Bluetooth keyboard, mouse, touchpad, shortcuts, media controls, and Windows sign-in behavior as an independent path.
- Added immediate Bluetooth launcher fallback when host delivery fails.

## Reliability work

- Moved the launcher-host runtime to process scope so Android activity recreation does not discard the active session.
- Added per-connection socket ownership so an older connection job cannot close a replacement session.
- Treated missing or replaced output streams as explicit failures rather than silent command loss.
- Changed untrusted-host pairing to an explicit user action and added stale-token cleanup.
- Improved reconnect backoff, heartbeat shutdown, and status-copy accuracy.

## Release and project polish

- Synchronized Android and Windows components at version 1.1.
- Added production build, signing, installation, release-checklist, privacy, security, contribution, license, notice, and trademark documentation.
- Preserved all historical development notes and manifests under `docs/history/`.

## Compatibility

- Android 9 or newer
- Windows 10 or Windows 11
- Protocol version 1
- Windows Launcher Host features require both devices on the same trusted local network

## Distribution artifacts

The official release builder produces:

- `CouchLink-v1.1.apk`
- `CouchLink-Host-v1.1-win-x64.exe`
- `CouchLink-v1.1-source.zip`
- per-artifact `.sha256` files
- `SHA256SUMS-v1.1.txt`
- release notes, installation instructions, changelog, source manifest, license, notice, privacy, security, contribution, and trademark documents

The default Windows executable is a framework-dependent single-file package and requires the .NET 8 Desktop Runtime.
