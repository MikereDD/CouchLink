# CouchLink v1.4.0 — Release Checklist

Status: Release preparation. Not approved for publication.

Candidate-stage checks reflect the builds and tests completed
during release preparation. Final tagged-source artifacts,
hardware acceptance, upgrade testing, and publication approval
remain outstanding.
## 1. Source and versioning

- [x] Create isolated release-preparation worktree.
- [x] Preserve original main worktree and staged signing scripts.
- [x] Android versionName updated to 1.4.0.
- [x] Android versionCode updated to 163.
- [x] Windows Host version metadata updated to 1.4.0.
- [x] Release builder default updated to 1.4.0.
- [ ] Verify all remaining version references.
- [ ] Verify protocol version 1 remains compatible.
- [ ] Confirm final committed source matches the release tag.

## 2. Documentation

- [x] Update CHANGELOG.md.
- [x] Update README.md with Streaming Apps features.
- [x] Create v1.4.0 release notes.
- [x] Create release validation report.
- [ ] Update building and installation instructions.
- [ ] Review NOTICE, privacy, and trademark documentation.
- [ ] Generate and verify the v1.4.0 source manifest.
- [ ] Review final documentation for accuracy.

Screenshot refresh is deferred to a separate task.

## 3. Android validation

- [x] Compile Android release candidate.
- [x] Run automated unit and regression tests.
- [x] Verify Streaming Apps launcher and custom icons.
- [ ] Verify corrected D-pad on the physical Pixel.
- [ ] Check keyboard-visible dialog controls.
- [ ] Review large-text layouts.
- [x] Record inherited Bluetooth lint errors.
- [x] Verify signed APK certificate and package identity.

## 4. Windows validation

- [x] Build Windows Host in Release configuration.
- [x] Build Windows Updater.
- [x] Run Windows versioning tests.
- [x] Run release-security and updater safety tests.
- [ ] Verify Windows Host remains compatible with Android.
- [ ] Verify existing launcher, discovery, and pairing behavior.
- [x] Verify Windows release signatures.

## 5. Release packaging

- [ ] Produce final Android APK.
- [ ] Produce Windows Host executable.
- [ ] Produce Windows Updater executable.
- [ ] Produce source ZIP.
- [ ] Produce and verify detached Windows signatures.
- [ ] Generate and verify SHA-256 checksums.
- [ ] Confirm no private keys or restricted artwork are included.
- [ ] Verify exact release artifact filenames.

## 6. Upgrade validation

- [ ] Verify Android upgrade from v1.3.3 to v1.4.0.
- [ ] Verify Windows updater from v1.3.3 to v1.4.0.
- [ ] Verify trusted-device and application settings survive.
- [ ] Verify Stable/Test update channel behavior.

## 7. Publication

- [ ] Complete final release validation.
- [ ] Review and commit release-preparation changes.
- [ ] Merge approved release source into main.
- [ ] Create and verify v1.4.0 tag.
- [ ] Synchronize Forgejo and GitHub.
- [ ] Publish signed artifacts and release notes.
- [ ] Verify published release and update discovery.

No release publication without explicit owner approval.
