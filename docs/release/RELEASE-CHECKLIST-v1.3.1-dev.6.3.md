# CouchLink v1.3.1-dev.6.3 Release Checklist

- [ ] All version-comparison tests pass.
- [ ] Signed Android APK builds and certificate verification passes.
- [ ] Windows Host and Updater publish successfully.
- [ ] Source manifest regenerates and verifies.
- [ ] Tag and GitHub prerelease point to the tested commit.
- [ ] Windows `dev.5` detects `dev.6.3`.
- [ ] Legacy Host launches the updater without `--expected-target-sha256`.
- [ ] Downloaded Host SHA-256 verification passes.
- [ ] Replacement and automatic restart pass.
- [ ] Newer Host path retains installed-target SHA-256 verification.
- [ ] Host settings and trusted-device state survive.
- [ ] Android signed update passes.
