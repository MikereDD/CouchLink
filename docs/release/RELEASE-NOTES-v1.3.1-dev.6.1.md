# CouchLink v1.3.1-dev.6.1

Windows updater target-validation correction.

## Fix

The hardened `dev.6` updater incorrectly required the installed Host filename to be exactly `CouchLink.Host.exe`. Published and manually organized CouchLink Hosts can use versioned filenames, so valid updates were rejected before replacement.

`dev.6.1` now verifies that the requested target is the executable of the actual running CouchLink Host process. It retains randomized trusted staging, updater-side SHA-256 verification, matching target/restart paths, rollback, and restart-on-failure behavior.
