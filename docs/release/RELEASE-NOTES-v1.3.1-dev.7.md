# CouchLink v1.3.1-dev.7

Independent Windows release-authenticity development build.

Windows Host and Updater assets now use detached ECDSA P-256 signatures. The public key is pinned in the applications; the private key remains outside the repository.

The Host verifies both downloaded executables before launching the updater. The updater then verifies the Host signature again immediately before replacement.

## Bootstrap note

An older Host such as `dev.6.4` does not yet enforce detached signatures. Updating from `dev.6.4` to this build bootstraps the signed updater. The full enforcement proof requires a subsequent signed build updated from this version.


## Canonical signing identity

Public-key SHA-256 fingerprint:

```text
28ec51ebeeea7ae970f9c62447868818a51932697f64472ec79e5ab0d906f93c
```

The release builder now runs valid-signature and tamper-rejection tests against both Windows executables. The external updater accepts a missing signature argument only for the one-time `dev.6.4 → dev.7` bootstrap. A later signed-to-signed update must report `Detached release signature verified: true`.
