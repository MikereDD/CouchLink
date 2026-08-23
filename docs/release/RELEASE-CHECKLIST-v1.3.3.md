# CouchLink v1.3.3 Stable Release Checklist

## Source and versioning

- [ ] Build from `release/1.3.3` with a clean working tree during stable preparation.
- [ ] Android `versionName` is `1.3.3`.
- [ ] Android `versionCode` is `162`.
- [ ] Windows `HostConstants.HostVersion` is `1.3.3`.
- [ ] Windows project `Version`, `VersionPrefix`, `AssemblyVersion`, `FileVersion`, and `InformationalVersion` match `1.3.3`.
- [ ] `SOURCE-MANIFEST-v1.3.2.sha256` has been removed from the current source root.
- [ ] `SOURCE-MANIFEST-v1.3.3.sha256` has been regenerated and verifies cleanly.

## Automated validation

- [ ] Windows solution builds in Release configuration.
- [ ] Version-comparison and network-selection/lifecycle tests pass.
- [ ] Windows release-security/updater safety tests pass.
- [ ] Windows Host publishes successfully for `win-x64`.
- [ ] Windows Updater publishes successfully for `win-x64`.
- [ ] Android signed release APK builds successfully.
- [ ] Android signing certificate matches the canonical CouchLink certificate.
- [ ] Windows Host and Updater detached signatures verify against the pinned release key.
- [ ] Tampered-payload/signature tests fail closed as expected.
- [ ] Source manifest verifies cleanly.

## Launcher Tray acceptance

- [x] Launcher Tray appears on Android Home with CouchLink-native presentation.
- [x] Tray exposes Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar Games Launcher, Epic Games Launcher, and Amazon Games.
- [x] Launcher actions require the connected Windows Launcher Host.
- [x] Bluetooth HID remains available for keyboard/mouse/shortcut input but is not used as a launcher fallback.

## Host network persistence acceptance

- [x] An explicitly selected wired interface survives a Host restart.
- [x] An explicitly selected Wi-Fi interface survives a Host restart.
- [x] Mobile Launcher Host reconnect works after reopening the Host without pressing **Use Interface** again.
- [x] If the saved/active interface is unavailable, discovery can fall back to another eligible physical LAN interface.
- [x] Wake-on-LAN remains available for a trusted wired Host and wakes the PC.
- [x] Mobile Launcher Host reconnects automatically after the PC wakes.

## Stable upgrade proof

- [ ] Install/update Windows Host `1.3.2 → 1.3.3` through the Stable-channel updater.
- [ ] Install/upgrade signed Android `1.3.2` (`versionCode 161`) → `1.3.3` (`versionCode 162`).
- [ ] Pairing/trusted-host state, settings, selected Host interface, and Stable/Test channel selection survive the upgrade.
- [ ] Launcher Tray, launcher actions, Audio Output, Wake PC, and discovery reconnect normally after update.
- [ ] Host/Updater/APK/source ZIP SHA-256 files match published artifacts.
- [ ] Detached Host and Updater signatures verify against the pinned release key.

## Stable packaging and publication

- [ ] Host EXE, Updater EXE, APK, source ZIP, detached signatures, checksum files, combined `SHA256SUMS`, and release metadata are produced.
- [ ] Source ZIP contains no private keys, keystores, passwords, build outputs, or machine-specific files.
- [ ] Release notes/checklist/validation documents match `1.3.3`.
- [ ] Merge the exact validated stable source into `main`.
- [ ] Tag the exact stable release commit as `v1.3.3`.
- [ ] Push `main` and tag to Forgejo and GitHub.
- [ ] Publish the GitHub release as a normal stable release, **not** a Pre-release.
- [ ] Confirm GitHub marks `v1.3.3` as the latest stable release.

## Deferred hardening

The existing post-1.3.1 hardening backlog remains open and is not represented as fixed by this release:

- machine-wide plaintext trusted-token mirror;
- pairing-code brute-force throttling;
- Android/Windows prerelease-rank alignment;
- future encrypted Host transport/TLS work.
