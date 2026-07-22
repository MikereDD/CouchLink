# CouchLink Virtual HID

> **Kernel-mode component. Build, sign, install, and test deliberately.**

CouchLink Virtual HID is a KMDF driver built on Microsoft Virtual HID Framework (VHF). It exposes a relative mouse and boot-compatible keyboard so trusted CouchLink input can reach the interactive desktop, lock screen, and Windows sign-in screen through the real HID stack.

## Why it exists

A Windows service running as LocalSystem in Session 0 cannot use User32 input APIs to control the Winlogon secure desktop. CouchLink therefore submits validated, fixed-size HID reports to a dedicated virtual device. Windows processes those reports like physical keyboard and mouse input while continuing to enforce normal authentication.

## Source map

| File | Purpose |
|---|---|
| `CouchLinkHidProtocol.h` | Shared user/kernel contract, report IDs, IOCTL, and device-interface GUID |
| `Driver.h` / `Driver.c` | KMDF + VHF implementation and report validation |
| `CouchLinkVhid.inf` | Root-enumerated software HID installation metadata |
| `CouchLink.VirtualHid.vcxproj` | Visual Studio / WDK driver project |
| `BUILD-SIGN-TEST.md` | Required build, signing, installation, and validation procedure |

## Safety model

- The repository does not ship a production-signed driver package.
- Every submission is checked for protocol version, report kind, reserved fields, and payload length.
- The Boot Service opens the device only when pre-login control is enabled.
- CouchLink never stores or validates the user's Windows PIN or password.
- Initial driver changes should be validated in a disposable VM before real-hardware installation.

## Build

Do **not** use `dotnet build` for this project. Install Visual Studio with the Desktop C++ workload and Windows Driver Kit, then follow [BUILD-SIGN-TEST.md](BUILD-SIGN-TEST.md) exactly.

See also [Virtual HID Foundation](../../../docs/VIRTUAL-HID-FOUNDATION.md) and [Pre-Login Connectivity](../../../docs/PRELOGIN-CONNECTIVITY.md).
