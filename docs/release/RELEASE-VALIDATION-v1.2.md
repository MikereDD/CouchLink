# CouchLink 1.2 Release Validation

Record the final signed-build results here before publication.

## Build record

```text
Build date:
Git commit:
Git tag: v1.2
Android APK SHA-256:
Android certificate SHA-256:
Windows host SHA-256:
Source archive SHA-256:
```

## Android verification

```powershell
cd .\src\android
.\tools\Verify-SignedRelease.ps1 -ApkPath <path-to-CouchLink-v1.2.apk>
```

Confirm:

- package: `dev.typezero.couchlink.remote`
- version name: `1.2`
- version code: `120`
- label: `CouchLink`
- expected certificate fingerprint matches

## Functional verification

Use the checklist in `RELEASE-CHECKLIST-v1.2.md` and record any exceptions. The validated TV target for model-specific controls is the Hisense A6H Google TV named Abaddon.
