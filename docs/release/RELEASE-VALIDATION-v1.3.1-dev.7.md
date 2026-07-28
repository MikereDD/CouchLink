# CouchLink v1.3.1-dev.7 Validation Record

## Objective

Close the remaining Windows updater audit blocker by adding independent release authenticity.

## Signature design

- Algorithm: ECDSA P-256 with SHA-256.
- Private key: stored outside the repository under `$HOME`.
- Public key: pinned in `CouchLink.ReleaseSecurity`.
- Host verification: downloaded Host and Updater before execution.
- Updater verification: downloaded Host again immediately before replacement.
- SHA-256 verification remains required.

## Pending

- [ ] Generate the local signing key.
- [ ] Build and package.
- [ ] Confirm `.sig` assets.
- [ ] Automated valid-signature verification for Host and Updater.
- [ ] Automated modified-payload rejection for Host and Updater.
- [ ] Automated modified-signature rejection for Host and Updater.
- [ ] Automated malformed-signature rejection for Host and Updater.
- [ ] Canonical public-key fingerprint verification.
- [ ] Bootstrap update from `dev.6.4`.
- [ ] Signed-to-signed proof build.


## Canonical public-key fingerprint

```text
28ec51ebeeea7ae970f9c62447868818a51932697f64472ec79e5ab0d906f93c
```

## Bootstrap compatibility

The `dev.7` updater treats `--signature` as optional only when launched by an older Host that cannot supply it. The bootstrap receipt is expected to report signature verification as `false`. Every signed-to-signed update launched by `dev.7` or newer must supply the signature and report verification as `true`.
