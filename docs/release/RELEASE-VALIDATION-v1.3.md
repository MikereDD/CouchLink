# CouchLink 1.3 Release Validation

Record the final signed-build results here before publication.

## Build record

```text
Build date:
Git commit:
Git tag: v1.3
Android APK SHA-256:
Android certificate SHA-256:
Windows host SHA-256:
Source archive SHA-256:
```

## Android verification

```powershell
cd .\src\android
.\tools\Verify-SignedRelease.ps1 -ApkPath <path-to-CouchLink-v1.3.apk>
```

Confirm:

- package: `dev.typezero.couchlink.remote`
- version name: `1.3`
- version code: `141`
- label: `CouchLink`
- expected certificate fingerprint matches

## Windows verification

```powershell
dotnet build .\src\windows\CouchLink.sln -c Release
```

Confirm the Host status badge reports `1.3`, the themed Host dashboard renders correctly, and the Audio Output popup switches and synchronizes endpoints without UI freezes.

## Functional verification

Use `RELEASE-CHECKLIST-v1.3.md` and record any exceptions. The validated TV target for model-specific controls remains the Hisense A6H Google TV named Abaddon.
