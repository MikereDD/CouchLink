# CouchLink v1.3.1-dev.3.1 Validation Record

## Release assets

Validated GitHub prerelease assets:

- `CouchLink-v1.3.1-dev.3.1.apk`
- `CouchLink-Host-v1.3.1-dev.3.1-win-x64.exe`
- `CouchLink-Updater-v1.3.1-dev.3.1-win-x64.exe`
- `CouchLink-v1.3.1-dev.3.1-source.zip`
- `SHA256SUMS-v1.3.1-dev.3.1.txt`

Git tag:

- `v1.3.1-dev.3.1`
- commit: `fbb328182e689e06bdf0ddbccf78fb9dc1a9c218`

## Windows validation

Source build tested:

- installed Host: `1.3.1-dev.2.2`
- available Test-channel release: `1.3.1-dev.3`

Result: **Passed**

Confirmed:

- Test channel discovered the GitHub prerelease.
- Prerelease version comparison recognized `1.3.1-dev.3` as newer.
- Release notes and download size were displayed.
- The Host and Updater assets matched the expected names.
- Downloads completed successfully.
- GitHub SHA-256 verification passed.
- The external updater launched.
- The running Host closed.
- The previous Host executable was preserved for rollback.
- The Host executable was replaced.
- CouchLink restarted automatically as `1.3.1-dev.3`.
- Pairing, discovery, connected-device state, and update-channel selection survived the update.

## Android validation

Installed package tested:

- installed Remote: `1.3.1-dev.2.2`
- installed build type: signed Release
- available Test-channel release: `1.3.1-dev.3.1`
- available `versionCode`: `147`
- package: `dev.typezero.couchlink.remote`
- signing certificate: canonical CouchLink release certificate

Result: **Passed**

Confirmed:

- Test Builds discovered the GitHub prerelease.
- Prerelease version comparison recognized `1.3.1-dev.3.1` as newer.
- The exact APK asset was located.
- HTTPS and official GitHub release-path validation passed.
- GitHub SHA-256 verification passed.
- Package-name verification passed.
- Signing-certificate verification passed.
- The updater requested Android's Install unknown apps permission when required.
- Android's system package installer recognized the APK as an update.
- The update installed in place.
- CouchLink opened as Remote `1.3.1-dev.3.1`, Release build `147`.
- App data and settings survived the update.

## Negative-path validation

The development cycle also confirmed the updater safely rejected invalid update conditions:

- Android rejected a release-signed APK when the installed app was debug-signed.
- CouchLink rejected an APK whose `versionCode` was not greater than the installed build.
- Stable and Test channels remained separated.
- Install actions remained disabled when no valid newer release was available.
- GitHub 404 responses were translated into a friendly no-release status.
- Raw diagnostic details remained available without exposing them as the primary user-facing message.

## Final status

The CouchLink Stable/Test updater architecture is validated end to end on Windows and Android.

- Windows: **Passed**
- Android: **Passed**
- GitHub prerelease workflow: **Passed**
- Forgejo-to-GitHub source/tag workflow: **Passed**
