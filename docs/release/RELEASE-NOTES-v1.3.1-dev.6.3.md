# CouchLink v1.3.1-dev.6.3

Windows updater compatibility correction for older Host versions.

## Fix

`dev.6.2` required the new `--expected-target-sha256` argument, but an older installed Host such as `dev.5` does not know how to supply it. The downloaded `dev.6.2` updater therefore stopped before replacement with `Missing --expected-target-sha256 argument`.

The external updater now treats that argument as optional for legacy Hosts. When launched by a newer Host, installed-target SHA-256 verification remains active. Downloaded-payload SHA-256 verification, trusted staging, rollback, and restart safeguards remain mandatory.
