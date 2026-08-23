# CouchLink v1.3.3 Validation Record

## Candidate scope

CouchLink v1.3.3 packages two reviewed changes on top of v1.3.2:

1. the Android Launcher Tray and host-only launcher execution path;
2. persistent Windows Host network-interface selection with safe fallback.

The release does not change launcher protocol v1, pairing/trust identity, updater security, Audio Output, Bluetooth HID keyboard/mouse behavior, or the existing Google TV Remote path.

## Hardware validation already completed

The feature and regression fix were exercised on real hardware before stable preparation.

Observed behavior:

- the Launcher Tray opens and launcher controls operate through the Windows Launcher Host;
- launcher execution no longer uses the retired Bluetooth HID / Windows Run fallback;
- both wired and Wi-Fi Host interface selections persist across a full Host exit/restart;
- after restart, the Android app reconnects to the Launcher Host without pressing **Use Interface** again;
- Wake-on-LAN remains available for the trusted wired Host;
- the PC wakes successfully from the Android app;
- the Android app reconnects to the Launcher Host after Windows and CouchLink Host return.

## Stable automated validation

Pending final release preparation:

- [ ] Windows Release solution build
- [ ] version-comparison/network lifecycle tests
- [ ] updater/release-security tests
- [ ] Windows Host publish
- [ ] Windows Updater publish
- [ ] Android signed APK
- [ ] Android certificate verification
- [ ] Windows detached-signature valid/tamper tests
- [ ] source-manifest verification

## Stable in-place update proof

Pending publication-candidate build:

- [ ] Windows Stable-channel update `1.3.2 → 1.3.3`
- [ ] Android signed update `versionCode 161 → 162`
- [ ] settings/trust/interface-selection preservation
- [ ] post-update Launcher Tray, Wake PC, discovery, and launcher connection

## Security status

v1.3.3 does not claim to resolve the previously deferred hardening items. The machine-wide plaintext trusted-token mirror, pairing-code throttling, prerelease-rank alignment, and future Host transport encryption remain separate backlog work.

## Final status

Pending final signed build, source-manifest generation, exact-tag verification, in-place updater proof, and publication.
