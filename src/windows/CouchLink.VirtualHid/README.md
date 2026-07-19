# CouchLink.VirtualHid

Signed KMDF **Virtual HID source driver** for CouchLink. It exposes one virtual
device with a relative mouse and a boot-compatible keyboard, and injects input
that reaches the interactive **and secure (login/lock) desktop** through the real
HID stack — the thing User32 `SendInput` from Session 0 can never do.

## Files

- `CouchLinkHidProtocol.h` — the entire user↔kernel contract: one IOCTL, one
  fixed 12-byte struct, report IDs, the device-interface GUID. Shared verbatim
  with the managed bridge.
- `Driver.h` / `Driver.c` — the KMDF + Microsoft VHF driver. HID report
  descriptor (mouse report ID 1, keyboard report ID 2), VHF create/start, and a
  single validated IOCTL that forwards fixed-size reports via `VhfReadReportSubmit`.
- `CouchLinkVhid.inf` — root-enumerated install for a software-only HID device.
- `CouchLink.VirtualHid.vcxproj` — WDK/KMDF project (built from Visual Studio on
  the VM, not by `dotnet build`).
- `BUILD-SIGN-TEST.md` — **read this.** Build, test-sign, install, and validate,
  strictly inside a disposable VM first.

## Safety

This archive still ships **no** compiled `.sys` or `.cat`. A kernel driver that
controls the secure desktop must be built against the WDK and signed; that step
is yours, in a VM, per `BUILD-SIGN-TEST.md`. Do not load an unsigned build on
your real host.

See `docs/VIRTUAL-HID-FOUNDATION.md`.
