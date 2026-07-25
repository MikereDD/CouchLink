# CouchLink TV Remote roadmap

Approved visual reference: `docs/images/couchlink-tv-remote-mockup.png`.

## Phase 1 — discovery and reachability

- Discover Android/Google TV Remote services with Android NSD.
- Allow manual IP/hostname entry.
- Remember the selected TV.
- Probe the pairing and remote-control services.

## Phase 2 — secure pairing

- Generate a CouchLink client certificate and private key on-device.
- Store credentials in Android Keystore-backed encrypted storage.
- Perform the Android TV Remote v2 pairing exchange.
- Prompt for the code displayed by the TV.
- Reconnect without repeating pairing.

## Phase 3 — core remote

- D-pad, OK, Back, Home and Menu.
- Volume up/down and mute.
- Play/pause and media navigation.
- Power/standby where supported.

## Phase 4 — extended Hisense testing

- Input switching and input listing.
- Guide and channel controls.
- Google Assistant/voice transport.
- TV state, volume state and diagnostics.
