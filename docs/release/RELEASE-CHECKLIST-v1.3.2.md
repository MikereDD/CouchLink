# CouchLink v1.3.2 Stable Release Checklist

## Source and versioning

- [ ] Build from `release/1.3.2` with a clean working tree during stable preparation.
- [ ] Android `versionName` is `1.3.2`.
- [ ] Android `versionCode` is `161`.
- [ ] Windows `HostConstants.HostVersion` is `1.3.2`.
- [ ] Windows project `Version`, `VersionPrefix`, `AssemblyVersion`, `FileVersion`, and `InformationalVersion` match `1.3.2`.
- [ ] `SOURCE-MANIFEST-v1.3.1.sha256` has been removed from the current source root.
- [ ] `SOURCE-MANIFEST-v1.3.2.sha256` has been regenerated and verifies cleanly.

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

## Networking acceptance carried from PR review

- [x] Automatic selection prefers an eligible physical Ethernet/Wi-Fi interface.
- [x] Startup with no eligible interface waits safely and can select the first eligible interface when it appears.
- [x] Selected-interface loss stops advertisement and requires explicit manual reselection.
- [x] Candidate-list-only changes publish an updated network-selection snapshot.
- [x] Stopped runtime does not resume advertisement during refresh.
- [x] Discovery advertisement is bound to the selected interface/broadcast target.
- [x] TCP remains on `IPAddress.Any:45821`.
- [x] Android discovery acquires/releases its Wi-Fi multicast lock with the discovery lifecycle.
- [x] Existing launcher/pairing protocol remains version `1`.

## Final UI acceptance

- [x] Windows Host Release build succeeds with the CouchLink-themed network selector.
- [x] Selected network adapter renders by friendly display name rather than object `ToString()`.
- [x] Dropdown and **USE INTERFACE** action match the existing CouchLink black/orange design language.

## Stable upgrade proof

- [ ] Install/update Windows Host `1.3.1 → 1.3.2` through the Stable-channel updater.
- [ ] Install/upgrade signed Android `1.3.1` (`versionCode 160`) → `1.3.2` (`versionCode 161`).
- [ ] Pairing/trusted-host state, settings, and Stable/Test channel selection survive the upgrade.
- [ ] Launcher controls and discovery reconnect normally after update.
- [ ] Host/Updater/APK/source ZIP SHA-256 files match published artifacts.
- [ ] Detached Host and Updater signatures verify against the pinned release key.

## Stable packaging and publication

- [ ] Host EXE, Updater EXE, APK, source ZIP, detached signatures, checksum files, combined `SHA256SUMS`, and release metadata are produced.
- [ ] Source ZIP contains no private keys, keystores, passwords, build outputs, or machine-specific files.
- [ ] Release notes/checklist/validation documents match `1.3.2`.
- [ ] Merge the exact validated stable source into `main`.
- [ ] Tag the exact stable release commit as `v1.3.2`.
- [ ] Push `main` and tag to Forgejo and GitHub.
- [ ] Publish the GitHub release as a normal stable release, **not** a Pre-release.
- [ ] Confirm GitHub marks `v1.3.2` as the latest stable release.

## Deferred hardening

The existing post-1.3.1 hardening backlog remains open and is not represented as fixed by this release:

- machine-wide plaintext trusted-token mirror;
- pairing-code brute-force throttling;
- Android/Windows prerelease-rank alignment;
- future encrypted Host transport/TLS work.
