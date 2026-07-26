# CouchLink v1.3.1-dev.3.1 Release Checklist

- [ ] Confirm Android `versionName` is `1.3.1-dev.3.1`.
- [ ] Confirm Android `versionCode` is `147`.
- [ ] Run `dotnet build .\src\windows\CouchLink.sln -c Release`.
- [ ] Run `.\Build-Release.ps1 -Version 1.3.1-dev.3.1`.
- [ ] Verify the signed APK uses the canonical CouchLink certificate.
- [ ] Create tag `v1.3.1-dev.3.1` from the updater development branch.
- [ ] Mark the GitHub release as **Pre-release**.
- [ ] Upload the exact APK, Host, Updater, source ZIP, and SHA256SUMS assets.
- [ ] From signed Android `1.3.1-dev.2.2`, verify Test detects and installs `1.3.1-dev.3.1`.
- [ ] Confirm settings and pairing state survive the Android update.
