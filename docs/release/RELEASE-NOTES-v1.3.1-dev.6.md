# CouchLink v1.3.1-dev.6

Proof build for the audited Windows updater.

## Purpose

- Prove that an installed Windows `1.3.1-dev.5` Host detects same-base prerelease `1.3.1-dev.6`.
- Exercise the hardened external updater with updater-side SHA-256 re-verification.
- Confirm constrained update paths, randomized staging, replacement, restart, and preserved application state.
- Retain all `dev.5` audit fixes without adding unrelated product changes.

Android is bumped to Release build 150 for synchronized cross-platform metadata.
