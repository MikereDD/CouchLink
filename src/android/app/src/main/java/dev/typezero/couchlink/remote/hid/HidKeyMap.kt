package dev.typezero.couchlink.remote.hid

internal data class KeyStroke(
    val modifiers: Int,
    val usage: Int,
)

internal object HidKeyMap {
    fun fromCharacter(character: Char): KeyStroke? {
        if (character in 'a'..'z') {
            return KeyStroke(0, 0x04 + (character - 'a'))
        }
        if (character in 'A'..'Z') {
            return KeyStroke(HidModifier.LEFT_SHIFT, 0x04 + (character - 'A'))
        }
        if (character in '1'..'9') {
            return KeyStroke(0, 0x1E + (character - '1'))
        }
        if (character == '0') return KeyStroke(0, 0x27)

        return when (character) {
            ' ' -> KeyStroke(0, 0x2C)
            '\n', '\r' -> KeyStroke(0, 0x28)
            '\t' -> KeyStroke(0, 0x2B)
            '-' -> KeyStroke(0, 0x2D)
            '=' -> KeyStroke(0, 0x2E)
            '[' -> KeyStroke(0, 0x2F)
            ']' -> KeyStroke(0, 0x30)
            '\\' -> KeyStroke(0, 0x31)
            ';' -> KeyStroke(0, 0x33)
            '\'' -> KeyStroke(0, 0x34)
            '`' -> KeyStroke(0, 0x35)
            ',' -> KeyStroke(0, 0x36)
            '.' -> KeyStroke(0, 0x37)
            '/' -> KeyStroke(0, 0x38)
            '!' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x1E)
            '@' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x1F)
            '#' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x20)
            '$' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x21)
            '%' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x22)
            '^' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x23)
            '&' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x24)
            '*' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x25)
            '(' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x26)
            ')' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x27)
            '_' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x2D)
            '+' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x2E)
            '{' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x2F)
            '}' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x30)
            '|' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x31)
            ':' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x33)
            '"' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x34)
            '~' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x35)
            '<' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x36)
            '>' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x37)
            '?' -> KeyStroke(HidModifier.LEFT_SHIFT, 0x38)
            else -> null
        }
    }
}
