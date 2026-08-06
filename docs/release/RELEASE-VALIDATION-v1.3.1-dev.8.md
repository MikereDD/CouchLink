# CouchLink v1.3.1-dev.8 Validation Record

## Hardware proof before implementation

The target PC successfully woke from classic S3 sleep three consecutive times after:

- the Realtek wired Ethernet adapter was confirmed wake-programmable and wake-armed;
- Wake on Magic Packet was enabled;
- Hybrid Sleep was disabled;
- automatic hibernation while plugged in was disabled.

## Build validation

- [ ] Version tests
- [ ] Android signed APK
- [ ] Android signing certificate
- [ ] Windows Host and Updater
- [ ] Windows detached signatures
- [ ] Source manifest

## Live CouchLink validation

- [ ] Trusted Host wake identity stored
- [ ] Wake PC shown while offline
- [ ] Magic packet sent without UI stall
- [ ] First S3 wake and trusted reconnect
- [ ] Second S3 wake and trusted reconnect
- [ ] Third S3 wake and trusted reconnect
- [ ] Cold-start Android while PC sleeps
- [ ] Launcher state restored
- [ ] Audio Output restored
- [ ] Bluetooth HID unaffected
- [ ] Forget Host removes Wake PC
- [ ] Timeout path verified

## Final status

Pending build and live phone-to-PC validation.
