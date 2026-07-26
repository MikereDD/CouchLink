# CouchLink 1.1 Release Checklist

## Stable source and versioning

- [x] Android version name set to `1.1`.
- [x] Android version code set to `106`.
- [x] Windows projects and runtime host version set to `1.1`.
- [x] Protocol remains version `1`.
- [x] README, changelog, release notes, privacy, security, license, notice, and trademark references synchronized.
- [x] Release-candidate wording removed from current documentation.
- [x] Android build toolchain pinned to the supported Gradle `8.13` line.
- [x] Kotlin and native-symbol packaging warnings from the validated build addressed.
- [x] Official Android signing-certificate fingerprint pinned in release verification.
- [x] Source-manifest update and verification tooling added.
- [x] Generated output and secrets excluded from the source archive.

## Pipeline validation

- [x] Combined signed APK and Windows EXE builder completed successfully on the Windows development workstation.
- [x] Android release signature verified with one signer.
- [x] Windows framework-dependent single-file publish completed without warnings.
- [x] Individual artifact checksums and combined checksum list were generated.
- [ ] Run `Build-Release.ps1` once from the final stable source after cleanup.
- [ ] Confirm the final output includes APK, EXE, source ZIP, release documents, per-file hashes, and `SHA256SUMS-v1.1.txt`.
- [ ] Confirm the final Android certificate digest matches the pinned CouchLink release fingerprint.
- [ ] Install the final APK and EXE and perform a brief launch, pairing, input, and launcher smoke test.

## Publication

- [ ] Commit the exact stable source to the Forgejo repository.
- [ ] Tag the exact release commit as `v1.1`.
- [ ] Push the same release commit and `v1.1` tag to the public mirror.
- [ ] Attach `CouchLink-v1.1.apk`.
- [ ] Attach `CouchLink-Host-v1.1-win-x64.exe`.
- [ ] Attach `CouchLink-v1.1-source.zip`.
- [ ] Attach all `.sha256` files and `SHA256SUMS-v1.1.txt`.
- [ ] Publish `RELEASE-NOTES-v1.1.md` and installation instructions.
- [ ] Preserve the complete v1.1 release as a separate immutable archive.
