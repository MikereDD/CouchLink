# CouchLink v1.4.0 — Release Notes

Status: Published stable release.

## Highlights

### Google TV Streaming Apps

- Launch supported streaming services through the existing secure Google TV Remote v2 connection.
- Add a Streaming Apps launcher to the TV Remote interface.
- Provide shortcuts for Netflix, Apple TV, and Hulu.
- Support custom app shortcuts with user-defined launch URIs.
- Add, reorder, manage, and remove persistent shortcuts.
- Import PNG, JPEG, and WebP images as custom shortcut icons.
- Preview, replace, and reset custom icons.
- Store normalized custom artwork privately on the Android device.
- Automatically retrieve and cache publisher-hosted service icons, with generic fallback and custom overrides.

### TV Remote improvements

- Correct the directional pad geometry so Up, Down, Left, and Right are symmetrically positioned around OK.
- Preserve existing navigation, media, pairing, and connection behavior.

### Windows Host and Updater

- Update the Windows Host release version to 1.4.0.
- Include the Windows Host and Updater in the unified release package.
- Preserve shared launcher protocol version 1 and existing Windows functionality.
- No new Windows Host feature is claimed in this release.

## Versions

- Android Remote: 1.4.0 (versionCode 163)
- Windows Launcher Host: 1.4.0
- Windows Updater: v1.4.0 release package
- Shared launcher protocol: 1

## Validation status

Development-build validation:
- Streaming Apps and custom-icon workflows exercised on real hardware.
- 56 host regression tests passed, including 8 D-pad geometry tests.
- Four publication-policy checks passed.

Local v1.4.0 release-candidate validation:
- Android unit tests and signed release build passed.
- Android APK v3 signature and established certificate verified.
- Android package and versionCode 163 verified.
- Android APK SHA-256 checksum verified.
- Windows Host and corrected v1.4.0 Updater built successfully.
- Windows versioning and updater safety tests passed.
- Both Windows detached signatures passed authenticity and tamper tests.
- Source manifest verified 446 files, excluding Git metadata.

Final release checks:
- Android v1.3.3 to v1.4.0 in-place installation succeeded.
- Windows Host v1.4.0 connected to the updated Android remote.
- Isolated Windows updater replacement and rollback-backup checks passed.
- All six release artifacts passed checksum verification.
- All 446 source archive files passed integrity verification.

Follow-up: Final D-pad physical-layout verification and keyboard-visible/large-text UI reviews remain separately unconfirmed.
Three inherited Android Bluetooth permission lint errors remain
documented; they have not been resolved by this release preparation.

## Release status

CouchLink v1.4.0 is the latest published stable release.

Updated screenshots will be prepared separately.
