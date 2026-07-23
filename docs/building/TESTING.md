# CouchLink Integration Test Checklist

## Windows host

- Build the full solution in Debug.
- Start the WPF host and confirm it advertises UDP `45820` and listens on TCP `45821`.
- Confirm the dashboard shows the expected local IP and host state.
- Confirm tray and startup preferences behave correctly.

## Android host connection

- Discover the Windows host on the same LAN.
- Pair using the six-digit code shown by the host.
- Confirm one trusted-device record is created.
- Restart both apps and verify automatic trusted reconnect.
- Close the host and confirm Android reports only the launcher host offline.
- Restart the host and confirm reconnect without another pairing code.

## Bluetooth HID

- Pair Android with Windows as a Bluetooth input device.
- Verify touchpad movement, left/right click, scrolling, drag lock, and keyboard entry.
- Verify Bluetooth input remains functional when the Windows host is closed.
- Verify the app distinguishes Bluetooth HID state from launcher-host state.
- If Android rejects stale HID registration, force-stop CouchLink, toggle Bluetooth, reopen CouchLink, and confirm recovery. This recovery should be automated in a future build.

## Launchers

Test launch-or-focus for Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar, Epic, and Amazon Games. Confirm state/result feedback and Bluetooth fallback when the host is unavailable.

## Known cleanup item

When Bluetooth is already connected, the status copy may continue to show `Opening Android Bluetooth HID profile...`. Replace it with connected-state text in the next runtime cleanup pass.
