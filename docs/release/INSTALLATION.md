# CouchLink 1.1 Installation and Pairing

## Requirements

### Android remote

- Android 9 or newer
- Bluetooth support capable of the Android Bluetooth HID device profile
- Wi-Fi or Ethernet access to the same trusted local network as the Windows PC for Launcher Host features

### Windows Launcher Host

- Windows 10 or Windows 11
- .NET 8 when using a framework-dependent build, or the self-contained published package
- Bluetooth support for direct Android HID pairing

## Install the Android remote

1. Install the signed CouchLink Android package.
2. Open CouchLink and grant the Bluetooth permissions requested by Android.
3. Use CouchLink's Bluetooth setup to pair the phone with Windows as an input device.
4. Confirm touchpad movement and keyboard input before configuring the optional host.

## Install the Windows Launcher Host

1. Place the official `CouchLink-Host-v1.1-win-x64.exe` in a permanent folder and run it.
2. Start CouchLink Host. The default package requires the .NET 8 Desktop Runtime.
3. Allow local-network access through Windows Firewall when prompted.
4. Optionally enable **Start with Windows** from the host preferences.

## Pair the Launcher Host

1. Place Android and Windows on the same trusted local network.
2. In CouchLink Android, open the Windows Launcher Host section and choose **Reconnect / Find Host**.
3. Select the discovered PC.
4. Enter the six-digit pairing code displayed by the Windows host.
5. Confirm the host reports the Android device as trusted and Android reports the host as connected.

The trusted token is retained locally for automatic reconnect. Use **Forget Host** to remove the Android-side trust record.

## Verify fallback behavior

1. Close the Windows host.
2. Confirm Android reports only the Launcher Host as offline.
3. Confirm Bluetooth keyboard, touchpad, shortcuts, and sign-in input continue to work.
4. Tap a launcher tile and confirm the configured Bluetooth fallback is attempted.

## Network ports

- UDP `45820`: local host discovery
- TCP `45821`: trusted pairing, heartbeat, launcher commands, and state results

These endpoints are intended only for trusted local networks and should not be exposed directly to the public internet.
