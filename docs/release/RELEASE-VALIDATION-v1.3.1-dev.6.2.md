# CouchLink v1.3.1-dev.6.2 Validation Record

## Confirmed dev.6.1 failure

The `dev.6.1` updater failed after the Host had already exited:

```text
System.ArgumentException: Process with an Id ... is not running.
```

Recovery restarted the existing `dev.5` Host.

## Correction

The Host now computes and passes the SHA-256 of its installed executable before launching the updater. After shutdown, the updater verifies the target file against that hash instead of querying the exited process.

## Pending

- [ ] Build and package.
- [ ] Publish GitHub prerelease.
- [ ] Windows discovery from `dev.5`.
- [ ] Existing-target SHA-256 validation.
- [ ] Downloaded-payload SHA-256 validation.
- [ ] Replacement and restart as `dev.6.2`.
- [ ] State preservation.
- [ ] Android signed update.
- [ ] Final reports.
