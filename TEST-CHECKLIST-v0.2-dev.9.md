# CouchLink Remote v0.2-dev.9 Test Checklist

## Build and launch

- [ ] Open the project with JDK 17.
- [ ] Sync Gradle successfully.
- [ ] Build `app` as Debug.
- [ ] Install over v0.2-dev.8 without losing preferences.
- [ ] Confirm Settings reports version `0.2-dev.9` and version code `38`.

## Premium Home UI

- [ ] Header shows the CL mark, CouchLink wordmark, and Remote Ready/Offline capsule without clipping.
- [ ] Bluetooth connection bar is readable and expands/collapses when tapped.
- [ ] Steam is the dominant full-width tile with cyan treatment.
- [ ] GOG Galaxy is the second full-width tile with violet treatment.
- [ ] Launcher grid order is Xbox, EA app, Ubisoft / Rockstar, Epic, Amazon.
- [ ] Launcher logos are centered and not cropped.
- [ ] Command Deck shows Alt-Tab, Task, Show, and Close.
- [ ] Bottom navigation remains fixed while the Home content scrolls.
- [ ] Selected bottom navigation destination has the raised metallic treatment.
- [ ] Layout remains usable in portrait at the device's normal display size and font scale.

## Functional regression

- [ ] Bluetooth HID reconnects to the paired Windows PC.
- [ ] Steam opens Big Picture.
- [ ] EA app launches.
- [ ] GOG, Xbox, Ubisoft, Rockstar, Epic, and Amazon launch through Windows search fallback.
- [ ] Alt-Tab works.
- [ ] Task Manager opens.
- [ ] Show Desktop works.
- [ ] Close Window sends the expected shortcut.
- [ ] Touchpad movement, tap, right-click, drag lock, and two-finger scrolling still work.
- [ ] Keyboard text and essential keys still work.
- [ ] Settings controls and diagnostics still work.
