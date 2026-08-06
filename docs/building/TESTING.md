# CouchLink 1.1 Integration Test Checklist

## Windows Launcher Host

- Build the full solution in Release configuration.
- Start the WPF host and confirm it advertises UDP `45820` and listens on TCP `45821`.
- Confirm the dashboard shows the expected local IP, host version `1.1`, protocol `1`, and host state.
- Confirm tray behavior and startup preferences work correctly.
- Confirm closing and reopening the dashboard does not duplicate the runtime or listener.

## Android host connection

- Confirm About/package metadata reports Android `1.1` and version code `106`.
- Discover the Windows host on the same trusted LAN.
- Verify discovery alone does not begin pairing with an untrusted host.
- Start pairing manually and enter the six-digit code shown by the host.
- Confirm one trusted-device record is created.
- Restart both apps and verify automatic trusted reconnect.
- Rotate Android and move the app between foreground and background; confirm the active host session survives.
- Close the host and confirm Android reports only the Launcher Host offline.
- Restart the host and confirm reconnect without another pairing code.
- Interrupt Wi-Fi briefly and confirm reconnect backoff and recovery.
- Use **Forget Host**, then confirm reconnect requires a fresh pairing code.

## Bluetooth HID

- Pair Android with Windows as a Bluetooth input device.
- Verify touchpad movement, left/right click, scrolling, drag lock, and keyboard entry.
- Verify media, navigation, and Windows shortcut controls.
- Verify Bluetooth input works at the Windows sign-in screen.
- Verify Bluetooth input remains functional when the Windows host is closed.
- Verify the app distinguishes Bluetooth HID state from Launcher Host state.
- Confirm a connected HID session reports **Bluetooth HID profile active.**

## Launchers

For Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar Games Launcher, Epic Games Launcher, and Amazon Games:

- launch while closed;
- focus while already running;
- close where supported;
- verify Android state/result feedback;
- force a host-send failure and confirm Bluetooth fallback occurs once;
- confirm repeated taps do not create duplicate launcher processes where focus is supported.

## Reliability and battery

- Leave CouchLink connected for several hours with the Android screen off.
- Confirm screen-off battery usage remains reasonable and no repeated pairing prompt appears.
- Restart the Android activity and confirm no duplicate discovery or TCP loops become visible.
- Restart the Windows host repeatedly and confirm old socket jobs do not close the newest session.
- Confirm heartbeat stops after a failed connection and reconnect resumes cleanly.

## Release hygiene

- Confirm no signing keys, passwords, pairing tokens, local logs, generated binaries, or machine-specific paths are present in source control.
- Confirm LICENSE, NOTICE, PRIVACY.md, SECURITY.md, CONTRIBUTING.md, and TRADEMARKS.md are included.
- Confirm release notes, installation guide, changelog, and version badges all report `1.1`.


## Wake PC pre-RC validation

- Upgrade Android to `1.3.1-dev.8` (`versionCode 157`) and Windows Host to `1.3.1-dev.8`.
- Connect the trusted Host once while awake.
- Confirm Settings shows the wired Wake-on-LAN MAC address.
- Put the PC into settled S3 sleep and confirm Home changes from Host connected to **WAKE PC**.
- Tap **WAKE PC** and confirm repeated packets do not freeze the Android UI.
- Confirm the PC wakes and CouchLink automatically discovers and authenticates the Host.
- Confirm launcher states and Audio Output repopulate after reconnect.
- Repeat three complete sleep/wake cycles.
- Cold-start Android while the PC is asleep and confirm the remembered trusted Host still offers **WAKE PC**.
- Use **Forget Host** and confirm the saved wake identity and Wake PC action disappear with the trust token.
- Close only the Windows Host while leaving the PC awake; confirm Wake PC reports a timeout rather than falsely claiming the Host reconnected.
- Verify Bluetooth HID remains usable throughout Host sleep, wake, and reconnect transitions.

## TV input selector regression

- Install Android `1.3.1-dev.8.1` (`versionCode 158`).
- Open **TV Remote → Input** without selecting an input first.
- Confirm HDMI 1, HDMI 2, and HDMI 3 all show the neutral subtitle **HDMI input**.
- Confirm no HDMI row is pre-highlighted.
- Confirm the removed Fire TV Stick is not named anywhere in the selector.
- Select each HDMI input and confirm the established HW4, HW5, and HW6 switching mappings still work.
- Wake the Windows PC and allow HDMI-CEC to activate its source; reopen the selector and confirm CouchLink does not invent or preselect a live HDMI state.
