# CouchLink 1.0 release checklist

## Build integrity

- [ ] Build from a clean checkout/archive.
- [ ] Use JDK 17 and the committed Gradle wrapper.
- [ ] Sign with the permanent CouchLink release keystore.
- [ ] Verify the APK with `apksigner verify --verbose --print-certs`.
- [ ] Generate and publish the SHA-256 checksum.
- [ ] Install the release APK, not `app-debug.apk` or `app-debug-androidTest.apk`.

## Upgrade and install

- [ ] Confirm clean installation on a supported Android device.
- [ ] Confirm upgrade over the final dev build after uninstalling any `.debug` application ID build.
- [ ] Confirm app label and launcher icon display correctly.
- [ ] Confirm Android reports version `1.0`.

## Bluetooth HID

- [ ] Pair with Windows and reconnect after phone app restart.
- [ ] Reconnect after Windows reboot.
- [ ] Verify keyboard and touchpad with Wi-Fi disabled.
- [ ] Verify input at the Windows sign-in screen with Secure Boot enabled.
- [ ] Verify background operation and foreground-service notification.
- [ ] Verify explicit disconnect does not show a false reconnecting state.

## Controls and launchers

- [ ] Test normal typing, symbols, backspace, Enter, and Windows shortcuts.
- [ ] Test scrolling in both directions and drag lock release.
- [ ] Test Steam, GOG, Xbox, EA, Ubisoft, Rockstar, Epic, and Amazon launchers.
- [ ] Confirm launcher icons are white on dark badges and outlines use CouchLink orange.

## Release archive

- [ ] Preserve this exact source package.
- [ ] Preserve the signed APK and checksum.
- [ ] Preserve release notes, test results, signing certificate fingerprint, and build environment details.
- [ ] Tag the repository `v1.0` only after all checks pass.
