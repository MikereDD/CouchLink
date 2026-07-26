# CouchLink 1.3-dev.6.3 Visual Test Notes

## Build gate

```powershell
dotnet build .\src\windows\CouchLink.sln -c Release
```

## Main window

- Verify custom title bar minimize, maximize/restore, close, dragging, and resize borders.
- Verify the dashboard at minimum, default, maximized, and restored sizes.
- Confirm no card, footer, button, trusted-device row, or final right-column section is clipped.
- Confirm the right-column scrollbar is dark and gains an orange outline while hovered/dragged.
- Confirm ABOUT, PAIR NEW DEVICE, REVOKE SELECTED, and REVOKE ALL retain their commands.
- Verify keyboard focus is visible on buttons and startup checkboxes.

## Trusted devices

- Verify rows show device name, stable identifier, and paired timestamp.
- Select multiple rows and confirm the selected row receives the orange CouchLink state.
- Verify scrolling with enough trusted devices to overflow the list.
- Verify Revoke Selected and Revoke All behavior is unchanged.

## Startup and persistence

- Toggle Start with Windows and Start minimized to tray.
- Restart the host and verify values persist.
- Confirm Audio Output favorites remain unchanged after toggling either startup option.

## Regression

- Pair a device and open a persistent trusted session.
- Confirm host status, client count, connected-device name, last event, endpoints, and footer Host ID update as before.
- Open Audio Output and confirm its approved v1.3-dev.6.2 styling and behavior are unchanged.
