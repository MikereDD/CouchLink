# CouchLink v0.2-dev.12 — Input correctness fixes

No visual or layout changes. This pass fixes behavioral issues found in review;
the orange-outline UI and white launcher marks are unchanged.

## Scroll momentum

- Two-finger scroll previously clamped the whole gesture into a single signed
  byte, so fast swipes flattened to one notch.
- `BluetoothHidController.scroll()` now emits the delta as a series of
  full-scale wheel reports (up to +/-127 each, capped at +/-480), preserving
  swipe momentum. The touchpad's existing +/-480 ceiling now flows through
  intact.

## Text entry reliability

- "Send text" and launcher name typing sent every keystroke back-to-back with
  no pacing, which could overrun the Bluetooth send queue and silently drop
  characters on longer input.
- Keystrokes are now paced (`KEYSTROKE_INTERVAL_MS`) and the full string is
  validated up front, so an unmappable character rejects the request instead of
  sending a partial string.
- Start Search / Run launches now press Enter only after paced typing
  completes, removing a race where Enter could fire before the command finished
  typing.

## Permissions

- Core HID input (register, connect, send reports) now requires only
  `BLUETOOTH_CONNECT`. `BLUETOOTH_ADVERTISE` is requested lazily, right before
  the "Pair Windows" discoverability request, so denying advertise no longer
  disables keyboard and mouse input.

## Foreground service hardening

- `BluetoothHidService` now bails out cleanly if it is re-created by the system
  (START_STICKY) after `BLUETOOTH_CONNECT` was revoked, avoiding a
  foreground-start exception on Android 14+. The Activity re-starts the service
  once permission is restored.

## Build metadata

- Version name: `0.2-dev.12`
- Version code: `44`

## Validation

- Source and resource structure checked; delimiters and imports inspected.
- Dependency versions confirmed to resolve (activity-compose 1.13.0,
  compose-bom 2025.08.00).
- Full Gradle compilation was not run in this environment (the wrapper cannot
  reach `services.gradle.org` / the Google Maven repository here).
