# CouchLink v1.3-dev.7 Release Notes

This release hardens Audio Output synchronization across the Windows popup and connected Android remote.

## Changes
- The Windows Audio Output popup checks for endpoint/default-output changes every two seconds while visible and updates only when the device fingerprint changes.
- Android requests an updated output list every 2.5 seconds while a trusted host session is connected.
- Background synchronization stops when the popup closes or the host session disconnects.
- Manual Refresh remains available.
- No approved visual styling was changed.
