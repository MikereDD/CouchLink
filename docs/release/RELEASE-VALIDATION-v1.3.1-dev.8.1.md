# CouchLink v1.3.1-dev.8.1 Validation Record

## Reason for build

The TV input selector contained test-era presentation data: HDMI 3 was permanently highlighted and labeled as a Fire TV Stick even after that device was unplugged. The highlight was not based on live TV input telemetry.

## Build validation

- [ ] Version tests
- [ ] Android signed APK
- [ ] Android signing certificate
- [ ] Windows Host and Updater
- [ ] Windows detached signatures
- [ ] Source manifest

## Live TV validation

- [ ] All HDMI rows use **HDMI input**
- [ ] No default HDMI highlight
- [ ] No stale Fire TV Stick name
- [ ] HDMI 1 / HW4 switching
- [ ] HDMI 2 / HW5 switching
- [ ] HDMI 3 / HW6 switching
- [ ] PC wake and HDMI-CEC do not create a false CouchLink selection
- [ ] Wake PC remains functional

## Final status

Pending build and live Android-to-TV validation.
