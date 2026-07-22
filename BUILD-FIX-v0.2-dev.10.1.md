# CouchLink Remote v0.2-dev.10.1 — Compose `weight` build fix

## Fixed

Android Studio reported six Kotlin compilation errors of this form:

```text
Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.
```

The cleanup pass had explicitly imported:

```kotlin
import androidx.compose.foundation.layout.weight
```

With the Compose version used by this project, that import resolves to an internal layout implementation detail rather than the public `RowScope` / `ColumnScope` modifier extension.

The invalid import was removed from all six affected source files:

- `MainActivity.kt`
- `PremiumComponents.kt`
- `HomeScreen.kt`
- `KeyboardScreen.kt`
- `SettingsScreen.kt`
- `TouchpadScreen.kt`

All existing calls such as `Modifier.weight(1f)` remain unchanged. They are valid inside `Row` and `Column` content scopes and resolve through those scope receivers without an explicit import.

## Build metadata

- Version name: `0.2-dev.10.1`
- Version code: `40`
- Previous build preserved: `0.2-dev.10`
