# CouchLink v0.1-dev.9.7 — Authentic Launcher Icons

Android-only visual update. Keep the existing v0.1-dev.9.6 Windows host running.

## Build

```powershell
cd .\CouchLink\src\android
$env:JAVA_HOME = 'G:\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

APK: `app\build\outputs\apk\debug\app-debug.apk`

## Visual checks

- Steam uses the recognizable Steam symbol.
- GOG uses the recognizable GOG.com/Galaxy mark.
- Xbox uses the Xbox sphere-X mark.
- EA uses the EA monogram.
- Ubisoft uses the Ubisoft swirl.
- Rockstar uses the R-star mark.
- Epic uses the Epic Games shield.
- Amazon uses the Amazon Games wordmark/smile.
- All icons are crisp, centered, evenly padded, and optically balanced.
- Steam and GOG remain featured tiles; the lower six remain a balanced 3 × 2 grid.

## Regression checks

- Every launcher still opens correctly.
- Steam still launches Big Picture.
- Xbox launch handler still works.
- Trusted reconnect, touchpad, keyboard, and Command Deck remain functional.
