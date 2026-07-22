package dev.typezero.couchlink.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.hid.MouseAction
import dev.typezero.couchlink.remote.hid.MouseButton
import dev.typezero.couchlink.remote.ui.components.ActionButton
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Panel
import dev.typezero.couchlink.remote.ui.theme.Raised2
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FAST_POINTER_THRESHOLD = 18f
private const val FAST_POINTER_MULTIPLIER = 1.25f
private const val SCROLL_ACTIVATION_THRESHOLD = 0.8f
private const val SCROLL_MULTIPLIER = 9f
private const val MAX_SCROLL_REPORT = 480

@Composable
internal fun TouchpadScreen(
    enabled: Boolean,
    sensitivity: Float,
    onSensitivityChanged: (Float) -> Unit,
    dragLock: Boolean,
    onMove: (Int, Int) -> Unit,
    onButton: (MouseButton, MouseAction) -> Unit,
    onScroll: (Int) -> Unit,
    onDragToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .background(Panel, RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = if (enabled) Accent else Raised2,
                shape = RoundedCornerShape(24.dp),
            )
            .pointerInput(enabled, sensitivity) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val speedBoost = if (
                        abs(dragAmount.x) + abs(dragAmount.y) > FAST_POINTER_THRESHOLD
                    ) {
                        FAST_POINTER_MULTIPLIER
                    } else {
                        1f
                    }
                    onMove(
                        (dragAmount.x * sensitivity * speedBoost).roundToInt(),
                        (dragAmount.y * sensitivity * speedBoost).roundToInt(),
                    )
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onTap = { onButton(MouseButton.Left, MouseAction.Click) },
                    onDoubleTap = {
                        onButton(MouseButton.Left, MouseAction.Click)
                        onButton(MouseButton.Left, MouseAction.Click)
                    },
                    onLongPress = { onButton(MouseButton.Right, MouseAction.Click) },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val pressedPointers = awaitPointerEvent()
                            .changes
                            .filter { it.pressed }
                        if (pressedPointers.size < 2) continue

                        val deltaY = pressedPointers
                            .map { it.positionChange().y }
                            .average()
                            .toFloat()
                        if (abs(deltaY) < SCROLL_ACTIVATION_THRESHOLD) continue

                        pressedPointers.forEach { it.consume() }
                        onScroll(
                            (-deltaY * SCROLL_MULTIPLIER)
                                .roundToInt()
                                .coerceIn(-MAX_SCROLL_REPORT, MAX_SCROLL_REPORT),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TOUCHPAD",
                color = if (enabled) TextColor else Muted,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (enabled) {
                    "Move • tap • hold • two-finger scroll"
                } else {
                    "Connect Bluetooth input in Settings"
                },
                color = Muted,
                fontSize = 12.sp,
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Sensitivity",
            color = Muted,
            fontSize = 12.sp,
        )
        Slider(
            value = sensitivity,
            onValueChange = onSensitivityChanged,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            enabled = enabled,
            valueRange = 0.65f..2.4f,
        )
        Text(
            text = String.format(Locale.US, "%.2f×", sensitivity),
            color = TextColor,
            fontSize = 12.sp,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionButton(
            label = "LEFT",
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            onButton(MouseButton.Left, MouseAction.Click)
        }
        ActionButton(
            label = if (dragLock) "RELEASE" else "DRAG LOCK",
            enabled = enabled,
            modifier = Modifier.weight(1f),
            active = dragLock,
            action = onDragToggle,
        )
        ActionButton(
            label = "RIGHT",
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            onButton(MouseButton.Right, MouseAction.Click)
        }
    }
}
