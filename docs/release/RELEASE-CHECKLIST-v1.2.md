# CouchLink 1.2 Release Checklist

## Source and metadata

- [x] Android metadata is `versionName 1.2` and `versionCode 120`.
- [x] Stable package ID remains `dev.typezero.couchlink.remote`.
- [x] Installed label is `CouchLink`.
- [x] Metallic CL launcher icon is preserved.
- [x] README includes the approved four-screen feature collage.
- [x] Changelog and release notes are current.
- [x] Source manifest has been regenerated and verified.

## Signed Android build

- [ ] Run `Build-Release.ps1 -Version 1.2` with the official keystore.
- [ ] Verify the expected signing-certificate SHA-256 fingerprint.
- [ ] Confirm the APK installs as the stable package.
- [ ] Confirm the launcher label is `CouchLink`.
- [ ] Confirm no development-only text appears in the installed app.

## Functional smoke test

- [ ] Bluetooth HID connects and sends keyboard/mouse input.
- [ ] Home, Touchpad, Keyboard, TV Remote, and Settings open normally.
- [ ] Windows Launcher Host reconnects and launcher actions work.
- [ ] Secure Google TV connection restores.
- [ ] Power off and Wake-on-LAN power on work.
- [ ] Home, Back, Settings, D-pad, and OK work.
- [ ] Volume, mute, channel, playback, and one-minute skips work.
- [ ] Google Live opens the Google TV Live tab.
- [ ] HDMI 1, HDMI 2, HDMI 3, Composite, and TV/Antenna work on the validated Hisense TV.

## Publication

- [ ] Merge `tv-remote-control` into `main`.
- [ ] Tag `v1.2`.
- [ ] Publish signed APK, Windows host package, source archive, checksums, and release notes.
- [ ] Delete the development branch only after the remote tag and release assets are confirmed.
