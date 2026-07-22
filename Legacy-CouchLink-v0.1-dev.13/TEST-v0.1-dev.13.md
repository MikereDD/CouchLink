# CouchLink v0.1-dev.13 Test Checklist

## Build identity

- [ ] Android `assembleDebug` succeeds.
- [ ] Android About reports `0.1-dev.13` / versionCode `29`.
- [ ] Windows WPF host builds successfully.
- [ ] Windows About reports host version `0.1-dev.13`.
- [ ] Boot Service heartbeat reports `0.1-dev.13`.
- [ ] Protocol remains version `1` on both platforms.

## Launcher behavior

For Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar Games Launcher, Epic Games Launcher, and Amazon Games:

- [ ] A closed launcher opens.
- [ ] A running launcher is focused instead of duplicated.
- [ ] Android displays the correct result status.
- [ ] The session remains connected after repeated launcher commands.

## Input regression

- [ ] Pointer movement and clicks work.
- [ ] Drag lock and two-finger scrolling work.
- [ ] Text and essential keyboard keys work.
- [ ] Command Deck shortcuts work.

## Pre-login regression

- [ ] Locking Windows moves Android from `45821` to `45822`.
- [ ] Lock-screen pointer and keyboard input work.
- [ ] PIN entry is accepted by Windows.
- [ ] Android receives the desktop-available handoff.
- [ ] Android reconnects persistently to `45821`.
- [ ] No reconnect storm or heartbeat-failure loop occurs.

## Documentation

- [ ] Root, Android, Windows, and Virtual HID READMEs render correctly in Forgejo.
- [ ] Changelog entry accurately matches tested behavior.
- [ ] Documentation links resolve from their respective directories.
