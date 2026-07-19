# CouchLink v0.1-dev.3 Pairing Test

1. Start the Windows host and leave it open.
2. Install the new Android debug APK.
3. Tap **Request Pairing**.
4. Read the six-digit code shown in CouchLink Host.
5. Enter that code on Android and tap **Trust This Device**.
6. Confirm the phone reports that it is paired and the Windows dashboard shows one trusted device.
7. Close and reopen the Android app, rediscover the host, and tap **Verify Trusted Link**.
8. Confirm no new code is required and the trusted connection is accepted.

Trusted-device data is stored under `%LOCALAPPDATA%\\CouchLink\\trusted-devices.json` on Windows and in Android app-private preferences. Removing the Android app clears its token. Deleting the Windows trusted-device file clears host trust.
