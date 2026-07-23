# Build validation — v0.2-dev.10

## Completed in the source-cleanup environment

- Pure Kotlin HID source compilation: passed for app models, typed input commands, key mapping, and report descriptor.
- HID command self-test: passed for all eight launcher inputs, including the Steam URI and escaped EA executable command.
- Kotlin parser/structure scan: no unexpected-token, delimiter, redeclaration, or visibility-exposure errors detected.
- Source hygiene scan: no wildcard imports, TODO/FIXME/HACK markers, trailing whitespace, lines over 120 characters, stringly typed HID commands, or light/thin font weights remain.
- Android manifest and resource XML parse: passed.
- Drawable reference scan: passed; all referenced drawables exist and no unreferenced `drawable-nodpi` PNGs remain.
- Gradle wrapper JAR integrity: passed.
- Version metadata consistency: `0.2-dev.10` / version code `39`.
- `MainActivity.kt` orchestration-size gate: passed at 284 lines.
- Source archive integrity: passed with a separate SHA-256 checksum.

## Environment limitation

A full Gradle/Android compile was attempted, but the container could not resolve `services.gradle.org`; the Gradle 9.3 distribution and Android/Compose dependencies were not cached. Final `assembleDebug` validation must therefore be completed in Android Studio with JDK 17.
