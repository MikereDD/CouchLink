# CouchLink TV Remote roadmap

Approved visual reference: `docs/images/couchlink-tv-remote-mockup.png`.

## v1.3.4 direction — selectable TV OS providers

CouchLink TV Remote will use one shared remote experience with selectable TV OS/provider backends.

The existing Google / Android TV implementation remains the reference backend and must keep its current behavior while the provider architecture is introduced.

Initial roadmap sequence:

1. Google TV / Android TV — existing implementation
2. Roku
3. Samsung Tizen
4. LG webOS
5. Fire TV
6. Apple TV
7. Additional TV platforms where practical

The implementation order may change as protocol feasibility, maintenance cost, and test hardware become clearer.

The TV OS is selected during TV setup. CouchLink should remember the selected TV, its provider type, pairing/trust state, and provider-specific connection data without mixing credentials between providers.

## Provider architecture

Introduce a provider boundary between the shared CouchLink remote UI and TV-specific protocols.

Shared responsibilities:

- provider identity and display name
- discovery and manual IP/hostname entry
- pairing / authorization state
- connect / disconnect / reconnect
- connection and error status
- capability reporting
- common remote command dispatch
- remembered-device persistence
- provider-specific diagnostics without leaking protocol details into the UI

The shared remote UI must not assume every provider implements every command.

Capabilities should determine which controls are enabled or shown.

Initial common command surface:

- D-pad: Up, Down, Left, Right, OK / Select
- Back
- Home
- Menu / Settings where supported
- Power / standby where supported
- Volume Up
- Volume Down
- Mute
- Play / Pause
- Rewind
- Fast Forward
- Channel Up / Down where supported
- Input listing / switching where supported
- Text / keyboard input where supported
- App launching where supported

Provider-specific features may extend this surface without forcing equivalent behavior onto other TV operating systems.

## Phase 1 — provider abstraction

- Define the TV provider interface / contract.
- Define shared connection, pairing, device, capability, and command models.
- Adapt the existing Google / Android TV implementation behind the provider contract.
- Preserve existing Google TV discovery, pairing, trust, reconnect, remote commands, and diagnostics.
- Keep provider protocol code out of the shared remote UI.
- Add provider-selection state and persistence.
- Add tests proving the Google TV backend behaves the same after the refactor.

This phase is complete only when the existing Google TV remote works exactly as it did before the provider abstraction.

## Phase 2 — TV OS selection UX

- Add TV OS / provider selection to remote setup.
- Show provider-aware discovery and manual-address flows.
- Remember the chosen provider for each saved TV.
- Surface provider capability differences cleanly.
- Prevent unsupported controls from appearing functional.
- Keep the existing CouchLink remote visual design consistent across providers.

## Phase 3 — Roku backend

- Implement Roku discovery.
- Implement connection/session handling.
- Map supported CouchLink remote commands.
- Report Roku capabilities.
- Add provider-specific tests and diagnostics.

## Phase 4 — Samsung Tizen backend

- Implement Samsung Tizen discovery.
- Implement pairing / authorization.
- Implement connection/session handling.
- Map supported CouchLink remote commands.
- Report Tizen capabilities.
- Add provider-specific tests and diagnostics.

## Phase 5 — LG webOS backend

The LG webOS provider should be isolated enough that a contributor can work on it without needing to modify the Google TV implementation or understand unrelated CouchLink subsystems.

Contributor boundary:

- LG/webOS discovery
- LG pairing / authorization
- LG connection and session handling
- LG command mappings
- LG capability reporting
- LG-specific diagnostics and tests

CouchLink supplies the shared provider contract, device model, remote UI integration, persistence hooks, and capability-driven control surface.

The first LG implementation should target local-network control and preserve CouchLink's rule that credentials and trust material remain device-local.

## Phase 6 — Fire TV backend

- Evaluate the appropriate local-control transport independently from Google TV.
- Implement discovery / manual addressing as supported.
- Implement pairing / authorization as required.
- Map supported commands and report capabilities.
- Add provider-specific tests and diagnostics.

## Phase 7 — Apple TV backend

- Evaluate a maintainable local-network control path before selecting an implementation.
- Implement discovery / manual addressing where supported.
- Implement pairing / authorization and session handling.
- Map supported CouchLink commands through the shared provider contract.
- Report Apple TV capabilities rather than assuming parity with other providers.
- Keep Apple TV credentials/trust material isolated from every other provider.
- Add provider-specific tests and diagnostics.

## Existing Google / Android TV foundation

The current backend already established the following roadmap work and remains the reference implementation:

### Discovery and reachability

- Discover Android/Google TV Remote services with Android NSD.
- Allow manual IP/hostname entry.
- Remember the selected TV.
- Probe the pairing and remote-control services.

### Secure pairing

- Generate a CouchLink client certificate and private key on-device.
- Store credentials in Android Keystore-backed encrypted storage.
- Perform the Android TV Remote v2 pairing exchange.
- Prompt for the code displayed by the TV.
- Reconnect without repeating pairing.

### Core remote

- D-pad, OK, Back, Home and Menu.
- Volume up/down and mute.
- Play/pause and media navigation.
- Power/standby where supported.

### Extended Hisense / Google TV work

- Input switching and input listing.
- Guide and channel controls.
- Google Assistant/voice transport.
- TV state, volume state and diagnostics.

## Design rules

- One CouchLink remote experience; multiple provider implementations.
- Never regress an existing provider to add another provider.
- Shared UI code must not contain TV-protocol-specific logic.
- Provider credentials and pairing state must remain isolated by provider/device.
- Capability reporting controls feature availability.
- Unsupported actions must fail clearly rather than silently pretending to work.
- Local-network control remains preferred; cloud dependencies are not introduced merely for platform parity.
- New providers should be independently testable.
- Provider additions should be reviewable as focused changes rather than rewrites of the entire TV Remote feature.
