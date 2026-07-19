# CouchLink v0.1-dev.10.1 Test Checklist

## Headless startup

1. Enable **Start CouchLink with Windows** and **Start minimized to tray**.
2. Reboot or run `CouchLink.Host.exe --minimized`.
3. Confirm no dashboard, black rectangle, taskbar button, or Alt+Tab entry appears.
4. Confirm the CL tray icon appears and Android reconnects.
5. Confirm Steam Big Picture still launches after trusted connection when enabled.
6. Double-click the tray icon and confirm the dashboard is created and shown normally.
7. Minimize the dashboard and confirm it returns cleanly to tray.
8. Exit from the title-bar X and from the tray menu; verify the process stops.

## Android navigation

1. Confirm Home, Touchpad, Keyboard, and Settings each show an icon and single-line label.
2. Confirm no label wraps at the Pixel 10 Pro XL display scale.
3. Confirm selected navigation uses orange and inactive navigation uses silver-gray.
4. Regression-test launcher controls, touchpad, keyboard, settings, pairing, and reconnect.
