# CouchLink release signing

CouchLink 1.1 must be signed with a private release key. Never commit the keystore or passwords.

## One-time key creation

From PowerShell with JDK 17 available:

```powershell
cd .\src\android
.\tools\New-CouchLinkKeystore.ps1
```

Store the generated `signing\couchlink-release.jks` and both passwords in two secure backup locations. Every future CouchLink update must use this same signing identity.

## Build both release applications

From the repository root:

```powershell
.\Build-Release.ps1
```

The script prompts for the keystore path, keystore password, and key password. Passwords are read as secure input, passed to Gradle only through temporary environment variables, and removed after the build.

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
release\CouchLink-v1.1.apk
release\CouchLink-v1.1.apk.sha256
```

## Verify before publishing

```powershell
.\tools\Verify-SignedRelease.ps1
```

Verification must report valid APK signatures and display the signer certificate. The complete release builder performs this verification automatically unless `-SkipAndroidSignatureVerification` is explicitly supplied.

## Official v1.1 signing identity

The stable release verifier expects this certificate SHA-256 fingerprint:

```text
a3d6a2b2a81c3ee0c96c911f31b3e46be663e5cf81ebfe1d5deccfc39d5b96bb
```

This fingerprint is public verification metadata, not a secret. A mismatch stops the combined release build so an APK cannot be published accidentally with the wrong key.
