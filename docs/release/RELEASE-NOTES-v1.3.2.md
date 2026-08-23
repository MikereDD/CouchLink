# CouchLink v1.3.2

CouchLink v1.3.2 is a stable patch release focused on reliable Launcher Host discovery across real physical LAN interfaces and integrating the new interface selector into CouchLink's existing Windows Host design.

## Highlights

- **Selectable advertised network interface** for the Windows Host, limited to eligible physical Ethernet and Wi-Fi adapters.
- **Safe startup behavior:** when no eligible interface exists, the Host waits and automatically selects the first eligible interface when one appears.
- **Safe loss behavior:** when the selected interface disappears, CouchLink stops advertising and requires explicit manual reselection instead of silently switching to another adapter.
- **Selected-interface discovery:** UDP advertisement uses the selected interface address and broadcast target; the TCP session listener remains on `IPAddress.Any:45821`.
- **Android discovery reliability:** the Android client holds a non-reference-counted Wi-Fi multicast lock while Launcher Host discovery is active and releases it when discovery stops.
- **CouchLink-native Windows styling:** the interface selector uses the existing CouchLink ComboBox treatment, the action uses the existing accent button, and selected adapters render by friendly display name.
- **No protocol change:** launcher protocol v1, pairing/trust identity, Wake-on-LAN semantics, Audio Output, Bluetooth HID, and Google TV Remote behavior remain unchanged.

## Version

- Android: `1.3.2` (`versionCode 161`)
- Windows Host: `1.3.2`
- Shared launcher protocol: `1`

## Release status

Stable. The networking behavior was exercised on real Ethernet/Wi-Fi hardware during the pull-request review cycle, including interface selection and interface-loss/recovery cases. The final release still requires the normal signed-build, checksum, detached-signature, source-manifest, and in-place updater verification gates before publication.
