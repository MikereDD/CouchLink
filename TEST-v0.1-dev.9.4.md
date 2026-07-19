# CouchLink v0.1-dev.9.4 Test

## Fresh pairing
1. Uninstall CouchLink Remote or clear its storage.
2. Reinstall and open the Android app.
3. Confirm the Windows host displays a new six-digit pairing code.
4. Confirm Android displays the pairing-entry panel.
5. Pair and verify the persistent session reconnects.

## Exit behavior
1. Click minimize: the host should stay active in the tray.
2. Restore from the tray.
3. Click X: CouchLink should fully exit and Android should disconnect.
4. Relaunch, then choose Exit CouchLink from the tray menu: the process should stop.
5. Confirm no CouchLink.Host process remains with `Get-Process CouchLink.Host -ErrorAction SilentlyContinue`.
