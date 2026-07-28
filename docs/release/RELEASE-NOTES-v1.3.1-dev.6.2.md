# CouchLink v1.3.1-dev.6.2

Windows updater process-exit race correction.

## Fix

`dev.6.1` attempted to reopen the Host process to confirm its executable path. The Host could finish shutting down before the updater performed that check, causing a valid update to fail with `Process with an Id ... is not running`.

The Host now hashes its installed executable before launching the updater. After shutdown, the updater confirms the target file still matches that expected hash before replacement. This removes the process-lifetime race while retaining trusted staging, target/restart equality, downloaded-payload SHA-256 verification, rollback, and restart-on-failure.
