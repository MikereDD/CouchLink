# CouchLink v1.3.1-dev.3.1 Validation Record

Expected release assets:

- `CouchLink-v1.3.1-dev.3.1.apk`
- `CouchLink-Host-v1.3.1-dev.3.1-win-x64.exe`
- `CouchLink-Updater-v1.3.1-dev.3.1-win-x64.exe`
- `CouchLink-v1.3.1-dev.3.1-source.zip`
- `SHA256SUMS-v1.3.1-dev.3.1.txt`

Android requirements:

- `versionName`: `1.3.1-dev.3.1`
- `versionCode`: `147`
- package: `dev.typezero.couchlink.remote`
- signing certificate: canonical CouchLink release certificate

Primary acceptance test: a signed `1.3.1-dev.2.2` installation downloads, verifies, and hands off the `1.3.1-dev.3.1` APK to Android's package installer as an in-place update.
