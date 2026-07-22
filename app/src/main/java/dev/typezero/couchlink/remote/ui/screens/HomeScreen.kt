package dev.typezero.couchlink.remote.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.R
import dev.typezero.couchlink.remote.hid.WindowsShortcut
import dev.typezero.couchlink.remote.model.LauncherId
import dev.typezero.couchlink.remote.ui.components.SectionLabel
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Cyan
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Purple
import dev.typezero.couchlink.remote.ui.theme.Silver
import dev.typezero.couchlink.remote.ui.theme.Success
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor

private data class SecondaryLauncherSpec(
    val label: String,
    val launcher: LauncherId,
    @DrawableRes val iconRes: Int,
)

private data class CommandSpec(
    val label: String,
    val shortcut: WindowsShortcut,
    val glyph: CommandGlyphType,
)

private enum class CommandGlyphType {
    AltTab,
    Task,
    Show,
    Close,
}

private val secondaryLauncherRows = listOf(
    listOf(
        SecondaryLauncherSpec("Xbox", LauncherId.Xbox, R.drawable.launcher_xbox),
        SecondaryLauncherSpec("EA app", LauncherId.Ea, R.drawable.launcher_ea),
        SecondaryLauncherSpec("Ubisoft", LauncherId.Ubisoft, R.drawable.launcher_ubisoft),
    ),
    listOf(
        SecondaryLauncherSpec("Rockstar", LauncherId.Rockstar, R.drawable.launcher_rockstar),
        SecondaryLauncherSpec("Epic", LauncherId.Epic, R.drawable.launcher_epic),
        SecondaryLauncherSpec("Amazon", LauncherId.Amazon, R.drawable.launcher_amazon),
    ),
)

private val commandDeck = listOf(
    CommandSpec("Alt-Tab", WindowsShortcut.AltTab, CommandGlyphType.AltTab),
    CommandSpec("Task", WindowsShortcut.TaskManager, CommandGlyphType.Task),
    CommandSpec("Show", WindowsShortcut.ShowDesktop, CommandGlyphType.Show),
    CommandSpec("Close", WindowsShortcut.CloseWindow, CommandGlyphType.Close),
)

@Composable
internal fun HomeScreen(
    onLauncher: (LauncherId) -> Unit,
    onShortcut: (WindowsShortcut) -> Unit,
    launcherEnabled: Boolean,
    inputEnabled: Boolean,
) {
    SectionLabel("LAUNCHERS")
    if (!launcherEnabled) {
        Text(
            text = "Connect Bluetooth input to use launchers.",
            color = Muted,
            fontSize = 11.sp,
        )
    }

    PrimaryLauncherTile(
        iconRes = R.drawable.launcher_steam,
        title = "Steam",
        subtitle = "Launch Big Picture",
        running = false,
        enabled = launcherEnabled,
        onClick = { onLauncher(LauncherId.Steam) },
    )

    FeaturedLauncherTile(
        iconRes = R.drawable.launcher_gog,
        title = "GOG Galaxy",
        subtitle = "Open your DRM-free library",
        enabled = launcherEnabled,
        onClick = { onLauncher(LauncherId.Gog) },
    )

    secondaryLauncherRows.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row.forEach { launcher ->
                SecondaryLauncherTile(
                    iconRes = launcher.iconRes,
                    label = launcher.label,
                    enabled = launcherEnabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onLauncher(launcher.launcher) },
                )
            }
        }
    }

    Spacer(Modifier.height(2.dp))
    SectionLabel("COMMAND DECK")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        commandDeck.forEach { command ->
            CommandDeckTile(
                glyph = command.glyph,
                label = command.label,
                enabled = inputEnabled,
                modifier = Modifier.weight(1f),
                onClick = { onShortcut(command.shortcut) },
            )
        }
    }
}

@Composable
private fun PrimaryLauncherTile(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    running: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    HeroLauncherTile(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        accent = Cyan,
        height = 124.dp,
        iconSize = 88.dp,
        titleSize = 28.sp,
        running = running,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun FeaturedLauncherTile(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    HeroLauncherTile(
        iconRes = iconRes,
        title = title,
        subtitle = subtitle,
        accent = Purple,
        height = 108.dp,
        iconSize = 76.dp,
        titleSize = 23.sp,
        running = false,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun HeroLauncherTile(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    accent: Color,
    height: Dp,
    iconSize: Dp,
    titleSize: TextUnit,
    running: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val alpha = if (enabled) 1f else 0.42f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer(alpha = alpha)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.76f),
                spotColor = Color.Black.copy(alpha = 0.34f),
            )
            .background(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF111820), Color(0xFF030608), Color(0xFF0B1117)),
                ),
                shape = shape,
            )
            .border(1.35.dp, Accent.copy(alpha = 0.96f), shape)
            .padding(2.dp)
            .border(0.75.dp, Accent.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 1.dp.toPx())
            drawArc(
                color = accent.copy(alpha = 0.08f),
                startAngle = 212f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(size.width * 0.42f, size.height * -0.34f),
                size = Size(size.width * 0.78f, size.height * 1.20f),
                style = stroke,
            )
            drawArc(
                color = Silver.copy(alpha = 0.05f),
                startAngle = 203f,
                sweepAngle = 148f,
                useCenter = false,
                topLeft = Offset(size.width * 0.50f, size.height * -0.20f),
                size = Size(size.width * 0.64f, size.height * 1.02f),
                style = stroke,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCradle(
                iconRes = iconRes,
                description = title,
                size = iconSize,
                accent = accent,
            )
            Spacer(Modifier.width(17.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = TextColor,
                        fontSize = titleSize,
                        lineHeight = titleSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    if (running) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "●",
                            color = Success,
                            fontSize = 12.sp,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = accent,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            Text(
                text = "›",
                color = accent.copy(alpha = 0.96f),
                fontSize = 43.sp,
                lineHeight = 43.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun IconCradle(
    @DrawableRes iconRes: Int,
    description: String,
    size: Dp,
    accent: Color,
) {
    val shape = RoundedCornerShape(23.dp)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(8.dp, shape, spotColor = Color.Black.copy(alpha = 0.42f))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF202830), Color(0xFF030608)),
                ),
                shape = shape,
            )
            .border(1.15.dp, Accent.copy(alpha = 0.92f), shape)
            .padding(4.dp)
            .border(0.65.dp, Accent.copy(alpha = 0.34f), RoundedCornerShape(20.dp))
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}

