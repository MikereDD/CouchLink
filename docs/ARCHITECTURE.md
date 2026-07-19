# CouchLink Architecture

## System overview

```text
Android Remote
  ├─ UDP 45820 → discovery
  ├─ TCP 45821 → desktop Session Host
  └─ TCP 45822 → Boot Service / pre-login broker
                         │
                         └─ Virtual HID → Windows HID stack
```

## Android remote

The Kotlin/Jetpack Compose client owns discovery, pairing, trusted identity, connection orchestration, touch and keyboard gestures, launcher commands, status feedback, and user-facing diagnostics. It treats the desktop and pre-login endpoints as two execution contexts belonging to one stable host identity.

## Windows components

### `CouchLink.Protocol`

Shared transport envelopes and message contracts. It remains independent of WPF, services, and input implementation details.

### `CouchLink.Host.Core`

The signed-in desktop runtime: discovery, pairing, trusted sessions, standard input routing, launcher launch-or-focus behavior, command handling, and diagnostics.

### `CouchLink.SessionHost`

Coordinates the user-session host and reports combined desktop/Boot Service state to the dashboard.

### `CouchLink.Host.Wpf`

The premium dashboard and tray application. It configures and observes the host but does not define protocol contracts.

### `CouchLink.BootService`

An automatically started LocalSystem service responsible for machine state, the pre-login trusted-session endpoint, permission mirroring, and the Virtual HID bridge.

### `CouchLink.VirtualHid`

A signed KMDF/VHF virtual mouse and boot keyboard. It is the only CouchLink input path intended to reach the Winlogon secure desktop.

## Connection lifecycle

1. Android discovers a stable host identity on UDP `45820`.
2. While Windows is signed in, Android maintains a persistent trusted session on TCP `45821`.
3. When Windows locks or reaches sign-in, Android reconnects to TCP `45822`.
4. After successful login, the Boot Service signals that the desktop session is available.
5. Android leaves `45822`, probes the desktop endpoint, and resumes on `45821`.

## Security invariants

- Unpaired devices cannot control the host.
- Pairing is never offered on the pre-login endpoint.
- Windows credentials are neither stored nor interpreted by CouchLink.
- Machine-level permission gates can disable all remote input or pre-login control.
- Protocol messages are versioned, framed, and validated before command execution.
- Kernel input accepts only the fixed HID contract defined by the shared header.
