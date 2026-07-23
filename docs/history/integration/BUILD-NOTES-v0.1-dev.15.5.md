# CouchLink v0.1-dev.15.5

## Scope

Android launcher-host client fixes. Windows launcher-host source and behavior are
unchanged. Bluetooth HID input path is unchanged.

## Fixes

### Pairing is now opt-in and dismissible (main fix)
Previously the client auto-connected to any discovered host, and an untrusted host
produced a modal pairing dialog that could not be dismissed (its dismiss button
looped back into pairing). A CouchLink PC on the network could therefore block the
touchpad and keyboard behind an inescapable prompt.

- Discovery now auto-connects only to hosts with a stored pairing token. Untrusted
  hosts are surfaced as "Tap Pair to enable launching" and never connect on their
  own.
- Added `pairWithHost()` (opt-in) — the only path that opens an untrusted session —
  wired to a new "PAIR WITH HOST" button in Settings and a "PAIR" status pill.
- Added `cancelPairing()`. The pairing dialog's `onDismissRequest` and a new
  "Not now" button now close it; since untrusted hosts are not auto-connected, it
  does not immediately return.
- A stored token rejected by the host (stale/revoked) is now cleared on `hello_ack`,
  so a stale-trust host degrades to an opt-in pairing target instead of
  re-prompting on every reconnect.

### Lifecycle / robustness
- Added `LauncherHostRuntime` singleton; the client is no longer recreated per
  Activity, removing the coroutine-scope leak on rotation and preserving the host
  session across configuration changes.
- `sendEnvelope` now guards the socket write with `runCatching` and surfaces a
  disconnect on failure instead of silently killing the launched coroutine.
- Added a per-host auto-connect backoff so a discovered-but-unreachable host is not
  retried on every 2-second advertisement.
- `submitPairingCode` now checks `isConnected && !isClosed` for a reliable
  open-socket test.

## Versions

- Combined working archive: 0.1-dev.15.5
- Android remote: 1.1-dev.4 (versionCode 104)
- Windows launcher host: 0.2-dev.1 (unchanged)

## Test focus

1. With a CouchLink host running on the LAN and the phone unpaired, confirm the app
   opens to Home/Touchpad/Keyboard with no forced pairing modal; Settings shows the
   host as "PAIR".
2. Tap "PAIR WITH HOST", enter the six-digit code, confirm the session becomes
   trusted and launcher tiles route to the host.
3. Re-open the app; confirm it auto-reconnects to the now-trusted host.
4. Trigger the pairing dialog, tap "Not now", and confirm it stays dismissed.
5. FORGET HOST, confirm it returns to opt-in "PAIR" state.
6. Stop the host mid-session; confirm launchers fall back to Bluetooth HID.
