# CouchLink v1.4.0 — Validation Record

Status: Release preparation; final validation pending.

## Candidate scope

CouchLink v1.4.0 introduces the Google TV Streaming Apps launcher,
persistent custom shortcuts, private custom artwork, and corrected
D-pad geometry.

The Windows Host and Updater remain part of the unified release.
Shared launcher protocol version 1 is unchanged.

## Previously completed development validation

- Streaming Apps and custom-icon workflows exercised on physical hardware.
- Netflix, Apple TV, Hulu, and custom launch actions exercised.
- Custom icon import, replacement, reset, and persistence exercised.
- 56/56 host regression tests passed.
- 8/8 focused D-pad geometry tests passed.
- 4/4 publication-policy checks passed.
- Android debug and instrumentation APK builds completed.

These results apply to earlier development builds, not final
signed v1.4.0 release artifacts.

## November 13, 2026 — Release candidate validation

These checks apply to local release candidates, not a final
committed and tagged source build.

- Android signed release build: PASS.
- Android debug unit tests and debug assembly: PASS.
- Android APK Signature Scheme v3: verified.
- Android signing certificate: established release identity preserved.
- Android package: dev.typezero.couchlink.remote.
- Android versionName: 1.4.0; versionCode: 163.
- Android APK SHA-256:
  5320F93FCDC883C97FA58FBD2D60B1324890032AE918EA22982ED8519503F4BE
- Android temporary signing configuration: removed.
- Windows Host candidate: Release publish succeeded, version 1.4.0.
- Windows Updater initial candidate had incorrect version 1.0.0.
- Corrected Windows Updater candidate: Release build PASS.
  ProductVersion 1.4.0; FileVersion 1.4.0.0.
- Corrected Updater ECDSA signature: PASS.
  Valid signature accepted; modified executable, modified signature,
  and malformed signature all rejected.
- Final tagged-source rebuild and signing remain pending.
- Windows version-comparison tests: 39/39 PASS; additional
  lifecycle and protocol checks passed.
- Windows updater safety tests: 7/7 PASS.
- Windows Host and Updater candidate detached signatures: PASS.
  Valid signatures accepted; modified payloads, modified signatures,
  and malformed signatures rejected for both executables.
- Windows signing private key matched the pinned ECDSA P-256
  public-key fingerprint.
- Source manifest: 444 files verified; no .git pointer entries.

The final signed and tagged-source build, physical device tests,
in-place upgrade tests, complete packaging, and owner approval
remain outstanding.
## Outstanding hardware validation

- [ ] Corrected D-pad visual and touch testing.
- [ ] Keyboard-visible dialog-control reachability.
- [ ] Large-text layout review.
- [ ] Confirm icon-editor reopening behavior.

## Android release validation

- [x] v1.4.0 release build passes.
- [x] Unit and regression tests pass.
- [x] Signing certificate matches the established release identity.
- [x] APK package identity and versionCode 163 verified.
- [ ] In-place upgrade from v1.3.3 verified.
- [ ] User settings and shortcut data survive upgrade.

## Windows release validation

- [x] Windows Host Release build passes.
- [x] Windows Updater build passes.
- [x] Version-comparison tests pass.
- [x] Release-security and updater safety tests pass.
- [x] Detached signatures verify against the pinned public key.
- [ ] In-place upgrade from v1.3.3 verified.
- [ ] Existing pairing, discovery, and launcher behavior preserved.

## Packaging and integrity

- [x] Source manifest generated and verified.
- [ ] Source archive reviewed for excluded material.
- [ ] SHA-256 checksums generated and verified.
- [x] Signed Windows assets verified.
- [ ] Exact release versions and filenames verified.
- [ ] Final release source matches the approved Git tag.

## Known issues and deferred work

- Three inherited Android Bluetooth permission lint errors.
- Keyboard-visible dialog reachability review.
- Broader large-text layout review.
- Automated icon-editor reopening timing observations.

No outstanding item is represented as resolved without new evidence.

## Final status

Not approved for publication.

Final tagged-source builds, device acceptance, upgrade verification,
integrity checks, and owner approval remain required.
