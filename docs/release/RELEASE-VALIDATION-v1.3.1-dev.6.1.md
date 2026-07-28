# CouchLink v1.3.1-dev.6.1 Validation Record

## Confirmed dev.6 failure

The `dev.6` external updater rejected the valid installed Host with:

```text
System.IO.InvalidDataException: The updater target must be CouchLink.Host.exe.
```

The recovery path restarted the existing `dev.5` Host successfully.

## Correction

The updater now binds `--target` to the executable path of the running Host process instead of enforcing a hard-coded filename.

## Pending

- [ ] Build and package.
- [ ] Publish GitHub prerelease.
- [ ] Windows discovery from `dev.5`.
- [ ] Download and updater-side SHA-256 verification.
- [ ] Replacement and restart as `dev.6.1`.
- [ ] State preservation.
- [ ] Android signed update.
- [ ] Final reports.
