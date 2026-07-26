# CouchLink v1.2-dev.2 TV Pairing Test

## Goal

Pair CouchLink directly with the Hisense Google TV using Android TV Remote v2. This build does not yet expose the final remote-control buttons.

## Test

1. Build and install the debug APK.
2. Open **TV Remote** and select Abaddon.
3. Tap **Test connection** and confirm ports 6467 and 6466 are reachable.
4. Tap **Pair with TV**.
5. Confirm the TV displays a six-character hexadecimal code.
6. Enter the code in CouchLink and tap **Pair TV**.
7. Confirm CouchLink reports **PAIRED**.
8. Close and reopen CouchLink and confirm the selected TV remains marked paired.

## Report

Record whether the code appeared, the exact error shown if pairing failed, and whether pairing persisted after restarting the app. Never include private keys or application signing passwords.
