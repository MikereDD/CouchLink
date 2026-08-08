# CouchLink v1.3.1-rc.1 Validation Record

## Release status

Release-candidate validation in progress.

## Pre-RC audit gate

The pre-RC source audit identified two items required before rc.1. Both have been corrected and locally validated before this release branch was prepared:

1. **HIGH — updater rollback double-restore:** `RestoreBackup` now returns without touching the installed target when no backup exists, so a second recovery call cannot delete an already-restored Host.
2. **MEDIUM — updater signature optionality:** `--signature` is now required and detached-signature verification is unconditional.

Focused updater tests cover restore/consume behavior, repeated restore safety, no-backup no-op behavior, missing/blank signature rejection, and valid signature-argument acceptance.

## Automated build validation

- [ ] Windows version-comparison tests
- [ ] Updater safety tests
- [ ] Windows Host publish
- [ ] Windows Updater publish
- [ ] Android signed APK
- [ ] Android certificate verification
- [ ] Windows detached-signature valid/tamper tests
- [ ] Source manifest verification

## Live regression validation

- [ ] Signed Windows update from `1.3.1-dev.8.1` to `1.3.1-rc.1`
- [ ] Signed Android upgrade to `versionCode 159`
- [ ] Pairing/authenticated Host reconnect
- [ ] Launcher controls
- [ ] Touchpad / keyboard / command deck / Bluetooth HID
- [ ] Audio Output
- [ ] Wake PC: three settled S3 sleep/wake/reconnect cycles
- [ ] Forget Host clears wake identity and trust
- [ ] TV input selector labels and HW4/HW5/HW6 switching

## Deferred audit findings

Not rc.1 blockers:

- Machine-wide plaintext trusted-device mirror: candidate for the pre-stable hardening pass.
- Pairing-code brute-force throttling: candidate for the pre-stable hardening pass.
- Cross-platform prerelease-rank parity: low priority; current `dev -> rc -> release` path is correct.
- Plaintext LAN trust token transport: known home-LAN tradeoff requiring a later architectural transport change.

## Final status

Pending RC build, signed-update proof, and live regression validation.
