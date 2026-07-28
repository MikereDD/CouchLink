# CouchLink Windows Release Signing

CouchLink uses detached ECDSA P-256 signatures with SHA-256 for independent Windows release authenticity.

## One-time setup

```powershell
.\scripts\New-WindowsReleaseSigningKey.ps1
```

The private key defaults to:

```text
$HOME\Documents\CouchLink\Keys\windows-release-private.pem
```

The script writes only the public key into:

```text
src\windows\CouchLink.ReleaseSecurity\PinnedReleaseKey.cs
```

The generator performs a sign/verify self-test before reporting success.

Back up the private key securely. Never commit or upload it.

## Release build

Run the normal release builder. It signs the Windows Host and Updater and creates:

```text
CouchLink-Host-v<version>-win-x64.exe.sig
CouchLink-Updater-v<version>-win-x64.exe.sig
```

Upload both signature files with the corresponding executables.

## Enforcement

The Host verifies both executables before launching the Updater. The Updater independently verifies the Host again before replacement.

SHA-256 checks remain mandatory but do not replace signature verification.

## Tamper test

Copy a signed executable and change one byte. Signature verification must fail. Never alter the actual release artifact used for a valid-update test.



## PowerShell compatibility

The generator creates the P-256 curve with
`ECCurve.CreateFromFriendlyName('nistP256')`. This avoids PowerShell treating
`.NamedCurves` as a nonexistent property.

## Verify the configured key later

```powershell
.\scripts\Test-WindowsReleaseSigningKey.ps1
```


## Canonical CouchLink key

```text
SHA-256: 28ec51ebeeea7ae970f9c62447868818a51932697f64472ec79e5ab0d906f93c
```

The build runs `CouchLink.ReleaseSecurity.Tests` against the signed Host and Updater. Each asset must accept its valid signature and reject a modified payload, modified signature, and malformed signature.

The `--signature` updater argument is optional only during the one-time bootstrap from an older unsigned-enforcement Host. Signed-to-signed updates require and reverify it.
