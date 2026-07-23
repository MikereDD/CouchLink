# Test checklist — v0.2-dev.10.1

## Build

- [ ] Open the project in Android Studio.
- [ ] Allow Gradle sync to complete.
- [ ] Run `Build > Clean Project`.
- [ ] Run `Build > Rebuild Project` or `./gradlew.bat clean :app:assembleDebug`.
- [ ] Confirm there are no `RowColumnParentData?.weight` access errors.
- [ ] Install the generated debug APK.

## Smoke test

- [ ] Premium Home screen renders correctly.
- [ ] Bottom navigation switches between Home, Touchpad, Keyboard, and Settings.
- [ ] Steam and GOG feature tiles render at full width.
- [ ] All six compact launcher tiles render in the approved order.
- [ ] Command Deck buttons render and respond.
- [ ] Touchpad sensitivity row and mouse-button row render correctly.
- [ ] Keyboard shortcut and key rows render correctly.
- [ ] Settings rows and pairing controls render correctly.
