# CouchLink release signing

CouchLink v1.4.0 must use the established Android release signing identity. Never commit keystores or passwords.

## Existing release signing identity

CouchLink must reuse the original Android signing identity
for every compatible application update.

The established certificate was created July 21, 2026.
Do not run New-CouchLinkKeystore.ps1 to sign an update.
Generating a replacement key would break normal upgrades
from previously published versions.

The existing release keystore uses PKCS12 format, despite
its .jks filename. Use the same password for the keystore
and private-key entry when signing.

Keystore passwords may be changed without changing the
certificate. Before changing passwords, create and verify
a backup. Confirm the certificate SHA-256 fingerprint
before and after the change.

Keep signing material outside the repository and maintain
secure, separate backups.

## Build both release applications

From the repository root:

```powershell
.\Build-Release.ps1
```

The script prompts for the keystore path, keystore password, and key password. Passwords are prompted as secure input, then temporarily supplied through environment variables and a keystore.properties file containing Base64-encoded values. Base64 is not encryption. The build script normally removes these temporary credentials in its cleanup block. Verify cleanup after failed or interrupted builds.

## Build only the signed APK

From `src\android`:

```powershell
.\tools\Build-SignedRelease.ps1
```

Press Enter at the keystore-path prompt to use the default:

```text
src\android\signing\couchlink-release.jks
```

Or provide the key explicitly:

```powershell
.\tools\Build-SignedRelease.ps1 `
  -KeystorePath .\signing\couchlink-release.jks `
  -Alias couchlink-release
```

Default output:

```text
release\CouchLink-v1.4.0.apk
release\CouchLink-v1.4.0.apk.sha256
```

## Verify before publishing

```powershell
.\tools\Verify-SignedRelease.ps1
```

Verification must report valid APK signatures and display the signer certificate. The complete release builder performs this verification automatically unless `-SkipAndroidSignatureVerification` is explicitly supplied.

## Established Android signing identity

The stable release verifier expects this certificate SHA-256 fingerprint:

```text
a3d6a2b2a81c3ee0c96c911f31b3e46be663e5cf81ebfe1d5deccfc39d5b96bb
```

This fingerprint is public verification metadata, not a secret. A mismatch stops the combined release build so an APK cannot be published accidentally with the wrong key.
