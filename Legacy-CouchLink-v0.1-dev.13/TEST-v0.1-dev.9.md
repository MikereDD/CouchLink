# CouchLink v0.1-dev.9 — Startup & Tray Test

1. Build and launch the Windows host.
2. Minimize: the window should disappear to the system tray while the Android session remains connected.
3. Double-click the tray icon to restore the dashboard.
4. Right-click the tray icon and test Open CouchLink, Toggle Remote Input, and Launch Steam Big Picture.
5. Close the dashboard with X; it should remain running in the tray.
6. Use Exit from the tray to fully stop CouchLink.
7. Run `tools\Enable-CouchLinkStartup.ps1`, sign out/in or inspect the HKCU Run entry, and confirm it starts minimized. Use `Disable-CouchLinkStartup.ps1` afterward if desired.
8. Regression-test Android Home, Touchpad, Keyboard, launcher tiles, and reconnect.
