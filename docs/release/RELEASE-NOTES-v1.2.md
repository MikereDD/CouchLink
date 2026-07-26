# CouchLink 1.2 Release Notes

CouchLink 1.2 promotes the complete Google TV Remote experience to stable while preserving the Bluetooth HID and Windows Launcher Host features from 1.1.

## Highlights

- Secure Google TV Remote v2 certificate pairing and local TLS control.
- Premium TV Remote interface with custom CouchLink icons and unified D-pad.
- Power off through the TV remote protocol.
- Wake-on-LAN power on with automatic reconnect.
- Home, Back, Settings, D-pad, OK, volume, mute, channel, and playback controls.
- One-minute backward and forward playback skips.
- Google TV Home Live-tab navigation without opening the antenna tuner.
- Premium input selector with verified Hisense A6H mappings for HDMI 1–3, composite, and TV/antenna.
- Haptic feedback, clear connection states, and accidental Power double-tap protection.
- Installed Android label standardized as `CouchLink` with the metallic CL icon.
- Updated README showcase covering Home, Touchpad, Keyboard, and TV Remote.

## Compatibility

Standard Google TV Remote v2 controls should work across compatible Google TV and Android TV devices that expose the same pairing and control services.

Direct hardware-input switching is model and firmware dependent. CouchLink 1.2 has been validated on a Hisense A6H Google TV with these mappings:

- HDMI 1: `HW4`
- HDMI 2: `HW5`
- HDMI 3: `HW6`
- Composite: `HW1`
- TV/Antenna: `HW0`

Wake-on-LAN also depends on TV hardware, firmware, standby mode, and network configuration.

## Stable Android metadata

```text
versionName 1.2
versionCode 120
applicationId dev.typezero.couchlink.remote
installed label CouchLink
```
