# Pre-Login Connectivity

CouchLink provides real pointer and keyboard control on the Windows lock and sign-in screens through a signed Virtual HID path. The feature respects the Winlogon security boundary rather than attempting to bypass it.

## Endpoint transition

| Windows state | Endpoint | Android behavior |
|---|---:|---|
| Signed-in desktop | TCP `45821` | Persistent desktop Session Host connection |
| Locked / sign-in required | TCP `45822` | Persistent Boot Service connection |
| Desktop becomes available | `45822` → `45821` | Clean handoff and automatic reconnect |

Both endpoints advertise the same stable host identity. Android keys a session to that identity, not to a temporary port number.

## Trust model

Trusted-device tokens are mirrored from the signed-in user's CouchLink store to:

```text
%ProgramData%\CouchLink\trusted-devices.json
```

The Boot Service authenticates only devices already approved through the desktop host. New pairing is unavailable before login.

## Input path

```text
Android command
  → trusted TCP 45822 session
  → Boot Service validation
  → VirtualHidBridge
  → fixed-size IOCTL payload
  → KMDF/VHF driver
  → Windows HID stack
  → lock or sign-in desktop
```

User32 mechanisms such as `SendInput`, `mouse_event`, and `SetCursorPos` cannot cross from Session 0 to the Winlogon secure desktop. Virtual HID works because input enters through the operating system's real HID path.

## Permission gates

Pre-login input is accepted only when both of these machine permissions are true:

- `RemoteInputEnabled`
- `PreLoginControlEnabled`

The Virtual HID driver must also be installed, running, and reachable. When any requirement is missing, the broker advertises remote input as unavailable and rejects control messages without terminating the trusted status session.

## Authentication boundary

CouchLink never knows the Windows PIN or password. It can type the same keystrokes as a physical keyboard; Windows alone decides whether those credentials are valid.

## Verified handoff behavior

The accepted baseline performs this sequence without a reconnect storm:

1. Desktop session on `45821`.
2. Lock transition to `45822` with `SignInRequired`.
3. PIN entry through Virtual HID.
4. `desktop_session_available` handoff signal.
5. Probe of `45822` reporting `DesktopAvailable`.
6. Persistent reconnection to `45821` with `DesktopReady`.
