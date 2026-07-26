# Audio Output Favorites — v1.3-dev.4

This build adds fast, persistent Headphones and TV / Display shortcuts above the full Windows playback-device list.

## Changes

- Adds Headphones and TV / Display favorite cards.
- Tapping an available favorite switches Windows audio immediately.
- EDIT opens a picker containing the currently available playback endpoints.
- CLEAR removes an assigned favorite.
- Favorites are stored by stable endpoint ID and full friendly name for each trusted Windows host.
- A saved device that disconnects remains visible as `UNAVAILABLE`.
- Refreshing after Bluetooth, USB, HDMI, dock, or wireless reconnection restores the shortcut automatically.
- First use seeds likely favorites from headphone/headset and display-audio names; both remain editable.

## Test

1. Start the v1.3-dev.4 Windows Host and connect the Android debug build.
2. Confirm Headphones and TV / Display cards appear above the complete output list.
3. Tap each available favorite and verify Windows audio and the active marker move together.
4. Use EDIT to assign different endpoints to both slots.
5. Restart the Android app and confirm both assignments persist for the same host.
6. Disconnect or disable a favorite endpoint and press REFRESH.
7. Confirm the favorite remains visible as `UNAVAILABLE` and cannot be selected.
8. Reconnect the endpoint, press REFRESH, and confirm the favorite becomes switchable again.
9. Use CLEAR and confirm the slot returns to `Not set`.
