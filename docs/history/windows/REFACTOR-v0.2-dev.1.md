# CouchLink Host v0.2-dev.1 — Launcher-only host (drivers removed)

Input now runs entirely over the phone's Bluetooth HID connection, which works at
the Windows sign-in screen. The host is a post-login launcher/status layer only.

## Removed
- `CouchLink.VirtualHid` — the KMDF virtual HID driver.
- `CouchLink.BootService` — LocalSystem worker, pre-login broker, Virtual HID bridge.
- `CouchLink.SessionHost` — boot-service status bridge and combined health snapshot.
- `WindowsInputController` (SendInput injection) and `MachinePermissionStore`.
- All input wire messages: mouse move/button/scroll, keyboard text, key press, shortcut.
- The "remote input" enable/disable toggle and its UI (tray item, dashboard panel,
  About diagnostics) plus `RemoteInputEnabled` on the snapshot and protocol.
- TCP `45822` (pre-login session) and the input maxima constants.

## Kept
- `CouchLink.Protocol` — discovery, hello/ack, pair request/result, ping/pong,
  launcher action/result.
- `CouchLink.Host.Core` — `DiscoveryAdvertiser`, `SessionServer` (launcher-only),
  `LauncherController` (unchanged), `PairingStore`/`PairingCoordinator`,
  `FrameCodec`, `NetworkAddressHelper`, `CouchLinkHostRuntime`.
- `CouchLink.Host.Wpf` — tray + dashboard, now pointing directly at
  `CouchLinkHostRuntime` (no `SessionHostCoordinator`), with pairing, trusted-device
  management, startup preferences, and diagnostics.

## Notes
- Session capabilities advertised to trusted clients are now
  `["heartbeat", "session_state", "launcher_actions"]`.
- Not built in this environment (no .NET SDK / NuGet access here). Build with
  `dotnet build .\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj` or Visual Studio.
- The Android app still needs a host-client layer (discovery, TCP, pairing UI,
  launcher tiles with HID fallback) to use this — that is the next pass.
