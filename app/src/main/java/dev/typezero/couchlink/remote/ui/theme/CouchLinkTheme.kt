package dev.typezero.couchlink.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val SurfaceColor = Color(0xFF020405)
internal val Panel = Color(0xFF070B0F)
internal val Raised = Color(0xFF0D1319)
internal val Raised2 = Color(0xFF151D25)
internal val Text = Color(0xFFF7F8FA)
internal val Muted = Color(0xFFB4B7BC)
internal val Accent = Color(0xFFFF8617)
internal val Success = Color(0xFF83FF4F)
internal val Danger = Color(0xFFFF6B6B)
internal val Cyan = Color(0xFF67D7FF)
internal val Purple = Color(0xFFC15AFF)
internal val Silver = Color(0xFFDDE4EA)
internal val SilverDim = Color(0xFF59636D)

@Composable
internal fun CouchLinkTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = SurfaceColor,
            surface = Panel,
            primary = Accent,
            onBackground = Text,
            onSurface = Text,
            onPrimary = Color.Black,
        ),
        content = content,
    )
}
