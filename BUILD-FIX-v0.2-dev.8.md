# CouchLink v0.2-dev.8 build fix

Corrected the five Kotlin compilation errors reported in `MainActivity.kt`:

- Restored the missing `PremiumHeader` composable used by `CouchLinkApp`.
- Escaped the four Windows path separators in the direct EA Desktop launch command.

The EA command still resolves at runtime to:

`%ProgramFiles%\Electronic Arts\EA Desktop\EA Desktop\EADesktop.exe`