@Composable
private fun SecondaryLauncherTile(
    @DrawableRes iconRes: Int,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(19.dp)
    val alpha = if (enabled) 1f else 0.42f

    Column(
        modifier = modifier
            .height(112.dp)
            .graphicsLayer(alpha = alpha)
            .shadow(9.dp, shape, ambientColor = Color.Black.copy(alpha = 0.68f))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF151C23), Color(0xFF040709)),
                ),
                shape = shape,
            )
            .border(1.1.dp, Accent.copy(alpha = 0.92f), shape)
            .padding(2.dp)
            .border(0.6.dp, Accent.copy(alpha = 0.30f), RoundedCornerShape(17.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconCradle(
            iconRes = iconRes,
            description = label,
            size = 68.dp,
            accent = Accent,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = TextColor.copy(alpha = 0.88f),
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CommandDeckTile(
    glyph: CommandGlyphType,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val alpha = if (enabled) 1f else 0.38f

    Column(
        modifier = modifier
            .height(90.dp)
            .graphicsLayer(alpha = alpha)
            .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.68f))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF182129), Color(0xFF040709)),
                ),
                shape = shape,
            )
            .border(1.1.dp, Accent.copy(alpha = 0.92f), shape)
            .padding(2.dp)
            .border(0.6.dp, Accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CommandGlyph(
            kind = glyph,
            color = TextColor.copy(alpha = alpha),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = TextColor.copy(alpha = alpha * 0.86f),
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CommandGlyph(
    kind: CommandGlyphType,
    color: Color,
) {
    Canvas(Modifier.size(36.dp)) {
        val strokeWidth = 2.3.dp.toPx()
        val thinStrokeWidth = 1.8.dp.toPx()

        when (kind) {
            CommandGlyphType.AltTab -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.10f, size.height * 0.29f),
                    size = Size(size.width * 0.54f, size.height * 0.47f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(strokeWidth),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.36f, size.height * 0.12f),
                    size = Size(size.width * 0.54f, size.height * 0.47f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                    style = Stroke(strokeWidth),
                )
            }

            CommandGlyphType.Task -> {
                repeat(3) { index ->
                    val y = size.height * (0.22f + index * 0.27f)
                    drawLine(
                        color,
                        Offset(size.width * 0.12f, y),
                        Offset(size.width * 0.19f, y + size.height * 0.07f),
                        thinStrokeWidth,
                    )
                    drawLine(
                        color,
                        Offset(size.width * 0.19f, y + size.height * 0.07f),
                        Offset(size.width * 0.29f, y - size.height * 0.05f),
                        thinStrokeWidth,
                    )
                    drawLine(
                        color,
                        Offset(size.width * 0.39f, y),
                        Offset(size.width * 0.88f, y),
                        strokeWidth,
                    )
                }
            }

            CommandGlyphType.Show -> {
                val eye = Path().apply {
                    moveTo(size.width * 0.08f, size.height * 0.50f)
                    quadraticBezierTo(
                        size.width * 0.50f,
                        size.height * 0.10f,
                        size.width * 0.92f,
                        size.height * 0.50f,
                    )
                    quadraticBezierTo(
                        size.width * 0.50f,
                        size.height * 0.90f,
                        size.width * 0.08f,
                        size.height * 0.50f,
                    )
                    close()
                }
                drawPath(eye, color, style = Stroke(strokeWidth))
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.12f,
                    center = center,
                    style = Stroke(strokeWidth),
                )
            }

            CommandGlyphType.Close -> {
                drawLine(
                    color,
                    Offset(size.width * 0.17f, size.height * 0.17f),
                    Offset(size.width * 0.83f, size.height * 0.83f),
                    strokeWidth,
                )
                drawLine(
                    color,
                    Offset(size.width * 0.83f, size.height * 0.17f),
                    Offset(size.width * 0.17f, size.height * 0.83f),
                    strokeWidth,
                )
            }
        }
    }
}
