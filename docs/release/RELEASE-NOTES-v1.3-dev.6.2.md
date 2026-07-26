# CouchLink 1.3-dev.6.2 Release Notes

CouchLink 1.3-dev.6.2 is a targeted **Audio Output reliability** patch for the Windows host. It hardens preference persistence, audio-device enumeration, and the Audio Output popup's threading. It intentionally makes **no** changes to the approved popup design — sizing, theme, dark scrollbar, device-row layout, footer, and favorites are all unchanged.

## Fixes

- **Single shared preferences instance.** The dashboard and the Audio Output popup now read and write one shared `HostPreferences` object. This removes a case where saving a favorite from the popup and toggling a startup option on the dashboard could overwrite each other's changes on disk.
- **Atomic settings persistence.** Settings are serialized to a temp file and atomically swapped into place, with a rolling backup of the previous file. A settings file that fails to parse is preserved (`host-settings.corrupt.json`) rather than being silently reset, and load/save failures are recorded to `host-settings.log`.
- **Fault-isolated audio enumeration.** `AudioOutputController.List()` validates the per-item HRESULT, catches errors per endpoint and continues, null-guards COM releases, and preserves already-enumerated devices. One malfunctioning endpoint or driver no longer blanks the whole output list.
- **Off-UI-thread audio operations.** Device enumeration and switching run on a background thread; UI updates are marshaled back through the dispatcher, and overlapping operations are guarded so the popup stays responsive during device changes. All existing success and failure messages are preserved.
- **Window drag hardening.** The redundant custom title-bar `DragMove` handler — which could throw while the window was maximized — has been removed. Dragging now relies on the standard window-chrome caption region.

## Deferred to v1.3-dev.7

- Live device-change notification via `IMMNotificationClient` so the popup and Android reflect changes made externally through Windows while open.
- Android/Windows state push synchronization.

## Scope

- Windows host only. Android has no functional changes in this patch.
- No protocol changes; wire compatibility with existing paired devices is unchanged.

## Version metadata

```text
Windows host version   1.3-dev.6.2  (HostConstants.HostVersion)
Android versionName    1.3-dev.6.2
Android versionCode    137
protocol version       1 (unchanged)
```
