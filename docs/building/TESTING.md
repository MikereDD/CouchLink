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
