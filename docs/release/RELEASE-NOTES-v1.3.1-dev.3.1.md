# CouchLink v1.3.1-dev.3.1

## Android updater version-code repair candidate

This selected Test-channel prerelease corrects the Android package ordering discovered during the first live updater installation test.

- Raises Android `versionCode` from `146` to `147`.
- Publishes Android version `1.3.1-dev.3.1` so it is newer than the signed `1.3.1-dev.2.2` test client.
- Preserves the successful Windows updater implementation from `1.3.1-dev.3`.
- Preserves HTTPS, exact asset-name, GitHub SHA-256, Android package, and signing-certificate verification.
- Exercises Android verified APK download and normal package-installer handoff.

Publish this as a GitHub **Prerelease**, not a stable release.
