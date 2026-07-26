# CouchLink v1.3 Release Notes

CouchLink v1.3 is the stable unified release for the Android remote and Windows Host.

## Highlights

- Complete Android Home, Touchpad, Keyboard, launcher, and secure Google TV Remote experience.
- Eight first-class Windows launcher integrations with trusted local pairing and reconnect.
- Windows Audio Output discovery and switching by stable endpoint ID.
- Headphones and TV / Display favorites on Android and Windows.
- Live synchronization when the Windows default output or available endpoints change.
- Redesigned Windows Host dashboard and Audio Output popup using the unified CouchLink theme.
- Shared, atomic host preferences with corruption preservation and fault-isolated endpoint enumeration.
- Updated README screenshot gallery covering Android and Windows.

## Stability

The v1.3 feature set was promoted from the tested v1.3-rc.1 baseline. No new feature work was introduced after the release candidate; stable preparation is limited to versioning, documentation, manifests, and release packaging.

## Release artifacts

Run `Build-Release.ps1` on Windows to produce and verify:

- signed Android APK
- Windows single-file executable
- clean source archive
- release documents
- SHA-256 checksums
- Android signing-certificate verification
