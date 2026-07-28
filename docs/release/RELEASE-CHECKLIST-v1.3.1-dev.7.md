# CouchLink v1.3.1-dev.7 Release Checklist

- [ ] Run `scripts\New-WindowsReleaseSigningKey.ps1` once.
- [ ] Back up the private key outside the repository.
- [ ] Confirm `PinnedReleaseKey.cs` contains only the public key.
- [ ] Version-comparison tests pass.
- [ ] Signed Android APK builds and verifies.
- [ ] Windows Host and Updater publish.
- [ ] Detached `.sig` files are generated for both Windows executables.
- [ ] Automated valid-signature verification passes for Host and Updater.
- [ ] Automated modified-payload rejection passes for Host and Updater.
- [ ] Automated modified-signature rejection passes for Host and Updater.
- [ ] Automated malformed-signature rejection passes for Host and Updater.
- [ ] Public-key fingerprint matches `28ec51ebeeea7ae970f9c62447868818a51932697f64472ec79e5ab0d906f93c`.
- [ ] Source manifest regenerates and verifies.
- [ ] GitHub prerelease contains both executables and both `.sig` files.
- [ ] Bootstrap update from `dev.6.4` completes.
- [ ] Follow-up signed-to-signed update proves enforcement.

- [ ] Bootstrap success receipt reports `Detached release signature verified: false`.
- [ ] Signed-to-signed proof receipt reports `Detached release signature verified: true`.
