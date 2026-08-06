# Wake PC setup and validation

CouchLink `1.3.1-dev.8` adds a local **Wake PC** action for an already trusted Windows Launcher Host.

## Supported first-release path

- Android and the PC are on the same local network.
- The PC uses wired Ethernet.
- The PC enters classic S3 sleep.
- The Ethernet adapter remains powered and armed for Wake-on-LAN.
- Hybrid Sleep is disabled.
- Automatic hibernation while plugged in is disabled.

Wake from shutdown, hibernation, Wi-Fi, a different subnet, or through an internet relay is not claimed.

## Windows preparation

On the Windows PC, confirm the wired adapter appears in both lists:

```powershell
powercfg /devicequery wake_programmable
powercfg /devicequery wake_armed
```

Inspect adapter power settings:

```powershell
Get-NetAdapter
Get-NetAdapterPowerManagement
```

The wired adapter should report `WakeOnMagicPacket` as enabled.

For the proven S3 path, use an elevated PowerShell window:

```powershell
powercfg /setacvalueindex SCHEME_CURRENT SUB_SLEEP HYBRIDSLEEP 0
powercfg /setacvalueindex SCHEME_CURRENT SUB_SLEEP HIBERNATEIDLE 0
powercfg /setactive SCHEME_CURRENT
```

The motherboard firmware must also keep PCIe/Ethernet wake enabled and must not cut standby power through an ErP/deep-power-off setting.

## First CouchLink use

1. Start the updated Windows Host and Android app while the PC is awake.
2. Let the existing trusted session reconnect.
3. Open Android **Settings → Windows Launcher Host**.
4. Confirm **Wake-on-LAN** shows a six-byte MAC address.
5. This first trusted connection stores the wake identity locally on Android.

CouchLink deliberately does not persist a MAC address merely from an untrusted discovery advertisement.

## End-to-end test

1. Keep Android connected to the same LAN.
2. Put the PC to sleep with **Start → Power → Sleep**.
3. Wait at least 30 seconds for S3 to settle.
4. On CouchLink Home, confirm the Host card shows **WAKE PC**.
5. Tap it once.
6. Confirm the status progresses through:
   - `Sending Wake-on-LAN…`
   - `Wake signal sent. Waiting…`
   - `PC responded. Reconnecting CouchLink Host…`
   - `Launcher host connected…`
7. Confirm the PC wakes and launcher/audio state returns without pairing again.
8. Repeat the complete sleep/wake cycle at least three times.

## Packet behavior

CouchLink sends the standard 102-byte magic packet:

- six `FF` bytes;
- the target MAC repeated sixteen times.

It sends ten bursts at 500 ms intervals to UDP ports 9 and 7 using both the limited broadcast address and active interface-specific broadcast addresses.

## Failure interpretation

- **No Wake PC button:** connect the updated Host once while awake so Android can save the trusted wired identity.
- **Wake signal sent, no power-up:** verify BIOS/UEFI PCIe wake, ErP/deep sleep, adapter standby power, Hybrid Sleep, and hibernation settings.
- **PC wakes but Host does not reconnect:** confirm CouchLink Host starts with Windows and is allowed through the local firewall.
- **Host is merely closed while Windows is awake:** Wake-on-LAN cannot start the application; reopen the Host normally.
