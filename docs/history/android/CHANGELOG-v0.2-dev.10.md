# CouchLink Remote v0.2-dev.10 — Clean Code Pass

## Architecture cleanup

- Preserved the approved v0.2-dev.9 premium interface while separating the former monolithic `MainActivity.kt` into focused files.
- Added dedicated modules for the CouchLink theme, premium shared components, Home, Touchpad, Keyboard, Settings, app models, HID commands, HID key mapping, and the HID report descriptor.
- Reduced `MainActivity.kt` from roughly 1,366 lines to a small orchestration layer responsible for state, permissions, navigation, and routing commands to the HID controller.
- Added `.editorconfig` and `.gitignore` files for consistent formatting and cleaner source control.
- Removed wildcard imports, compressed one-line functions, stringly typed command routing, and the remaining light font weight.

## Type safety

- Added `LauncherId` for supported launcher actions.
- Added `MouseButton`, `MouseAction`, `RemoteKey`, and `WindowsShortcut` command types.
- Replaced raw command strings such as `"left"`, `"click"`, and `"task_manager"` with compile-time checked enums.
- Moved keyboard character mapping and the composite HID descriptor into focused HID source files.

## Reliability fixes

- Prevented duplicate Bluetooth HID profile-proxy requests while one is already pending.
- Explicit disconnect now reports a normal disconnected state instead of falsely claiming that CouchLink is reconnecting.
- A rejected connection request now clears the connecting state and reports the failure.
- Invalid Bluetooth addresses are handled safely instead of being allowed to throw through the UI.
- Paired-host sorting now uses a locale-stable comparison.
- `sendText()` now fails when any HID report fails instead of returning success after a partial send.
- Launcher automation now stops before pressing Enter if command text could not be sent.
- Drag lock is released automatically when Bluetooth disconnects or the user leaves the Touchpad screen.
- Bluetooth state is refreshed when the Activity resumes and after a permission result, including denied permission results.
- HID profile loss now schedules recovery instead of waiting for the Activity to resume.
- The disconnecting callback no longer reuses the connecting flag, preventing a temporary false “reconnecting” status.
- Disconnect rejection now reports a clear failure while preserving the live connection state.

## State and UI hygiene

- Navigation destination, touchpad sensitivity, keyboard draft text, connection-panel expansion, and paired-device expansion now survive Activity recreation where appropriate.
- Static launcher, command-deck, keyboard, and navigation definitions are no longer rebuilt during every recomposition.
- Reusable composables now accept a conventional optional `Modifier` parameter.
- Removed eight unreferenced legacy command PNGs after the Command Deck moved to custom-drawn glyphs.
- Reconnect backoff values and Bluetooth preference names are centralized constants rather than inline magic values.
- The approved premium header, Steam/GOG hierarchy, launcher order, Command Deck, and fixed metallic bottom navigation are unchanged.

## Build metadata

- Version name: `0.2-dev.10`
- Version code: `39`
- Previous build preserved: `0.2-dev.9`
