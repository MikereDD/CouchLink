# CouchLink v1.3.1-dev.8 Release Checklist

- [ ] Windows version-comparison tests pass, including `dev.8 > dev.7.1`.
- [ ] Source manifest verifies.
- [ ] Android signed APK builds as versionCode 157.
- [ ] Android signing certificate matches the canonical CouchLink certificate.
- [ ] Windows Host and Updater publish successfully.
- [ ] Windows detached signatures and tamper tests pass.
- [ ] Updated Host advertises a normalized wired MAC address.
- [ ] Android stores the MAC only after a trusted session.
- [ ] Settings displays the trusted PC wake identity.
- [ ] PC reaches settled classic S3 sleep.
- [ ] Wake PC works for three consecutive complete sleep/wake cycles.
- [ ] Cold-starting Android while the PC sleeps still presents Wake PC.
- [ ] Android automatically rediscovers and reconnects after wake.
- [ ] Launcher state and Audio Output repopulate.
- [ ] Bluetooth HID remains available while the Host is asleep.
- [ ] Forget Host removes the wake identity and action.
- [ ] Timeout messaging is accurate when Windows is awake but Host is closed.
- [ ] Existing TV Wake-on-LAN remains unaffected.
