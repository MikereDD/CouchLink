# CouchLink Virtual HID Research Foundation

## Why this milestone exists

Windows services run in Session 0. User32 input injection from the Boot Service is not a supported path to the Winlogon secure desktop. CouchLink therefore treats all Session 0 `SendInput`, `mouse_event`, and `SetCursorPos` experiments as retired.

## Target architecture

Android Remote → Boot Service → authenticated local device channel → signed KMDF Virtual HID source driver → Windows HID stack.

The driver must expose mouse and keyboard collections through Microsoft Virtual HID Framework (VHF). It must accept only small fixed-size reports from the CouchLink Boot Service and must never accept arbitrary kernel memory, raw pointers, file paths, or executable payloads.

## Safety gate

This archive does **not** install a kernel driver. It provides the source boundary, protocol draft, prerequisite checks, and VM-first test plan. Do not register an unsigned experimental driver on the primary CouchLink host.

## Required tooling

- Visual Studio with Desktop C++ and Windows Driver Kit
- KMDF/VHF headers and libraries
- Test-signing in an isolated Windows VM
- Snapshot/recovery path before driver installation

## Next implementation steps

1. Build a minimal VHF mouse-only driver in a VM.
2. Add a restricted IOCTL carrying a versioned mouse report.
3. Add a Boot Service bridge with explicit device ACLs.
4. Validate pointer movement on desktop and lock screen.
5. Add buttons and wheel.
6. Add keyboard only after mouse behavior is stable and audited.
