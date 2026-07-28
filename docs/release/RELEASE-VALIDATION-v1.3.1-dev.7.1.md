# CouchLink v1.3.1-dev.7.1 Validation Record

## Previous proof

`dev.6.4 → dev.7` completed successfully:

- downloaded payload SHA-256 verified;
- installed target SHA-256 verified;
- Host replaced and restarted;
- no updater error was written;
- detached signature verification was `false`, as expected for the pre-signature bootstrap Host.

Windows and Android both reported `dev.7` up to date after installation.

## Current proof objective

Validate the complete signed-to-signed Windows path from `dev.7` to `dev.7.1`.

## Expected success receipt

```text
Downloaded payload SHA-256 verified: true
Installed target SHA-256 verified: true
Detached release signature verified: true
Replacement completed: true
Restart requested: true
```

## Pending

- [ ] Build and package.
- [ ] Publish GitHub prerelease.
- [ ] Windows discovery from `dev.7`.
- [ ] Host and Updater signature enforcement.
- [ ] Replacement and automatic restart as `dev.7.1`.
- [ ] Success receipt confirms detached signature verification.
- [ ] State preservation.
- [ ] Android signed update.
- [ ] Final reports.
