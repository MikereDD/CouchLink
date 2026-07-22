# CouchLink v0.2-dev.11 — Orange Outline Pass

## Visual changes

- Replaced silver/steel panel outlines with CouchLink brand orange (`#FF8617`).
- Kept the outlines crisp and restrained rather than neon or heavily glowing.
- Converted all eight launcher marks to monochrome white at render time:
  Steam, GOG Galaxy, Xbox, EA app, Ubisoft, Rockstar, Epic, and Amazon.
- Preserved the dark premium surfaces, cyan Steam subtitle, violet GOG subtitle,
  green connection state, and existing layout.
- Applied the matching orange outline language to the status pill, Bluetooth panel,
  launcher cards, Command Deck, bottom navigation, and shared premium panels.

## Build metadata

- Version name: `0.2-dev.11`
- Version code: `41`

## Validation

- Source and resource structure checked.
- Kotlin edits inspected for balanced delimiters and required imports.
- ZIP archive integrity tested after packaging.
- Full Gradle compilation could not run in this environment because the wrapper
  attempted to download Gradle from `services.gradle.org`, which is unavailable here.
