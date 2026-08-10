# CouchLink v1.3.1

CouchLink v1.3.1 is the stable release of the completed 1.3.1 development and release-candidate cycle.

## Highlights

- **Wake PC** for a previously trusted wired Windows Host, with automatic rediscovery and authenticated reconnect after S3 wake.
- Signed Windows updates using detached ECDSA P-256 signatures verified against the pinned CouchLink release key.
- Hardened external updater behavior: detached `--signature` is mandatory and rollback is safe to invoke more than once.
- Android update identity checks preserve package-name, signing-certificate, SHA-256, and monotonic `versionCode` validation.
- Corrected TV input selector with neutral HDMI labels, preserved HW4/HW5/HW6 mappings, and no invented current-input highlight.
- Continued launcher controls, touchpad/keyboard, Bluetooth HID, Audio Output, trusted reconnect, and living-room control workflows.

## Version

- Android: `1.3.1` (`versionCode 160`)
- Windows Host: `1.3.1`

## Release status

Stable. This release is intended to replace the validated `v1.3.1-rc.1` build after final signed-build and in-place update checks.
