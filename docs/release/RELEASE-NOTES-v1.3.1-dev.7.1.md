# CouchLink v1.3.1-dev.7.1

Signed-to-signed updater enforcement proof build.

## Purpose

`dev.6.4 → dev.7` successfully bootstrapped the first signature-aware Windows Host. This build proves that a signature-aware Host requires and validates detached signatures through the complete update flow.

A successful `dev.7 → dev.7.1` update must report:

```text
Downloaded payload SHA-256 verified: true
Installed target SHA-256 verified: true
Detached release signature verified: true
Replacement completed: true
Restart requested: true
```

The release uses the same canonical CouchLink ECDSA P-256 signing identity introduced in `dev.7`.
