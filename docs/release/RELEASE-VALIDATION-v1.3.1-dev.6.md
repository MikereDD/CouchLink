# CouchLink v1.3.1-dev.6 Validation Record

## Purpose

This prerelease exists to prove the audited Windows updater can perform a same-base prerelease update from `1.3.1-dev.5` to `1.3.1-dev.6`.

## Pre-build status

- Windows prerelease-aware comparator: implemented in `dev.5`.
- Automated comparator tests: present.
- External updater SHA-256 re-verification: present.
- Randomized staging and constrained paths: present.
- Rollback and restart-on-failure hardening: present.
- Windows independent binary authenticity: still required before stable promotion.

## Pending validation

- [ ] Complete release build.
- [ ] Publish GitHub prerelease.
- [ ] Windows `dev.5 → dev.6` discovery.
- [ ] Windows download and updater-side SHA-256 verification.
- [ ] Windows replacement and automatic restart.
- [ ] Windows state preservation.
- [ ] Android `dev.5 → dev.6` signed in-place update.
- [ ] Capture final Windows and Android test reports.
