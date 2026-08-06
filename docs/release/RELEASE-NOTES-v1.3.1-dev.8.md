# CouchLink v1.3.1-dev.8

Wake PC pre-RC feature build.

## New

- A trusted offline Windows Host can expose **WAKE PC** on the Android Home screen.
- Android remembers the trusted PC name, endpoint, and wired Ethernet MAC after an authenticated session.
- Wake-on-LAN sends repeated magic packets over the local network and waits up to 45 seconds for normal CouchLink discovery and trusted reconnection.
- The Windows Host now prefers an active wired Ethernet adapter when advertising its primary network identity.
- Settings diagnostics show the saved Windows wake identity and current waking state.

## Supported scope

This build claims wired Ethernet wake from settled classic S3 sleep on the same LAN. Hybrid Sleep and automatic hibernation should be disabled. Shutdown, hibernation, Wi-Fi wake, and routed wake are not yet claimed.

## Version

- Android: `1.3.1-dev.8` (`versionCode 157`)
- Windows Host: `1.3.1-dev.8`
