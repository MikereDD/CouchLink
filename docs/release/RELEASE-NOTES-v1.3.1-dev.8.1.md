# CouchLink v1.3.1-dev.8.1

TV input selector correction build before the release candidate.

## Fixed

- Removes the stale **Fire TV Stick** name from HDMI 3.
- Replaces Hisense-specific HDMI subtitles with the neutral label **HDMI input**.
- Removes the permanent HDMI 3 highlight.
- Leaves all HDMI rows unselected because CouchLink does not currently receive trustworthy live input-state feedback from the TV.
- Preserves the validated Hisense mappings: HDMI 1 = HW4, HDMI 2 = HW5, and HDMI 3 = HW6.

## Version

- Android: `1.3.1-dev.8.1` (`versionCode 158`)
- Windows Host: `1.3.1-dev.8.1`
