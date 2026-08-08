# CouchLink GitHub Prerelease Workflow

This is the validated release order for CouchLink Test-channel builds.

1. Prepare and build the release source on the active release branch (for this cycle, `release/1.3.1`).
2. Run the Windows solution build.
3. Run the root signed release builder.
4. Confirm the exact release assets and SHA-256 manifest.
5. Commit all source, version, manifest, and release-document changes.
6. Push the branch to canonical Forgejo.
7. Push the same branch commit to the GitHub mirror.
8. Create the version tag locally on the exact release commit.
9. Push the tag to Forgejo and GitHub.
10. On GitHub, draft a release using the existing pushed tag.
11. Mark the release as **Pre-release**.
12. Paste the matching release notes.
13. Upload:
    - APK
    - Windows Host EXE
    - Windows Updater EXE
    - source ZIP
    - `SHA256SUMS` file
14. Publish the prerelease.
15. Test from an older signed Test-channel build.
16. Record validation results before promoting any build toward Stable.

## Important rules

- Forgejo remains the canonical source remote.
- GitHub is the public mirror and release host.
- Never create a GitHub release tag from an outdated `main` commit.
- Android `versionCode` must always increase, regardless of `versionName`.
- Android update testing must use release-signed builds with the canonical certificate.
- Stable remains the default channel.
- Test builds are opt-in.
- Internal engineering builds do not need to be offered publicly.
