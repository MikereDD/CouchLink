# Audio Output — v1.3-dev.2

This polish build follows successful end-to-end testing of CouchLink audio-output switching.

## Changes

- Corrected the `IMMDeviceCollection` COM IID.
- Filters obvious capture and loopback labels from the normal playback picker.
- Uses compact Android labels for common driver suffixes.
- Preserves full endpoint names and stable endpoint IDs internally.

## Test

1. Start the v1.3-dev.2 Windows Host.
2. Connect the Android debug build.
3. Refresh Audio Output.
4. Confirm microphone, Line, and What U Hear entries are hidden.
5. Switch among at least three active outputs and verify the green current-output state follows.
