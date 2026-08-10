# CouchLink v1.3.1 Stable Release Checklist

## Source and versioning

- [ ] Build from `release/1.3.1` with a clean working tree during stable preparation.
- [ ] Android `versionName` is `1.3.1`.
- [ ] Android `versionCode` is `160`.
- [ ] Windows `HostConstants.HostVersion` is `1.3.1`.
- [ ] Windows project `Version` and `InformationalVersion` are `1.3.1`.
- [ ] `SOURCE-MANIFEST-v1.3.1-rc.1.sha256` has been removed.
- [ ] `SOURCE-MANIFEST-v1.3.1.sha256` has been regenerated and verifies cleanly.

## Automated validation

- [ ] Windows solution builds in Release configuration.
- [ ] Windows version-comparison tests pass.
- [ ] `CouchLink.Updater.Tests` passes all updater safety tests.
- [ ] Windows Host publishes successfully for `win-x64`.
- [ ] Windows Updater publishes successfully for `win-x64`.
- [ ] Android signed release APK builds successfully.
- [ ] Android signing certificate matches the canonical CouchLink certificate.
- [ ] Windows detached-signature valid/tamper tests pass for Host and Updater.
- [ ] Source manifest verifies cleanly.

## RC-to-stable regression

- [ ] Install/upgrade the signed Android `1.3.1` APK over `1.3.1-rc.1` and confirm `versionCode 160`.
- [ ] Install/update the Windows Host from `1.3.1-rc.1` to `1.3.1` using the published Stable-channel release.
- [ ] Host and Updater SHA-256 checks pass.
- [ ] Detached Host and Updater signatures verify against the pinned release key.
- [ ] Windows replacement completes and the stable Host restarts successfully.
- [ ] Android package name and signing certificate remain valid.
- [ ] Pairing, trusted-host state, settings, and update-channel selection survive the upgrade.

## Living-room acceptance

- [ ] Pair/connect to the Windows Host and confirm launcher controls.
- [ ] Confirm touchpad, keyboard, command deck, and Bluetooth HID remain functional.
- [ ] Confirm Audio Output loads, changes endpoints, and resynchronizes after reconnect.
- [ ] Confirm Wake PC still wakes the trusted S3 Host and reconnects automatically.
- [ ] Confirm Forget Host removes trust and the saved Wake PC identity.
- [ ] Confirm HDMI 1/2/3 show neutral **HDMI input** labels with no default highlight.
- [ ] Confirm HDMI mappings HW4/HW5/HW6 still switch the expected inputs.

## Stable packaging and publication

- [ ] Host EXE, Updater EXE, APK, source ZIP, detached signatures, checksum files, and combined `SHA256SUMS` are produced.
- [ ] Source ZIP contains no private keys, keystores, passwords, build outputs, or machine-specific files.
- [ ] Release notes/checklist/validation documents match `1.3.1`.
- [ ] Merge the exact validated stable source into `main`.
- [ ] Tag the exact stable release commit as `v1.3.1`.
- [ ] Push `main` and tag to Forgejo and GitHub.
- [ ] Publish the GitHub release as a normal stable release, **not** a Pre-release.
- [ ] Upload only the approved 13 public release assets.
- [ ] Confirm GitHub marks `v1.3.1` as the latest stable release.
