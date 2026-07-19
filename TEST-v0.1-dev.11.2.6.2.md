# CouchLink v0.1-dev.11.2.6.2 Test Checklist

## Build

- Windows solution builds successfully.
- Android `assembleDebug` reports version `0.1-dev.11.2.6.2` / versionCode `25`.
- Boot Service installs, runs automatically, and reports `PreLoginBroker: Listening`.

## Secure-screen pointer

1. Reboot Netzach and remain at the Windows sign-in screen.
2. Open CouchLink on Android and wait for the trusted pre-login connection.
3. Open Touchpad and move one finger slowly, then quickly.
4. Confirm the Windows pointer visibly follows movement in every direction.
5. Confirm left click opens the sign-in panel and can select Accessibility.
6. Open Windows On-Screen Keyboard and enter the PIN using pointer clicks.
7. Confirm Android remains open through login and reconnects to the desktop host.
8. Background and reopen Android; Steam Big Picture must not relaunch.

## Regression

- Normal desktop touchpad movement, clicks, and scrolling still work.
- No Android `Broken pipe` crash during lock/login handoff.
