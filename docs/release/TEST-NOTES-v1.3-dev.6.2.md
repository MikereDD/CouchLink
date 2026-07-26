# CouchLink 1.3-dev.6.2 Test Notes

Focused verification for the Audio Output reliability patch. Run on the Windows host (`CouchLink.Host`). No Android build is required for this patch, though a paired device is useful for the remote-switch checks.

## 1. Shared preferences (persistence)

- [ ] Open the Audio Output popup, assign a Headphones favorite. Close the popup.
- [ ] Open the dashboard, toggle **Start with Windows** on, then off.
- [ ] Reopen the Audio Output popup — the Headphones favorite is still assigned. (Pre-patch this could be lost.)
- [ ] Reverse order: enable **Start with Windows**, then assign a favorite from the popup. Confirm the startup setting and the Run-key registration are still intact.
- [ ] Restart the host and confirm both favorites and startup options persist.

## 2. Atomic save + corruption handling

- [ ] With the host closed, manually corrupt `%AppData%\CouchLink\host-settings.json` (truncate it / insert invalid JSON).
- [ ] Start the host. Confirm it starts with defaults, `host-settings.corrupt.json` now contains the corrupt content, and `host-settings.log` records the load failure.
- [ ] Change a setting and confirm a fresh valid `host-settings.json` is written, with `host-settings.backup.json` holding the prior version after a subsequent save.

## 3. Fault-isolated enumeration

- [ ] With multiple output devices present (including at least one HDMI/display and one headset), open the popup and confirm all expected active outputs are listed.
- [ ] If a driver/endpoint is in a bad state, confirm the list still populates the healthy devices rather than showing an empty list with an error.
- [ ] Confirm long and duplicate device names still render (wrapped) and remain switchable.

## 4. Off-UI-thread operations / responsiveness

- [ ] Tap a favorite or a device row; confirm the switch applies to all roles and the status message matches the previous wording (e.g., "Audio switched to <name>.").
- [ ] Rapidly tap multiple rows; confirm no duplicate/overlapping switches occur and the window re-enables cleanly after each.
- [ ] Confirm the window does not freeze during enumeration/switching.
- [ ] Confirm Refresh, favorites edit/clear, and the active-output highlight behave as before.

## 5. Window chrome

- [ ] Drag the window by the title bar — dragging works via the chrome caption.
- [ ] Maximize (double-click caption), then drag — confirm no crash (the previous manual DragMove path is gone).
- [ ] Minimize and Close from the custom chrome buttons still work.

## 6. Regression / unchanged design

- [ ] Popup sizing at minimum, default, and enlarged window sizes is unchanged; footer stays anchored; no clipped device rows.
- [ ] Dark scrollbar, row layout, and favorites cards are visually identical to 1.3-dev.6.1.2.

## Build

```text
Windows: dotnet build src/windows/CouchLink.sln -c Release
Android: unchanged (version aligned to 1.3-dev.6.2 / versionCode 137)
```
