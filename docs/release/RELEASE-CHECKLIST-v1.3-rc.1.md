# CouchLink v1.3-rc.1 Release Checklist

## Version and source

- [ ] Android reports `1.3-rc.1` with `versionCode 140`.
- [ ] Windows Host reports `1.3-rc.1`.
- [ ] Source manifest verification passes.
- [ ] README screenshot gallery renders correctly on the local Git host and GitHub preview.
- [ ] No private signing files, local paths, tokens, or personal identifiers are present.

## Windows Host

- [ ] `dotnet build .\src\windows\CouchLink.sln -c Release` succeeds.
- [ ] Main Host window opens, resizes, maximizes, restores, and scrolls correctly.
- [ ] Pairing, trusted reconnect, revoke-selected, and revoke-all work.
- [ ] Start-with-Windows and start-minimized settings persist without overwriting favorites.
- [ ] Host settings save atomically and corrupt settings are preserved.
- [ ] Audio Output popup opens, closes, resizes, and keeps complete rows accessible.
- [ ] All three Windows audio roles switch together.
- [ ] External Windows output changes synchronize to the popup.
- [ ] Added, removed, renamed, enabled, and disabled endpoints refresh correctly.
- [ ] Slow or malformed endpoints do not blank the complete device list.

## Android

- [ ] Debug and signed release APK builds succeed.
- [ ] Existing installation upgrades without losing settings.
- [ ] Bluetooth HID reconnect, Touchpad, Keyboard, and shortcuts work.
- [ ] All launcher tiles launch or focus their expected applications.
- [ ] Android Audio Output favorites persist per host.
- [ ] Android output state synchronizes after Windows-side changes.
- [ ] Google TV pairing, navigation, playback, power, volume, channel, Live, and input controls pass.

## Packaging and signing

- [ ] `Build-Release.ps1` completes on Windows.
- [ ] Android signing certificate matches the pinned expected certificate.
- [ ] Signed APK installs and launches.
- [ ] Windows release package launches on a clean test location.
- [ ] SHA-256 files are generated and verified for APK, Windows package, and source ZIP.
- [ ] Final release notes and artifact names consistently use `v1.3-rc.1`.
