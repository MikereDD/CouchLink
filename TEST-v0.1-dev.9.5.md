# CouchLink v0.1-dev.9.5.1 — Trusted Device Management Test

## Android build

The Android protocol and UI are unchanged from v0.1-dev.9.4. The already-installed APK is suitable for this test. Fully close and reopen CouchLink Remote after starting the new Windows host.

## Pairing recovery

1. Start CouchLink Host v0.1-dev.9.5.1.
2. Confirm Trusted Devices shows 0 after the previous reset.
3. Tap **Pair New Device** on Windows.
4. Fully close and reopen CouchLink Remote on Android.
5. Confirm a six-digit code appears in the Windows Pairing panel.
6. Enter the code on Android and complete pairing.
7. Confirm the Pixel appears in the Windows Trusted Devices list.

## Device revocation

1. Select the Pixel in the Trusted Devices list.
2. Choose **Revoke Selected** and confirm.
3. Confirm the Android session disconnects.
4. Confirm Trusted Devices returns to 0.
5. Re-pair the phone.
6. Choose **Revoke All** and confirm the same result.

## Regression

- Minimize keeps the host in the tray.
- X exits the host.
- Tray Exit fully terminates it.
- Trusted reconnection works after pairing.
- Mouse, keyboard, launchers, and Command Deck remain functional.
