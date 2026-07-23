# CouchLink Android v1.1-dev.1

## Windows launcher-host integration

- Added UDP discovery for CouchLink Host on port 45820.
- Added framed TCP sessions on port 45821 using protocol version 1.
- Added persistent client identity and trusted-host pairing token storage.
- Added six-digit pairing flow using the code displayed by the Windows host.
- Added heartbeat, reconnect, launcher result feedback, and launcher state tracking.
- Launcher tiles now prefer the Windows host for launch-or-focus actions.
- Bluetooth HID launcher commands remain available as an automatic fallback.
- Bluetooth keyboard, mouse, touchpad, shortcuts, and sign-in behavior remain independent of the Windows host.
- Added a Windows launcher-host status panel to the Android Home screen.

## Build

- Android version: 1.1-dev.1
- Version code: 101
- Intended host: CouchLink Windows Host 0.2-dev.1
