# CouchLink v0.2-dev.4

- Launcher tiles are enabled when either Bluetooth HID or Windows Host is connected.
- When Windows Host is connected, launchers use the existing exact Host command path.
- When only Bluetooth is connected, CouchLink opens Windows Start Search, types the launcher name, and presses Enter.
- Header status now distinguishes REMOTE READY, HOST READY, and FULL READY.
- Windows Host remains preferred for exact paths, status reporting, and advanced commands.
