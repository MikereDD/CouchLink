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
    └── launcher actions and state

Windows host
├── discovery advertisement
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
