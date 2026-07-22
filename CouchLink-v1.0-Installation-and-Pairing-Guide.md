# CouchLink v1.0 — Installation and Pairing Guide

## Install and verify

1. Put both files in the same folder:

```text
CouchLink-v1.0.apk
CouchLink-v1.0.apk.sha256
```

2. Optional but recommended: verify the APK checksum in PowerShell:

```powershell
Get-FileHash .\CouchLink-v1.0.apk -Algorithm SHA256
```

Compare the displayed hash with the one inside:

```powershell
Get-Content .\CouchLink-v1.0.apk.sha256
```

3. Install the APK on the Android phone.

Android may ask to allow installation from the browser or file manager being used. Enable that permission temporarily, install CouchLink, then disable it afterward if desired.

## Pair CouchLink with Windows

1. On the Windows PC, open:

```text
Settings → Bluetooth & devices
```

2. Make sure Bluetooth is turned on.

3. On the Android phone, open CouchLink.

4. Grant the Bluetooth permissions CouchLink requests.

5. Tap the Bluetooth connection panel at the top of CouchLink, then choose the pairing or Windows pairing option.

6. CouchLink should make the phone discoverable as a Bluetooth input device.

7. On Windows, click:

```text
Add device → Bluetooth
```

8. Select the phone or CouchLink device when it appears.

9. Accept any pairing confirmation shown on both Windows and Android.

10. Return to CouchLink. The top panel should eventually show:

```text
BLUETOOTH INPUT CONNECTED
```

## Test it

Once connected:

- Open the **Touchpad** tab and move a finger around.
- Test left-click and scrolling.
- Open the **Keyboard** tab and type into Notepad.
- From the Home screen, test a launcher such as Steam.

The launcher buttons send keyboard commands to Windows, so the PC must be unlocked and accepting keyboard input.

## If it does not connect

On both devices, remove the old pairing and pair again.

### Windows

```text
Settings → Bluetooth & devices → Devices
```

Find the phone or CouchLink entry, open its menu, and choose **Remove device**.

### Android

```text
Settings → Connected devices → Previously connected devices
```

Forget the Windows PC.

Then restart Bluetooth on both devices, reopen CouchLink, and repeat the pairing process.

Also make sure CouchLink has permission for:

```text
Nearby devices
Bluetooth connection
Bluetooth advertising/discoverability
```

The advertising permission may only be requested when the pairing button is used.
