# CouchLink v0.1-dev.15.6 Build Notes

## Scope

Focused Android launcher-host reliability pass. Windows Host remains 0.2-dev.1.

## Changes

- Process-wide host session survives Activity recreation.
- Failed host launcher writes invoke Bluetooth HID fallback for the original tap.
- Output-stream absence and session replacement are explicit send failures.
- Pairing send failures return actionable UI feedback.
- Each connection job owns and closes only its own socket resources.
- Discovery timeout no longer replaces useful discovered/disconnected copy.

## Versions

- Combined source: 0.1-dev.15.6
- Android: 1.1-dev.5 (105)
- Windows Host: 0.2-dev.1
