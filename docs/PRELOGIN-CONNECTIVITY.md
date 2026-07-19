# Pre-Login Connectivity

CouchLink v0.1-dev.12 adds real pre-login pointer and keyboard control on top of
the status-only broker, gated behind the signed Virtual HID driver.

The Boot Service broadcasts the stable host identity on UDP 45820 and accepts
trusted status sessions on TCP 45822. The desktop host continues to use TCP
45821 after login. The Android client recognizes the endpoint transition and
reconnects automatically.

Trusted-device tokens are mirrored from the signed-in user's local CouchLink
store to `%ProgramData%\CouchLink\trusted-devices.json`. The service only
authenticates devices already paired from the desktop host. Pairing and
credential storage remain unavailable before login.

## How pre-login input now works

`CouchLink.BootService` runs as LocalSystem in Session 0, and User32 input
(`SendInput`, `mouse_event`, `SetCursorPos`) still cannot reach the Winlogon
secure desktop from there — that OS boundary is unchanged. Instead, the Boot
Service opens the **CouchLink Virtual HID driver** and submits small fixed-size
HID reports. Because those reports enter through the real HID stack, Windows
treats them as genuine mouse and keyboard input and they are honored on the
desktop, the lock screen, and the sign-in screen.

Input is enabled only when **both** conditions hold:

1. `%ProgramData%\CouchLink\machine-permissions.json` has `RemoteInputEnabled`
   **and** `PreLoginControlEnabled` set true (mirrored from the signed-in user's
   authorization), and
2. the Virtual HID driver is installed and running.

When either is missing, the broker advertises `remoteInputEnabled: false`, the
Android client stays on the status panel, and any input message is answered with
`prelogin_input_unavailable`. This is a strict superset of the old status-only
behavior, so nothing regresses when the driver is absent.

Advertised pre-login capabilities become
`prelogin_status, machine_state, wake_on_lan, prelogin_input, mouse, keyboard`
once input is ready.

## What is still deliberately out of scope

This is **not** a Windows Credential Provider. It does not store or auto-fill
passwords; it lets you drive the real pointer and keyboard so you can type your
own PIN/password on the secure desktop. A future audited Credential Provider
milestone can add true credential submission if wanted.

## Hard boundary (unchanged)

Session 0 User32 injection to the secure desktop remains impossible. The Virtual
HID driver is the supported path around it, and it must be signed. Until a
signed build exists (test-signed in a VM for development, production/attestation
signed for the real host), keep pre-login input confined to the test VM. See
`src/windows/CouchLink.VirtualHid/BUILD-SIGN-TEST.md`.
