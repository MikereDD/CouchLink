# CouchLink v1.3.1-dev.6.4 Validation Record

## Previous proof

`dev.5 → dev.6.3` completed successfully:

- legacy launch arguments accepted;
- downloaded payload verified;
- Host replaced and restarted;
- trusted-device state survived;
- listener and discovery resumed;
- no new updater error was written.

## Current proof objective

Validate the newer Host path in which `dev.6.3` supplies `--expected-target-sha256`.

## Expected success receipt

```text
Downloaded payload SHA-256 verified: true
Installed target SHA-256 verified: true
Replacement completed: true
Restart requested: true
```

## Pending

- [ ] Build and package.
- [ ] Publish GitHub prerelease.
- [ ] Windows discovery from `dev.6.3`.
- [ ] Replacement and restart as `dev.6.4`.
- [ ] Success receipt confirms installed-target SHA-256 verification.
- [ ] State preservation.
- [ ] Android signed update.
- [ ] Final reports.
