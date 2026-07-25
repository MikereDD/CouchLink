# TV Remote wire diagnostics — v1.2-dev.3.3

This debug build is labeled **CouchLink TV Diagnostic** and uses version code 110.

The TV Remote screen always shows `DIAGNOSTIC v1.2-dev.3.3` plus the latest RX/TX frame.

The persistent trace can be read with:

```powershell
adb shell run-as dev.typezero.couchlink.remote.debug cat files/couchlink-tv-wire.txt
```
