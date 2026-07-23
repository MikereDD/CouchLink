# v0.2-dev.8 hard cleanup

- Removed the obsolete LAN discovery/session client and all Windows Host pairing state.
- Removed the INTERNET permission.
- Removed Host, protocol, LAN, Wake-on-LAN, pre-login, and pairing UI/code paths.
- Made all input and launcher routing explicitly Bluetooth HID-only.
- Simplified status states to connected, reconnecting, ready, and offline.
- Simplified Bluetooth device selection to the paired-device list.
- Updated About, privacy text, and diagnostics for the real architecture.
- Raised minSdk from 26 to 28 because Android Bluetooth HID Device is the core feature.
- Marked Bluetooth hardware as required.
- Removed stale Windows Host comments and `connectBestMatch`.
- Replaced the obsolete README.
