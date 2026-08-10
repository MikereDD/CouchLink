# CouchLink v1.3.1-rc.1 Release Checklist

## Source and versioning

- [ ] Build from `release/1.3.1` with a clean working tree.
- [ ] Android `versionName` is `1.3.1-rc.1`.
- [ ] Android `versionCode` is `159`.
- [ ] Windows `HostConstants.HostVersion` is `1.3.1-rc.1`.
- [ ] Windows project `Version` and `InformationalVersion` are `1.3.1-rc.1`.
- [ ] `SOURCE-MANIFEST-v1.3.1-dev.8.1.sha256` has been removed.
- [ ] `SOURCE-MANIFEST-v1.3.1-rc.1.sha256` has been regenerated and verifies cleanly.

## Automated validation

- [ ] Windows version-comparison tests pass.
- [ ] `CouchLink.Updater.Tests` passes all updater safety tests.
- [ ] Windows Host builds/publishes successfully.
- [ ] Windows Updater builds/publishes successfully.
- [ ] Android release APK builds successfully.
- [ ] Android signing certificate matches the canonical CouchLink certificate.
- [ ] Windows detached-signature valid/tamper tests pass for Host and Updater.

## Update regression

- [ ] A signed `1.3.1-dev.8.1` Windows Host detects `1.3.1-rc.1` on the Test channel.
- [ ] Host payload SHA-256 verifies.
- [ ] Updater payload SHA-256 verifies.
- [ ] Detached Host and Updater signatures verify against the pinned release key.
- [ ] Replacement completes and the RC Host restarts successfully.
- [ ] Missing/blank updater `--signature` is rejected by the focused safety tests.
- [ ] Double rollback is non-destructive in the focused safety tests.

## Android / living-room regression

- [ ] Install/upgrade the signed RC APK over the previous signed Test build.
- [ ] Pair/connect to the Windows Host and confirm launcher controls.
- [ ] Confirm touchpad, keyboard, command deck, and Bluetooth HID remain functional.
- [ ] Confirm Audio Output loads, changes endpoints, and resynchronizes after reconnect.
- [ ] Complete at least three settled S3 **Wake PC** cycles with automatic Host reconnect.
- [ ] Confirm **Forget Host** removes trust and the saved Wake PC identity.
- [ ] Confirm HDMI 1/2/3 show neutral **HDMI input** labels with no default highlight.
- [ ] Confirm HDMI mappings HW4/HW5/HW6 still switch the expected inputs.

## Release packaging

- [ ] Host EXE, Updater EXE, APK, source ZIP, detached signatures, checksum files, and combined `SHA256SUMS` are produced.
- [ ] Source ZIP contains no private keys, keystores, passwords, build outputs, or machine-specific files.
- [ ] Release notes/checklist/validation documents match `1.3.1-rc.1`.
- [ ] Tag the exact validated release commit as `v1.3.1-rc.1`.
- [ ] Push the release commit and tag to Forgejo and GitHub.
- [ ] Publish the GitHub release as **Pre-release**.
