# CouchLink v1.3.1 Validation Record

## Stable-candidate status

Stable preparation follows the published `v1.3.1-rc.1` release candidate.

The RC source was tagged at commit `51a551f` as `v1.3.1-rc.1`. Before that tag:

- the source manifest verified with 401 files;
- the Windows release-signing key validated and matched the pinned public key;
- the Windows Release solution built successfully;
- the focused updater safety tests passed all seven rollback/signature cases;
- the signed Android and Windows RC applications built successfully.

The published RC was then installed on the Android device and Uriel. Normal use after installation showed no reported regression, and the RC was accepted for stable promotion.

## Pre-RC security gate carried into stable

The two pre-RC audit findings required for promotion remain fixed:

1. **Updater rollback double-restore:** `RestoreBackup` exits without touching the installed target when no backup exists.
2. **Mandatory updater authenticity:** the external updater requires `--signature` and verifies the detached signature unconditionally.

## Stable automated validation

- [ ] Windows Release solution build
- [ ] Windows version-comparison tests
- [ ] Updater safety tests
- [ ] Windows Host publish
- [ ] Windows Updater publish
- [ ] Android signed APK
- [ ] Android certificate verification
- [ ] Windows detached-signature valid/tamper tests
- [ ] Source manifest verification

## Stable live validation

- [ ] Signed Windows update from `1.3.1-rc.1` to `1.3.1`
- [ ] Signed Android upgrade from `versionCode 159` to `versionCode 160`
- [ ] Pairing/authenticated Host reconnect and settings preservation
- [ ] Launcher controls
- [ ] Touchpad / keyboard / command deck / Bluetooth HID
- [ ] Audio Output
- [ ] Wake PC and automatic reconnect
- [ ] Forget Host clears wake identity and trust
- [ ] TV input selector labels and HW4/HW5/HW6 switching

## Deferred post-1.3.1 hardening

Not stable blockers unless new evidence changes their risk assessment:

- Remove the unused machine-wide plaintext trusted-device mirror or protect it before any future need to reintroduce machine-wide trust state.
- Add pairing-code brute-force throttling.
- Align prerelease rank handling across Android and Windows if alpha/beta channels are ever used.
- Treat encrypted Host transport/TLS as a later architectural improvement for LAN token confidentiality.

## Final status

Pending stable build, signing, publication, and RC-to-stable in-place update proof.
