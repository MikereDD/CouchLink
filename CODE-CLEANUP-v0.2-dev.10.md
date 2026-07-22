# Code cleanup report — v0.2-dev.10

## Primary finding

The largest maintenance problem in v0.2-dev.9 was the 1,366-line `MainActivity.kt`. It mixed Activity lifecycle handling, Bluetooth permission flows, app state, navigation, every screen, visual components, launcher metadata, touch gestures, diagnostics, and command routing in one file.

## New source layout

```text
app/src/main/java/dev/typezero/couchlink/remote/
├── MainActivity.kt
├── model/
│   └── AppModels.kt
├── hid/
│   ├── BluetoothHidController.kt
│   ├── BluetoothHidRuntime.kt
│   ├── BluetoothHidService.kt
│   ├── HidKeyMap.kt
│   ├── HidReport.kt
│   └── InputCommands.kt
└── ui/
    ├── components/
    │   └── PremiumComponents.kt
    ├── screens/
    │   ├── HomeScreen.kt
    │   ├── KeyboardScreen.kt
    │   ├── SettingsScreen.kt
    │   └── TouchpadScreen.kt
    └── theme/
        └── CouchLinkTheme.kt
```

## Boundaries preserved

- The premium visual design was not downgraded or replaced with stock Material styling.
- Steam remains the dominant full-width launcher.
- GOG Galaxy remains the second full-width feature launcher.
- The launcher grid remains Xbox, EA app, Ubisoft / Rockstar, Epic, Amazon.
- Bluetooth HID remains the active input and launcher transport.
- Existing preferences keep their original keys, so installing over v0.2-dev.9 should preserve them.
- Legacy command artwork that had no remaining code references was removed; the premium Command Deck uses the current custom-drawn glyphs.

## Remaining future cleanup candidates

These were intentionally not expanded during this pass because they would change architecture or behavior substantially:

- Move user-facing strings into Android string resources when localization becomes a roadmap item.
- Add instrumented Compose tests for the premium layout and navigation.
- Add unit tests around reconnect timing and Bluetooth callback state transitions through an injectable Bluetooth adapter boundary.
- Replace delayed launcher automation with a serialized HID command queue if rapid report delivery proves unreliable on real devices.
