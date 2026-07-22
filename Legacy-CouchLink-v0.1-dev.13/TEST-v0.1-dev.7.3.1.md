# CouchLink v0.1-dev.7.3.1 — Source Metadata Refresh

This is a source and project-metadata cleanup build. Runtime behavior is unchanged from v0.1-dev.7.3.

## Verify in Android Studio

- Open `src/android`.
- Confirm the project is displayed as **CouchLink Remote**.
- Open `README.md` and confirm the heading reads **v0.1-dev.7.3 Premium Control Client**.
- Confirm the README lists persistent sessions, remote input, keyboard controls, and launcher actions.

## Build regression

```powershell
.\gradlew.bat assembleDebug
```

Confirm the APK builds and installs normally. The Android app should report version `0.1-dev.7.3.1`.
