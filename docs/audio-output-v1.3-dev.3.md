# Audio Output — v1.3-dev.3

This naming-polish build follows successful end-to-end testing of CouchLink v1.3-dev.2.

## Changes

- Displays `Speakers (Realtek(R) Audio)` as `Speakers (Realtek)`.
- Keeps useful hardware identity for generic output names.
- Continues shortening bulky NVIDIA and SteelSeries driver suffixes.
- Preserves full endpoint names and stable endpoint IDs internally.
- Preserves native Windows Core Audio enumeration and switching across Console, Multimedia, and Communications roles.

## Test

1. Start the v1.3-dev.3 Windows Host.
2. Connect the Android debug build.
3. Refresh Audio Output.
4. Confirm the Realtek endpoint appears as `Speakers (Realtek)`.
5. Confirm `Speakers (Sound Blaster X4)` remains distinct.
6. Switch among Realtek, Sound Blaster X4, display audio, and headphones.
7. Verify the green current-output state follows every successful switch.
