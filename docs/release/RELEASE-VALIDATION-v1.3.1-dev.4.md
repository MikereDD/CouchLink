# CouchLink v1.3.1-dev.4 Validation Record

## Release identity

Published GitHub prerelease assets:

- `CouchLink-v1.3.1-dev.4.apk`
- `CouchLink-v1.3.1-dev.4.apk.sha256`
- `CouchLink-Host-v1.3.1-dev.4-win-x64.exe`
- `CouchLink-Updater-v1.3.1-dev.4-win-x64.exe`
- `CouchLink-v1.3.1-dev.4-source.zip`
- `SHA256SUMS-v1.3.1-dev.4.txt`

Git tag:

- `v1.3.1-dev.4`
- commit: `62b6a00da36610ac3479045a0ff4cee329ad84de`

## Build validation

Result: **Passed**

Confirmed:

- Signed Android Release APK compiled successfully.
- APK Signature Scheme v3 verification passed.
- The canonical CouchLink Android signing certificate was verified.
- Windows Host published successfully for `win-x64`.
- Windows Updater published successfully for `win-x64`.
- Source packaging completed successfully.
- The source manifest regenerated and verified with 213 entries.
- SHA-256 values were generated for all primary release assets.

## Windows updater validation

Starting Host:

- Host version: `1.3.1-dev.2.2`
- update channel: Test
- available release: `1.3.1-dev.4`

Result: **Passed**

Confirmed:

- The Test channel discovered the published GitHub prerelease.
- Prerelease version comparison recognized `1.3.1-dev.4` as newer.
- The Host and Updater assets matched the expected names.
- The update downloaded successfully.
- SHA-256 verification passed.
- The external updater launched.
- The running Host closed.
- The previous Host executable was preserved for rollback.
- The Host executable was replaced.
- CouchLink restarted automatically as `1.3.1-dev.4`.
- Host identity, trusted-device data, update-channel selection, and working state survived the update.
- The new updater controls were present after restart:
  - Return to Stable
  - Copy Test Report
- Windows updater stages no longer produced a blank Stage field.

Final Windows Test-channel report:

```text
CouchLink Updater Test Report
Host version: 1.3.1-dev.4
Channel: Test
Available version: None
Status: CouchLink is up to date.
Stage: Check complete
Progress: 0%
Timestamp: 2026-07-26T15:33:20.1346751-05:00
Error: None
```

## Android updater validation

Starting Remote:

- installed Remote: `1.3.1-dev.3.1`
- update channel: Test
- available release: `1.3.1-dev.4`
- target `versionCode`: `148`
- package: `dev.typezero.couchlink.remote`
- signing certificate: canonical CouchLink release certificate

Result: **Passed**

Confirmed:

- Test Builds discovered the published GitHub prerelease.
- Prerelease version comparison recognized `1.3.1-dev.4` as newer.
- The exact APK asset was located.
- HTTPS and official GitHub release-path validation passed.
- GitHub SHA-256 verification passed.
- Package-name verification passed.
- Signing-certificate verification passed.
- Android's system package installer recognized the APK as an update.
- The update installed in place.
- CouchLink opened as Remote `1.3.1-dev.4`.
- App data and settings survived the update.
- Return to Stable and Copy Test Report were present.
- The final Test-channel check correctly reported the current build as up to date.
- No downgrade or unnecessary reinstall was offered.

Final Android Test-channel report:

```text
CouchLink Updater Test Report
Remote version: 1.3.1-dev.4-debug (148)
Channel: Test
Available: 1.3.1-dev.4
Stage: Up to date
Status: CouchLink 1.3.1-dev.4-debug is up to date.
Progress: 0%
```

## Stable-channel and negative-path validation

Confirmed:

- Stable and Test channels remained separated.
- Stable remained the default channel.
- Test-channel use remained opt-in.
- Return to Stable restored the Stable channel.
- Install actions remained disabled when no valid newer release was available.
- Windows translated GitHub 404 responses into a friendly no-release status.
- Raw diagnostic details remained available without replacing the user-facing status.
- Version comparison prevented downgrade behavior.
- Technical diagnostics could be copied from both apps.

## Final status

- Source compilation: **Passed**
- Android signing and certificate verification: **Passed**
- Windows Host and Updater publishing: **Passed**
- Source-manifest verification: **Passed**
- GitHub prerelease publication: **Passed**
- Windows `1.3.1-dev.2.2 → 1.3.1-dev.4`: **Passed**
- Android `1.3.1-dev.3.1 → 1.3.1-dev.4`: **Passed**
- Windows post-update Test-channel check: **Passed**
- Android post-update Test-channel check: **Passed**
- Stable/Test separation and no-downgrade behavior: **Passed**

CouchLink `v1.3.1-dev.4` is validated end to end on Windows and Android.
