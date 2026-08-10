# CouchLink v1.3.1-rc.1

First release candidate for CouchLink v1.3.1. This build freezes the feature set and begins final release validation.

## Highlights

- Carries forward the signed Windows self-updater with pinned ECDSA P-256 release verification.
- Requires the external updater's detached `--signature` argument and verifies it unconditionally.
- Makes updater rollback safe to call more than once without deleting an already-restored Host.
- Adds focused updater safety tests covering rollback recovery and fail-closed signature handling.
- Carries forward **Wake PC** for trusted wired Windows Hosts, with automatic rediscovery and reconnect after S3 wake.
- Carries forward the corrected TV input selector with neutral HDMI labels and no invented current-input highlight.
- Preserves Android APK certificate/package verification and monotonic update checks.

## Version

- Android: `1.3.1-rc.1` (`versionCode 159`)
- Windows Host: `1.3.1-rc.1`

## RC policy

No new features are planned during the release-candidate phase. Changes after this point should be limited to validated release blockers, regressions, and release-process corrections.
