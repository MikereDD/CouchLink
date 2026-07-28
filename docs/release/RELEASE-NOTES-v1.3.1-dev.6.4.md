# CouchLink v1.3.1-dev.6.4

Installed-target SHA-256 verification proof build.

## Purpose

The successful `dev.5 → dev.6.3` update proved the legacy compatibility path. This build validates the newer Host protocol introduced in `dev.6.2`.

After a successful update, the updater writes:

```text
%TEMP%\CouchLink-Updater-success.log
```

A successful `dev.6.3 → dev.6.4` update should report:

```text
Downloaded payload SHA-256 verified: true
Installed target SHA-256 verified: true
Replacement completed: true
Restart requested: true
```

The receipt is diagnostic only and cannot cause an otherwise successful update to fail.
