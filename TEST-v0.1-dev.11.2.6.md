# CouchLink v0.1-dev.11.2.6.2 Test Checklist

## Build
- Windows solution builds successfully.
- Android `assembleDebug` succeeds and reports version `0.1-dev.11.2.6.2` / versionCode `23`.

## Pre-login
- Reboot and remain at Windows sign-in.
- Android connects without manually enabling the desktop host.
- Status identifies port `45822`.
- Touchpad movement visibly moves the Windows pointer.
- Left click opens Accessibility and Windows On-Screen Keyboard.
- OSK can enter the PIN and complete sign-in.

## Handoff
- Android stays open through sign-in.
- It reconnects to port `45821` without crashing.
- Touchpad remains functional after handoff.
- Steam Big Picture launches once only and does not relaunch after Android background/resume.
