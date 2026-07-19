# CouchLink v0.1-dev.12.1 Test Checklist

## Source/build gates

- [ ] Android `assembleDebug` reports `0.1-dev.12.1` / versionCode `28`.
- [ ] .NET projects build successfully.
- [ ] `CouchLink.VirtualHid` builds as `Debug | x64` with the matching WDK.
- [ ] `infverif` accepts `CouchLinkVhid.inf`.
- [ ] Driver package is test-signed and installed only in a disposable VM snapshot.
- [ ] A standard non-admin process cannot open the CouchLink VHID interface; LocalSystem/admin can.

## Functional VM test

- [ ] Driver appears as CouchLink Virtual HID plus HID mouse and keyboard children.
- [ ] Desktop touchpad movement, tap/left click, right click, drag, scroll, and typing work.
- [ ] Rapid movement plus typing preserves report order and does not disconnect the bridge.
- [ ] Win+L: touchpad movement and both click buttons work on the lock screen.
- [ ] On-Screen Keyboard can be opened and operated.
- [ ] Keyboard reports can focus and type into the sign-in field.
- [ ] Disconnect during drag releases mouse buttons and keyboard state.
- [ ] Driver Verifier produces no bugcheck or violation during the test pass.

## Stop conditions

Stop and roll back the VM snapshot on any bugcheck, stuck key/button, malformed HID device, access-control failure, or repeated driver restart.
