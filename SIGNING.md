# CouchLink release signing

CouchLink 1.0 must be signed with a private release key. Never commit the keystore or passwords.

## One-time key creation

From PowerShell with JDK 17 available:

```powershell
.\tools\New-CouchLinkKeystore.ps1
```

Store the generated `signing\couchlink-release.jks` and both passwords in two secure backup locations. Every future CouchLink update must use this same signing identity.

## Build the signed APK

```powershell
.\tools\Build-SignedRelease.ps1 -KeystorePath .\signing\couchlink-release.jks
```

Output:

```text
release\CouchLink-v1.0.apk
release\CouchLink-v1.0.apk.sha256
```

## Verify before publishing

```powershell
.\tools\Verify-SignedRelease.ps1
```

The verification must report valid APK signatures and display the signer certificate.
