package dev.typezero.couchlink.remote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.R
import dev.typezero.couchlink.remote.hid.BluetoothHidController
import dev.typezero.couchlink.remote.model.AppScreen
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Cyan
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Raised
import dev.typezero.couchlink.remote.ui.theme.Silver
import dev.typezero.couchlink.remote.ui.theme.SilverDim
import dev.typezero.couchlink.remote.ui.theme.Success
import dev.typezero.couchlink.remote.ui.theme.SurfaceColor
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor

private data class NavigationDestination(
    val screen: AppScreen,
    val icon: String,
    val label: String,
)

private val navigationDestinations = listOf(
    NavigationDestination(AppScreen.Home, "⌂", "Home"),
    NavigationDestination(AppScreen.Touchpad, "◎", "Touchpad"),
    NavigationDestination(AppScreen.Keyboard, "⌨", "Keyboard"),
    NavigationDestination(AppScreen.Settings, "⚙", "Settings"),
)

@Composable
internal fun PremiumHeader(
    bluetoothState: BluetoothHidController.State,
    modifier: Modifier = Modifier,
) {
    val statusColor = when {
        bluetoothState.connected -> Success
        bluetoothState.connecting -> Accent
        bluetoothState.registered -> Success
        else -> Muted
    }
    val statusLabel = when {
        bluetoothState.connected -> "REMOTE READY"
        bluetoothState.connecting -> "CONNECTING"
        bluetoothState.registered -> "REMOTE READY"
        else -> "REMOTE OFFLINE"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.couchlink_logo),
            contentDescription = "CouchLink",
            modifier = Modifier
                .width(76.dp)
                .height(58.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Couch",
                color = TextColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
            )
            Text(
                text = "Link",
                color = Accent,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
            )
        }
        Spacer(Modifier.width(7.dp))
        StatusPill(
            label = statusLabel,
            color = statusColor,
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    color: Color,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.78f),
                spotColor = color.copy(alpha = 0.20f),
            )
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF161B20), Color(0xFF05080B)),
                ),
                shape = shape,
            )
            .border(1.15.dp, Accent.copy(alpha = 0.92f), shape)
            .padding(2.dp)
            .border(0.6.dp, Accent.copy(alpha = 0.28f), shape)
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(13.dp)
                .shadow(9.dp, CircleShape, spotColor = color.copy(alpha = 0.90f))
                .background(color, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.60f), CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = color,
            fontSize = 8.5f.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.9f.sp,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun ConnectionOverview(
    bluetoothState: BluetoothHidController.State,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusText = when {
        bluetoothState.connected -> "BLUETOOTH INPUT CONNECTED"
        bluetoothState.connecting -> "BLUETOOTH INPUT RECONNECTING"
        bluetoothState.registered -> "BLUETOOTH INPUT READY"
        else -> "BLUETOOTH INPUT DISCONNECTED"
    }
    val shape = RoundedCornerShape(23.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 11.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.80f),
                spotColor = Color.White.copy(alpha = 0.08f),
            )
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF151B21), Color(0xFF030608)),
                ),
                shape = shape,
            )
            .border(1.2.dp, Accent.copy(alpha = 0.94f), shape)
            .padding(2.dp)
            .border(0.65.dp, Accent.copy(alpha = 0.28f), RoundedCornerShape(21.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BluetoothMedallion(
                active = bluetoothState.connected || bluetoothState.registered,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = statusText,
                modifier = Modifier.weight(1f),
                color = TextColor,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.1f.sp,
                maxLines = 2,
            )
            Text(
                text = if (expanded) "⌃" else "⌄",
                color = TextColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (expanded) {
            HorizontalDivider(color = SilverDim.copy(alpha = 0.45f))
            Text(
                text = bluetoothState.message,
                color = Muted,
                fontSize = 11.sp,
            )
            bluetoothState.connectedHost?.let { host ->
                Text(
                    text = "Device: ${host.name}",
                    color = Muted,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun BluetoothMedallion(
    active: Boolean,
) {
    val glow = if (active) Cyan else SilverDim
    Box(
        modifier = Modifier
            .size(53.dp)
            .shadow(9.dp, CircleShape, spotColor = glow.copy(alpha = 0.24f))
            .background(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF26313A), Color(0xFF05080B)),
                ),
                shape = CircleShape,
            )
            .border(1.15.dp, Accent.copy(alpha = 0.92f), CircleShape)
            .padding(3.dp)
            .border(0.6.dp, Accent.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(26.dp)) {
            val stroke = 2.dp.toPx()
            val color = if (active) TextColor else Muted
            val centerX = size.width * 0.50f
            drawLine(
                color,
                Offset(centerX, size.height * 0.08f),
                Offset(centerX, size.height * 0.92f),
                stroke,
            )
            drawLine(
                color,
                Offset(centerX, size.height * 0.08f),
                Offset(size.width * 0.76f, size.height * 0.32f),
                stroke,
            )
            drawLine(
                color,
                Offset(size.width * 0.76f, size.height * 0.32f),
                Offset(size.width * 0.24f, size.height * 0.72f),
                stroke,
            )
            drawLine(
                color,
                Offset(size.width * 0.24f, size.height * 0.28f),
                Offset(size.width * 0.76f, size.height * 0.68f),
                stroke,
            )
            drawLine(
                color,
                Offset(size.width * 0.76f, size.height * 0.68f),
                Offset(centerX, size.height * 0.92f),
                stroke,
            )
        }
    }
}

@Composable
internal fun SectionLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier,
        color = Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 3.8f.sp,
    )
}

@Composable
internal fun ActionButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    action: () -> Unit,
) {
    Button(
        onClick = action,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) Accent else Raised,
        ),
        contentPadding = PaddingValues(5.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = label,
            color = if (active) Color.Black else TextColor,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun BottomNav(
    current: AppScreen,
    onSelect: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 14.dp, vertical = 7.dp)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = outerShape,
                    ambientColor = Color.Black.copy(alpha = 0.84f),
                    spotColor = Color.White.copy(alpha = 0.10f),
                )
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF111820), Color(0xFF020405)),
                    ),
                    shape = outerShape,
                )
                .border(1.2.dp, Accent.copy(alpha = 0.94f), outerShape)
                .padding(2.dp)
                .border(
                    0.7.dp,
                    Accent.copy(alpha = 0.28f),
                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationDestinations.forEachIndexed { index, destination ->
                val selected = current == destination.screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .background(
                            brush = if (selected) {
                                Brush.verticalGradient(
                                    listOf(Color(0xFF25313C), Color(0xFF080D12)),
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Transparent),
                                )
                            },
                            shape = RoundedCornerShape(18.dp),
                        )
                        .then(
                            if (selected) {
                                Modifier.border(
                                    1.dp,
                                    Accent.copy(alpha = 0.88f),
                                    RoundedCornerShape(18.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelect(destination.screen) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = destination.icon,
                            color = if (selected) TextColor else Muted,
                            fontSize = if (destination.screen == AppScreen.Settings) 24.sp else 27.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = destination.label,
                            color = if (selected) TextColor else Muted,
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }

                if (index < navigationDestinations.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(54.dp)
                            .background(Accent.copy(alpha = 0.30f)),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PremiumPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.72f))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF121920), Color(0xFF05080B)),
                ),
                shape = shape,
            )
            .border(1.dp, Accent.copy(alpha = 0.72f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
