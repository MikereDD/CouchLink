# CouchLink v0.1-dev.15.3

## Scope

This build restores full Windows Launcher Host management to the Android Settings screen while preserving Bluetooth HID as the independent input path.

## Test focus

1. Open **Settings → Windows Launcher Host**.
2. Confirm PC name, endpoint, host version, and connection status.
3. Stop the Windows host and use **FIND HOST** after restarting it.
4. Use **FORGET HOST**, enable pairing on Windows, and confirm the six-digit pairing flow appears again.
5. Confirm touchpad and keyboard continue working over Bluetooth HID while the launcher host is offline.
