# CouchLink v1.3.1-dev.6.3 Validation Record

## Confirmed dev.6.2 failure

An installed `dev.5` Host launched the downloaded updater without the newly introduced target-hash argument:

```text
System.ArgumentException: Missing --expected-target-sha256 argument.
```

Recovery restarted the existing Host.

## Correction

The external updater now accepts legacy launches without `--expected-target-sha256`. It still enforces downloaded-payload SHA-256 verification and all path, staging, rollback, and restart safeguards. Newer Hosts continue to supply and receive installed-target hash verification.

## Pending

- [ ] Build and package.
- [ ] Publish GitHub prerelease.
- [ ] Windows discovery from `dev.5`.
- [ ] Legacy updater launch compatibility.
- [ ] Downloaded-payload SHA-256 validation.
- [ ] Replacement and restart as `dev.6.3`.
- [ ] State preservation.
- [ ] Android signed update.
- [ ] Final reports.
