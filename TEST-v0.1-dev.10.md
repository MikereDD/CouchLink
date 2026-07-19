# CouchLink v0.1-dev.10 Test Checklist

## Build
- Windows solution builds without warnings or errors.
- Android debug APK builds and installs over v0.1-dev.9.7.

## Windows startup settings
- Toggle Start CouchLink with Windows and confirm HKCU Run entry changes.
- Toggle Start minimized to tray and confirm the saved setting survives restart.
- Toggle Launch Steam Big Picture after connection and confirm the setting persists.

## Android settings
- Settings appears as the fourth bottom-navigation destination.
- Haptics and Natural scrolling switches persist after app restart.
- Existing Home, Touchpad, Keyboard, launcher, and Command Deck controls remain functional.

## Wake-on-LAN
- Settings shows the host adapter MAC address.
- Put Netzach to sleep, tap WAKE PC, and confirm it wakes and CouchLink reconnects.
- If the hardware/firmware does not support WOL, confirm the app reports the limitation cleanly.

## Regression
- Pairing, trusted reconnect, revocation, tray operation, mouse, keyboard, Steam Big Picture, and all launcher buttons still work.
