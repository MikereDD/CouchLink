# CouchLink v1.3.3

CouchLink v1.3.3 is a stable patch release that adds the Android Launcher Tray and fixes Windows Host network-interface persistence so launcher discovery, Wake-on-LAN, and post-wake reconnect remain reliable without reopening the Host to reselect an interface.

## Highlights

- **Android Launcher Tray:** a compact Home card opens a CouchLink-styled launcher sheet for Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar Games Launcher, Epic Games Launcher, and Amazon Games.
- **Host-only launcher execution:** launcher actions now require the paired Windows Launcher Host and no longer fall back to Bluetooth HID / Windows Run behavior.
- **Persistent Host interface:** the Windows Host saves the adapter chosen with **Use Interface** and restores it automatically after a Host restart.
- **Safe network fallback:** if the saved or active adapter is unavailable, the Host selects another eligible physical LAN interface instead of leaving discovery stopped until manual intervention.
- **Wake/reconnect verified:** Wake-on-LAN and automatic Launcher Host reconnect were exercised after the persistence fix.
- **No launcher protocol change:** protocol v1, pairing/trust identity, updater security, Audio Output, Bluetooth HID keyboard/mouse input, and the existing Google TV Remote path remain compatible.

## Version

- Android: `1.3.3` (`versionCode 162`)
- Windows Host: `1.3.3`
- Shared launcher protocol: `1`

## Release status

Stable candidate. Launcher Tray behavior, interface persistence across Host restarts, wired/Wi-Fi selection persistence, Wake-on-LAN, and automatic mobile reconnect were exercised on real hardware before release preparation. Final signed-build, checksum, detached-signature, source-manifest, exact-tag, and in-place update gates remain required before publication.
