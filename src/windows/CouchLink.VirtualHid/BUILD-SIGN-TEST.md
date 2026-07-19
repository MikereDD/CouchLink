# Building, signing, and testing the CouchLink Virtual HID driver

This is the part that **cannot** be shipped pre-built, because a kernel driver
that controls the secure desktop must be compiled against the WDK and signed.
Do every step below inside a **disposable Windows VM** with a snapshot taken
first. Do not load an unsigned/experimental kernel driver on your real host.

---

## 0. Why a VM

The driver runs in kernel mode. A mistake can bluescreen the machine, and
enabling test-signing lowers the machine's security posture. A VM with a clean
snapshot means a bad build costs you a rollback, not your PC. `docs/VIRTUAL-HID-FOUNDATION.md`
made this a hard gate; this document is how you satisfy it.

## 1. One-time tooling (inside the VM)

1. Install **Visual Studio 2022** with the *Desktop development with C++* workload.
2. Install the **Windows SDK** and the matching **Windows Driver Kit (WDK)**.
   The WDK version must match the SDK version. The WDK provides `vhf.h` and
   `vhfkm.lib`, which this driver depends on.
3. Confirm the *Spectre-mitigated libraries* for your toolset are installed
   (the driver template requires them).

## 2. Build

1. Open `src/windows/CouchLink.sln` in Visual Studio **on the VM**.
   The `CouchLink.VirtualHid` project appears as a Kernel Mode Driver (KMDF)
   target. It is intentionally **not** referenced by the .NET solution and does
   not build with `dotnet build`; build it from Visual Studio.
2. Select `Debug | x64` (or `ARM64` to match your VM).
3. Build the `CouchLink.VirtualHid` project. Output:
   - `CouchLinkVhid.sys`
   - `CouchLinkVhid.inf`
   - `couchlinkvhid.cat` (catalog, produced by the build)

## 3. Enable test signing (VM only)

From an elevated prompt in the VM, then reboot:

```
bcdedit /set testsigning on
shutdown /r /t 0
```

You'll see a "Test Mode" watermark. That's expected and only in the VM.

## 4. Create a test certificate and sign

```
:: create a one-off test cert (first time only)
makecert -r -pe -ss PrivateCertStore -n "CN=CouchLink Test" CouchLinkTest.cer

:: sign the driver and catalog
signtool sign /v /s PrivateCertStore /n "CouchLink Test" /fd sha256 CouchLinkVhid.sys
signtool sign /v /s PrivateCertStore /n "CouchLink Test" /fd sha256 couchlinkvhid.cat

:: trust the test cert on the VM
certutil -addstore -f root CouchLinkTest.cer
certutil -addstore -f trustedpublisher CouchLinkTest.cer
```

(`makecert` is legacy; `New-SelfSignedCertificate` in PowerShell works too and
is preferred on newer WDKs. Either produces a cert you trust only inside the VM.)

## 5. Install the virtual device

The INF describes a **root-enumerated** software device, so you create the
device node yourself and point it at the driver:

```
pnputil /add-driver CouchLinkVhid.inf /install
devgen /add /instanceid CL01 /hardwareid "Root\CouchLinkVhid"
```

`devgen` ships with recent WDKs. On older kits use `devcon install CouchLinkVhid.inf "Root\CouchLinkVhid"`.

Verify in Device Manager: under *Human Interface Devices* you should now see
**CouchLink Virtual HID**, plus a HID-compliant mouse and keyboard beneath it.

## 6. Validate — the milestone checklist

Do these in order and stop if any step misbehaves.

1. **Desktop pointer.** Sign in normally. Enable *Remote Input* and
   *Pre-Login Control* so `machine-permissions.json` has both flags true.
   Start `CouchLink.BootService`. Connect the Android app and move the touchpad.
   The cursor should move on the desktop.
2. **Desktop buttons + scroll + typing.** Left/right click, scroll, and type a
   few characters into Notepad.
3. **Lock screen pointer.** Press Win+L. The touchpad should now move the cursor
   on the lock screen — the thing `SendInput` from Session 0 could never do.
4. **Login.** Sign out fully so Windows shows `SignInRequired`. The Android app
   switches to the pre-login panel; the touchpad and PIN keyboard should let you
   focus the field and enter your PIN/password and sign in.
5. **Cleanup on disconnect.** Kill the app mid-drag; confirm no button or key
   stays stuck (the host calls `ReleaseAll` on disconnect).

## 7. Removing it

```
devgen /remove /instanceid CL01 /hardwareid "Root\CouchLinkVhid"   :: or devcon remove
pnputil /delete-driver CouchLinkVhid.inf /uninstall
bcdedit /set testsigning off
```

Then restore the VM snapshot if you want a perfectly clean state.

---

## Going to your real living-room PC

Test-signing is for the VM. To run on Netzach / the real Steam PC without test
mode you need a **production signature**: an EV code-signing certificate and
Microsoft **attestation signing** (or full WHQL) through the Partner Center
hardware dashboard. That submission signs the same `.sys` you validated here.
Until then, keep it in the VM.

## Security notes baked into the driver

- The driver accepts exactly one IOCTL and exactly one fixed-size struct
  (`COUCHLINK_HID_SUBMIT`, 12 bytes). Wrong size → rejected.
- It validates protocol version, report kind, and payload length before it
  touches the HID stack. It never takes pointers, paths, or variable-length data.
- The user-mode bridge only injects when **both** the machine authorization flag
  is set **and** the driver is present. Absent either, the Boot Service behaves
  exactly like the status-only baseline.
