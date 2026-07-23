# CouchLink v0.2-dev.12.1 — Reliability Fix Pass

This pass hardens the v0.2-dev.12 Bluetooth HID changes without altering the approved premium UI.

## Fixed

- Core Bluetooth HID permission requests now ask only for `BLUETOOTH_CONNECT`.
- Pair/discoverability requests ask for `BLUETOOTH_ADVERTISE` only when needed.
- Pairing resumes automatically after advertise permission is granted; a second tap is no longer required.
- The foreground HID service now guards its initialization state and returns `START_NOT_STICKY` when required permission is unavailable.
- Foreground-service startup no longer catches `Throwable`; expected runtime failures are handled as `Exception`.
- Keyboard text pacing increased from 6 ms to 12 ms per character to reduce dropped HID input on slower Bluetooth stacks.
- Version bumped to `0.2-dev.12.1` (`versionCode 45`).

## Preserved

- Brand-orange outlines
- White monochrome launcher icons
- Premium dark UI
- Launcher command completion sequencing
- Up-front text validation
- Scroll delta chunking
