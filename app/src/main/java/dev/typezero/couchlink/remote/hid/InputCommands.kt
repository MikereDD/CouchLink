package dev.typezero.couchlink.remote.hid

internal enum class MouseButton(
    val mask: Int,
) {
    Left(0x01),
    Right(0x02),
    Middle(0x04),
}

internal enum class MouseAction {
    Down,
    Up,
    Click,
}

internal enum class RemoteKey(
    val usage: Int,
) {
    Enter(0x28),
    Escape(0x29),
    Backspace(0x2A),
    Tab(0x2B),
    Space(0x2C),
    Delete(0x4C),
    Right(0x4F),
    Left(0x50),
    Down(0x51),
    Up(0x52),
    Home(0x4A),
    End(0x4D),
}

internal enum class WindowsShortcut(
    val modifiers: Int,
    val usage: Int,
) {
    AltTab(HidModifier.LEFT_ALT, 0x2B),
    CloseWindow(HidModifier.LEFT_ALT, 0x3D),
    TaskManager(HidModifier.LEFT_CTRL or HidModifier.LEFT_SHIFT, 0x29),
    ShowDesktop(HidModifier.LEFT_GUI, 0x07),
}

internal object HidModifier {
    const val LEFT_CTRL = 0x01
    const val LEFT_SHIFT = 0x02
    const val LEFT_ALT = 0x04
    const val LEFT_GUI = 0x08
}
