# CouchLink GitHub Stable Release Workflow

This is the stable-publication order for CouchLink.

1. Prepare the stable source on `release/1.3.1` and verify the working tree and version metadata.
2. Regenerate and verify `SOURCE-MANIFEST-v1.3.1.sha256`.
3. Run focused tests and a full Windows Release solution build.
4. Commit the stable-preparation source and push the release branch to Forgejo and GitHub.
5. Run the signed release builder and inspect the complete release artifact set.
6. Perform manual install/regression checks against the built Android APK and Windows Host.
7. Merge the exact validated stable source into `main` and push `main` to Forgejo and GitHub.
8. Tag the exact stable release commit `v1.3.1` and push the tag to both remotes.
9. Draft the GitHub release from the existing pushed tag.
10. Do **not** mark the release as Pre-release.
11. Upload the approved 13 assets: Host EXE/hash/signature/signature-hash, Updater EXE/hash/signature/signature-hash, APK/hash, source ZIP/hash, and combined `SHA256SUMS`.
12. Publish the stable release and confirm it is the latest stable release.
13. From the installed RC, use the Stable channel to validate the published in-place update on Windows and Android.
14. Record the final stable validation results.

## Important rules

- Forgejo remains the canonical source remote.
- GitHub remains the public mirror and release host.
- The stable tag must point to the exact validated stable source commit.
- Android `versionCode` must increase; `1.3.1` uses `160`.
- Android stable update testing must use the canonical release signing certificate.
- Windows release binaries must retain detached ECDSA signatures verified against the pinned CouchLink public key.
- Never publish private signing material, keystores, passwords, or machine-specific files.
