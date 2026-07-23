# CouchLink v0.1-dev.15.4

## Scope

Android status-copy correction only. Windows launcher-host source and behavior are unchanged.

## Fix

Repeated Bluetooth HID controller startup calls could overwrite the successful connected message with `Opening Android Bluetooth HID profile…`. The controller now detects an existing connected or registered HID session and reports `Bluetooth HID profile active.` instead.

## Versions

- Combined working archive: 0.1-dev.15.4
- Android remote: 1.1-dev.3 (versionCode 103)
- Windows launcher host: 0.2-dev.1
