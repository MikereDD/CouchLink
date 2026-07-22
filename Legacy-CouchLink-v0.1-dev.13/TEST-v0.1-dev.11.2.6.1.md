# CouchLink v0.1-dev.11.2.6.2 Test Checklist

## Build

- Uninstall or stop the existing CouchLink Boot Service before building.
- `dotnet clean` succeeds.
- `dotnet restore` succeeds.
- `dotnet build` succeeds for all Windows projects.
- Android `assembleDebug` succeeds and reports version `0.1-dev.11.2.6.2` / versionCode `24`.

## Regression

- Boot Service installs and listens on TCP 45822.
- Android connects before login.
- Pre-login pointer movement and clicks work without crashing.
- Windows On-Screen Keyboard can be operated through CouchLink touchpad.
- Android reconnects to the desktop host after login.
- Steam Big Picture launches at most once per Windows host session.
