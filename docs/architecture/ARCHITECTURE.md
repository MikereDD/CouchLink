# CouchLink Architecture

## Design principle

CouchLink deliberately separates secure input from rich desktop launcher control. The phone behaves as a real Bluetooth keyboard and mouse, while the optional Windows host provides post-login launcher management over the local network.

```text
Android remote
├── Bluetooth HID
│   ├── keyboard and text entry
│   ├── mouse and touchpad
│   ├── media and Windows shortcuts
│   └── Windows sign-in input
└── Local-network host client
    ├── UDP host discovery :45820
    ├── six-digit trusted pairing
    ├── framed TCP session :45821
    ├── heartbeat and reconnect
    ├── trusted wired-host Wake-on-LAN
    └── launcher actions and state

Windows host
├── discovery advertisement with preferred wired MAC identity
├── trusted-device store
├── launcher detection and control
├── launch/focus/close results
├── tray and startup behavior
└── dashboard and diagnostics
```

## Connection independence

Losing the Windows host must not disable Bluetooth input. Losing Bluetooth HID must not falsely report that the launcher host is disconnected. The Android UI therefore presents separate status panels for each route.

## Security boundary

- Bluetooth HID uses the operating systems' standard pairing and HID stacks.
- The Windows host accepts only trusted clients paired with a six-digit code and persistent token.
- Launcher-host traffic remains on the local network.
- The Windows host does not inject keyboard or mouse input and does not bypass Windows authentication.
- No cloud account, relay, analytics, or telemetry is required.

## Launcher behavior

When the host is connected, launcher tiles request launch-or-focus behavior and receive real result/state feedback. When the host is unavailable, supported launcher actions may fall back to Bluetooth keyboard commands.


## Wake PC trust and lifecycle

The Windows Host advertises its active network identity, preferring wired Ethernet over Wi-Fi. Android may display that identity while discovering an untrusted host, but it persists the MAC address only after the existing pairing token has opened a trusted session.

When a trusted Host is offline, Android can send a standard Wake-on-LAN magic packet to UDP ports 9 and 7 through the limited broadcast address and every active interface-specific broadcast address. It sends repeated bursts, keeps normal UDP discovery running, and waits for the trusted Host to advertise and authenticate again.

Wake PC does not weaken the launcher-host trust boundary:

- magic packets contain only the target MAC address;
- no pairing token is transmitted in the wake packet;
- untrusted discovered hosts do not receive a persistent Wake PC action;
- forgetting a Host removes its token, endpoint, and saved wake identity together.

The supported first-release target is a wired Windows PC using classic S3 sleep. Hybrid Sleep and automatic hibernation can prevent consistent wake behavior and are outside the claimed path.
