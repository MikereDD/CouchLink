# CouchLink v0.1-dev.11.3 Test Checklist

This is a research-foundation release, not a functional lock-screen HID release.

- Windows solution builds without installing any driver.
- Android `assembleDebug` reports `0.1-dev.11.3` / versionCode `26`.
- Boot Service and normal desktop control remain functional.
- `tools/Test-CouchLinkDriverPrereqs.ps1` reports whether Visual Studio C++ and Windows Kits are available.
- No `.sys`, `.inf`, `.cat`, or driver-install command is present.
- Review `docs/VIRTUAL-HID-FOUNDATION.md` before beginning VM work.
