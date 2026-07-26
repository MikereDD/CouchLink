# CouchLink v1.3-rc.1 Release Notes

CouchLink v1.3-rc.1 is the feature-complete release candidate for the unified Android remote and Windows Host release line.

## Included

- Complete Android Home, Touchpad, Keyboard, and secure Google TV Remote experience.
- Eight first-class Windows launcher integrations with trusted local pairing and reconnect.
- Windows Audio Output discovery and switching by stable endpoint ID.
- Headphones and TV / Display favorites on Android and Windows.
- Live synchronization when the Windows default output or available endpoints change.
- Redesigned Windows Host dashboard and Audio Output popup using the unified CouchLink theme.
- Shared, atomic host preferences with corruption preservation and fault-isolated endpoint enumeration.
- Updated README screenshot gallery covering Android and Windows.

## Release-candidate policy

No new features are planned between this build and v1.3 stable. Only confirmed build, packaging, signing, crash, data-loss, synchronization, audio-switching, accessibility, or serious visual defects should be changed.

## Required final builds

Run `Build-Release.ps1` on Windows to produce and verify the signed Android APK, Windows executable package, clean source archive, signing certificate, and SHA-256 files.
