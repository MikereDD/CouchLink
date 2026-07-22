# CouchLink Remote v0.2-dev.10 Test Checklist

## Build and install

- [ ] Open the project with JDK 17.
- [ ] Sync Gradle successfully.
- [ ] Build the `app` Debug configuration.
- [ ] Install over v0.2-dev.9 without clearing app data.
- [ ] Confirm Settings reports version `0.2-dev.10` and version code `39`.
- [ ] Confirm haptics and natural-scrolling preferences remain unchanged after upgrade.

## Premium UI regression

- [ ] Header shows the CL mark, CouchLink wordmark, and Remote Ready/Offline capsule without clipping.
- [ ] Bluetooth connection panel expands and collapses correctly.
- [ ] Steam remains the dominant cyan full-width launcher.
- [ ] GOG Galaxy remains the violet second full-width launcher.
- [ ] Launcher order remains Xbox, EA app, Ubisoft / Rockstar, Epic, Amazon.
- [ ] Command Deck remains Alt-Tab, Task, Show, Close.
- [ ] Bottom navigation remains fixed while screen content scrolls.
- [ ] No labels or controls use visibly thin/light typography.
- [ ] Home, Keyboard, Touchpad, and Settings retain the approved metallic treatment.

## Bluetooth HID lifecycle

- [ ] Permission denial leaves Settings usable and shows the permission-required state.
- [ ] Granting permission starts HID registration without restarting the app.
- [ ] Returning to CouchLink after enabling Bluetooth refreshes the HID state.
- [ ] Pairing and connecting to the Windows PC succeeds.
- [ ] Backgrounding and reopening CouchLink does not create duplicate connection attempts.
- [ ] Automatic reconnect still works after the Windows PC restarts.
- [ ] Pressing Disconnect shows a disconnected state and does not say Reconnecting.
- [ ] A rejected connection attempt exits the Connecting state.

## Input regression

- [ ] Touchpad movement works.
- [ ] Single tap sends left click.
- [ ] Double tap sends double click.
- [ ] Long press sends right click.
- [ ] Two-finger scrolling works in the selected natural-scrolling direction.
- [ ] Drag lock releases when leaving Touchpad.
- [ ] Drag lock releases if Bluetooth disconnects.
- [ ] Keyboard text sends and clears only after a successful send.
- [ ] Escape, Tab, Backspace, Enter, and arrow keys work.
- [ ] Alt-Tab, Show Desktop, Task Manager, and Close Window work.

## Launcher regression

- [ ] Steam opens Big Picture.
- [ ] EA app launches through the explicit executable command.
- [ ] GOG Galaxy opens through Windows Start Search.
- [ ] Xbox opens through Windows Start Search.
- [ ] Ubisoft Connect opens through Windows Start Search.
- [ ] Rockstar Games Launcher opens through Windows Start Search.
- [ ] Epic Games Launcher opens through Windows Start Search.
- [ ] Amazon Games opens through Windows Start Search.
