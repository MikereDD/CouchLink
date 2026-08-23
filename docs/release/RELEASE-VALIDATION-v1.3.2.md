# CouchLink v1.3.2 Validation Record

## Candidate scope

CouchLink v1.3.2 packages the reviewed LAN-interface-selection pull request plus CouchLink-native presentation for the new Windows Host selector.

The release does not change launcher protocol v1, pairing/trust identity, Wake-on-LAN semantics, Audio Output, Bluetooth HID, or Google TV Remote behavior.

## Networking validation already completed

The LAN-interface work was reviewed and exercised on real hardware before stable preparation. Covered behavior includes:

- automatic physical Ethernet/Wi-Fi candidate selection;
- delayed interface availability at startup;
- selected-interface loss without silent failover;
- explicit manual reselection;
- candidate inventory refresh notifications;
- stopped-runtime advertisement protection;
- selected-interface-only UDP advertisement;
- unchanged TCP listener behavior on `IPAddress.Any:45821`;
- Android Wi-Fi multicast-lock lifecycle during Host discovery.

Issues discovered during hardware testing were corrected before the final pull-request commit was merged.

## UI validation already completed

The network-interface selector was then themed on a dedicated `network-switch-theme` branch using CouchLink's existing WPF styles.

Observed final state:

- the selector uses the CouchLink ComboBox treatment;
- the selected item shows the adapter's friendly `DisplayName`;
- the dropdown shows the expected Ethernet and Wi-Fi candidates;
- **USE INTERFACE** uses the CouchLink accent-button treatment;
- the Windows Release solution builds successfully after the UI change.

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

- [ ] Windows Stable-channel update `1.3.1 → 1.3.2`
- [ ] Android signed update `versionCode 160 → 161`
- [ ] settings/trust preservation
- [ ] post-update discovery and launcher connection

## Security status

v1.3.2 does not claim to resolve the previously deferred hardening items. The machine-wide plaintext trusted-token mirror, pairing-code throttling, prerelease-rank alignment, and future Host transport encryption remain separate backlog work.

## Final status

Pending final signed build, source-manifest generation, exact-tag verification, in-place updater proof, and publication.
