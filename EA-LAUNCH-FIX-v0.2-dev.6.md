# CouchLink v0.2-dev.6 — EA launcher fix

The Bluetooth-only EA launcher now uses Windows Run with the verified installed executable:

`%ProgramFiles%\Electronic Arts\EA Desktop\EA Desktop\EADesktop.exe`

This bypasses Windows Start Search, which selected the EA updater instead of the EA desktop launcher.

Steam Big Picture and the other Bluetooth launcher fallbacks are unchanged.
