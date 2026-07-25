package dev.typezero.couchlink.remote.model

internal enum class AppScreen {
    Home,
    Touchpad,
    Keyboard,
    TvRemote,
    Settings,
}

internal enum class LauncherId(
    val displayName: String,
) {
    Steam("Steam"),
    Gog("GOG Galaxy"),
    Xbox("Xbox"),
    Ea("EA app"),
    Ubisoft("Ubisoft Connect"),
    Rockstar("Rockstar Games Launcher"),
    Epic("Epic Games Launcher"),
    Amazon("Amazon Games"),
}
