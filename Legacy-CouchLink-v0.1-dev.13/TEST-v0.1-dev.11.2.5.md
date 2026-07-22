# CouchLink v0.1-dev.11.2.6.2 Test Checklist

## Build

- Uninstall the previous Boot Service before cleaning or rebuilding its output folder.
- `dotnet clean`, `dotnet restore`, and `dotnet build` succeed under `src/windows`.
- Android `assembleDebug` succeeds and reports version `0.1-dev.11.2.6.2`.

## Pre-login touchpad stability

1. Reboot Netzach and remain at the Windows sign-in screen.
2. Open CouchLink Remote and wait for the trusted pre-login connection.
3. Open Touchpad and move the pointer continuously for at least 30 seconds.
4. Click Accessibility, enable Windows On-Screen Keyboard, and enter the PIN by clicking its keys.
5. Confirm the Android process does not crash or close.
6. Complete sign-in and continue moving/clicking during the broker-to-session-host handoff.
7. Confirm CouchLink reconnects and touchpad control resumes without restarting the Android app.

## Steam Big Picture trigger

1. Allow the first trusted post-login connection to launch Steam Big Picture.
2. Background and reopen CouchLink Remote several times.
3. Toggle Wi-Fi once and allow the app to reconnect.
4. Confirm Steam Big Picture is not relaunched by those reconnects.
5. Confirm the manual Steam Big Picture launcher action still works.

## Regression

- Boot Service reports Listening on TCP 45822 with no broker error.
- Remote Input and PreLoginControlEnabled remain true when previously enabled.
- Host starts minimized to tray without a ghost window.
- Pairing, keyboard, launchers, Wake-on-LAN, and trusted-device management remain operational.
