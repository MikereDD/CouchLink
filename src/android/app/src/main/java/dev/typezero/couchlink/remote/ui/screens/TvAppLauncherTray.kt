package dev.typezero.couchlink.remote.ui.screens

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.R
import dev.typezero.couchlink.remote.tv.launcher.*
import dev.typezero.couchlink.remote.tv.provider.TvProviderId
import dev.typezero.couchlink.remote.ui.theme.Accent
import dev.typezero.couchlink.remote.ui.theme.Muted
import dev.typezero.couchlink.remote.ui.theme.Silver
import dev.typezero.couchlink.remote.ui.theme.Text as TextColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI only. All launch requests go through the existing provider runtime. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TvAppLauncherTray(
    shortcuts: List<TvAppShortcut>,
    enabled: Boolean,
    providerDisplayName: String,
    onLaunch: (TvAppShortcut) -> Unit,
    onAddCustomApp: (displayName: String, launchTarget: String, icon: TvIconDraft?) -> Unit,
    onRemove: (TvAppShortcut) -> Unit,
    providerId: TvProviderId,
    supportsAppLaunch: Boolean,
    connecting: Boolean = false,
    onAddKnownService: (String) -> Unit,
    onMove: (TvAppShortcut, Int) -> Unit,
    iconStorage: TvCustomIconStorage? = null,
    serviceIconCache: TvKnownServiceIconCache? = null,
    onSetCustomIcon: ((TvAppShortcut, TvIconDraft?) -> Unit)? = null,
) {
    var expanded by rememberSaveable(providerId) { mutableStateOf(false) }
    var managing by rememberSaveable(providerId) { mutableStateOf(false) }
    var addStep by rememberSaveable(providerId) { mutableStateOf<String?>(null) }
    var managedId by rememberSaveable(providerId) { mutableStateOf<String?>(null) }
    var editingIconId by rememberSaveable(providerId) { mutableStateOf<String?>(null) }
    val status = when {
        !supportsAppLaunch -> "App launching is not supported by this TV provider."
        connecting -> "Connecting to TV…"
        !enabled -> "Connect the TV Remote to launch apps."
        shortcuts.isEmpty() -> "Add your favorite streaming apps"
        else -> "${shortcuts.size} saved • $providerDisplayName"
    }
    val dismiss = {
        expanded = false
        managing = false
        addStep = null
        managedId = null
        editingIconId = null
    }

    // Same compact launcher-card treatment as HomeScreen.LauncherTrayCard.
    val cardShape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .shadow(9.dp, cardShape, ambientColor = Color.Black.copy(alpha = 0.68f))
            .background(Brush.horizontalGradient(listOf(Color(0xFF111820), Color(0xFF05080B), Color(0xFF0B1117))), cardShape)
            .border(1.1.dp, Accent.copy(alpha = 0.92f), cardShape)
            .clickable(role = Role.Button) { expanded = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StreamingIconCradle(R.drawable.ic_tv_app_generic, null, 48)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text("STREAMING APPS", color = TextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(status, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(6.dp))
        Text("›", color = Accent, fontSize = 31.sp, fontWeight = FontWeight.Bold)
    }

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = dismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF090D11),
            contentColor = TextColor,
            scrimColor = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(Modifier.padding(top = 10.dp, bottom = 8.dp).size(42.dp, 4.dp)
                    .background(Silver.copy(alpha = 0.40f), RoundedCornerShape(2.dp)))
            },
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Streaming Apps", color = TextColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(status, color = if (enabled) Accent else Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = dismiss, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "Close streaming apps" }) {
                        Text("×", color = Muted, fontSize = 28.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { addStep = "choice" }, enabled = supportsAppLaunch) {
                        Text("+ ADD APP", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                    if (shortcuts.isNotEmpty()) {
                        TextButton(onClick = { managing = !managing }) {
                            Text(if (managing) "DONE" else "MANAGE", fontWeight = FontWeight.SemiBold, color = Muted)
                        }
                    }
                }
                if (managing) {
                    Text("Select an app to reorder or remove it.", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                }
                if (shortcuts.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        StreamingIconCradle(R.drawable.ic_tv_app_generic, null, 58)
                        Spacer(Modifier.height(12.dp))
                        Text("Your favorites, one tap away", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Add a known service or a custom TV app. Nothing is added automatically.",
                            color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
                shortcuts.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { shortcut ->
                            val customBitmap = rememberCustomIconBitmap(iconStorage, shortcut.iconReference)
                            val defaultBitmap = rememberKnownServiceIconBitmap(
                                serviceIconCache,
                                shortcut.knownServiceId.takeIf { shortcut.kind == TvAppShortcutKind.KnownService },
                            )
                            StreamingAppTile(
                                label = shortcut.displayName,
                                iconRes = streamingShortcutIcon(shortcut),
                                bitmap = customBitmap ?: defaultBitmap,
                                iconDescription = when {
                                    customBitmap != null -> "Custom app icon"
                                    defaultBitmap != null -> "Default service icon"
                                    else -> null
                                },
                                enabled = enabled || managing,
                                modifier = Modifier.weight(1f),
                                actionLabel = if (managing) "Manage ${shortcut.displayName}" else "Launch ${shortcut.displayName}",
                                onClick = {
                                    if (managing) managedId = shortcut.id else {
                                        onLaunch(shortcut)
                                        dismiss()
                                    }
                                },
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }

            }
        }
    }

    if (addStep == "choice") {
        AlertDialog(
            onDismissRequest = { addStep = null },
            title = { Text("Add app", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Choose how to add an app for $providerDisplayName.", fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { addStep = "known" }, modifier = Modifier.fillMaxWidth()) { Text("Known Service") }
                    OutlinedButton(onClick = { addStep = "custom" }, modifier = Modifier.fillMaxWidth()) { Text("Custom App") }
                }
            },
            confirmButton = { TextButton(onClick = { addStep = null }) { Text("CANCEL") } },
        )
    }
    if (addStep == "known") {
        KnownServiceDialog(
            services = if (supportsAppLaunch) TvKnownServiceCatalog.supportedBy(providerId) else emptyList(),
            addedIds = shortcuts.mapNotNull { it.knownServiceId }.toSet(),
            onDismiss = { addStep = null },
            onAdd = onAddKnownService,
        )
    }
    if (addStep == "custom") {
        AddCustomTvAppDialog(
            iconStorage = iconStorage,
            onDismiss = { addStep = null },
            onSave = onAddCustomApp,
        )
    }
    shortcuts.firstOrNull { it.id == managedId }?.let { shortcut ->
        val index = shortcuts.indexOf(shortcut)
        AlertDialog(
            onDismissRequest = { managedId = null },
            title = { Text(shortcut.displayName, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Position ${index + 1} of ${shortcuts.size}", color = Muted, fontSize = 13.sp)
                    OutlinedButton(onClick = { onMove(shortcut, index - 1) }, enabled = index > 0, modifier = Modifier.fillMaxWidth()) { Text("Move earlier") }
                    OutlinedButton(onClick = { onMove(shortcut, index + 1) }, enabled = index < shortcuts.lastIndex, modifier = Modifier.fillMaxWidth()) { Text("Move later") }
                    if (iconStorage != null && onSetCustomIcon != null) {
                        OutlinedButton(onClick = { editingIconId = shortcut.id; managedId = null }, modifier = Modifier.fillMaxWidth()) {
                            Text("EDIT ICON", fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { onRemove(shortcut); managedId = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Remove app", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { managedId = null }) { Text("DONE") } },
        )
    }
    shortcuts.firstOrNull { it.id == editingIconId }?.let { shortcut ->
        val defaultBitmap = rememberKnownServiceIconBitmap(
            serviceIconCache,
            shortcut.knownServiceId.takeIf { shortcut.kind == TvAppShortcutKind.KnownService },
        )
        if (iconStorage != null && onSetCustomIcon != null) CustomAppIconDialog(shortcut, iconStorage,
            onDismiss = { editingIconId = null }, onSave = { onSetCustomIcon(shortcut, it) }, defaultIconRes = streamingShortcutIcon(shortcut), defaultIconBitmap = defaultBitmap)
    }
}

@DrawableRes
internal fun streamingShortcutIcon(shortcut: TvAppShortcut): Int =
    // Publication fallback: service identity and launch URI never select a brand graphic.
    when (shortcut.kind) {
        TvAppShortcutKind.KnownService, TvAppShortcutKind.CustomApp -> R.drawable.ic_tv_app_generic
    }

@Composable
internal fun StreamingIconCradle(@DrawableRes iconRes: Int, description: String?, size: Int, bitmap: Bitmap? = null) {
    val shape = RoundedCornerShape(23.dp)
    Box(
        Modifier.size(size.dp).shadow(8.dp, shape, spotColor = Color.Black.copy(alpha = 0.42f))
            .background(Brush.verticalGradient(listOf(Color(0xFF202830), Color(0xFF030608))), shape)
            .border(1.15.dp, Accent.copy(alpha = 0.92f), shape).padding(4.dp)
            .border(0.65.dp, Accent.copy(alpha = 0.34f), RoundedCornerShape(20.dp)).padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Preserve the original generic artwork colors; do not tint.
        if (bitmap == null) Image(painterResource(iconRes), contentDescription = description,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Image(bitmap.asImageBitmap(), contentDescription = description,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun StreamingAppTile(
    label: String,
    @DrawableRes iconRes: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    bitmap: Bitmap? = null,
    iconDescription: String? = null,
    actionLabel: String = label,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(19.dp)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Column(
        modifier.height((108 + 28 * (fontScale - 1)).dp).graphicsLayer(alpha = if (enabled) 1f else 0.42f)
            .shadow(9.dp, shape, ambientColor = Color.Black.copy(alpha = 0.68f))
            .background(Brush.verticalGradient(listOf(Color(0xFF151C23), Color(0xFF040709))), shape)
            .border(1.1.dp, Accent.copy(alpha = 0.92f), shape).padding(2.dp)
            .border(0.6.dp, Accent.copy(alpha = 0.30f), RoundedCornerShape(17.dp))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = actionLabel, onClick = onClick)
            .semantics { contentDescription = actionLabel }
            .padding(horizontal = 7.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StreamingIconCradle(iconRes, iconDescription, 58, bitmap)
        Spacer(Modifier.height(3.dp))
        Text(label, color = TextColor.copy(alpha = 0.88f), fontSize = 11.sp, lineHeight = 13.sp,
            fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun KnownServiceDialog(
    services: List<TvKnownService>,
    addedIds: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Known Service", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose a service. Its launch link is filled in for you.", fontSize = 13.sp)
                if (services.isEmpty()) Text("No supported services for this TV provider.", color = Muted)
                services.forEach { service ->
                    val added = service.id in addedIds
                    OutlinedButton(
                        enabled = !added,
                        onClick = {
                            try { onAdd(service.id); onDismiss() }
                            catch (invalid: IllegalArgumentException) { error = invalid.message ?: "Could not add this service." }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Image(painterResource(R.drawable.ic_tv_app_generic), null, Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(service.displayName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (added) Text("Added", color = Muted, fontSize = 11.sp)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

@Composable
private fun AddCustomTvAppDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, TvIconDraft?) -> Unit,
    iconStorage: TvCustomIconStorage?,
) {
    val icon = remember { TvIconEditState(null) }
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!icon.saving) onDismiss() },
        title = { Text("Custom App", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add a name and an app link supported by your TV. Your icon is optional.", fontSize = 13.sp)
                CustomIconControls(icon, iconStorage)
                OutlinedTextField(name, { name = it.take(80); error = null }, label = { Text("App name") }, enabled = !icon.saving, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(target, { target = it.take(2048); error = null }, label = { Text("Launch URI / app link") },
                    placeholder = { Text("app://open or https://…") }, enabled = !icon.saving, singleLine = true, isError = error != null, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                Text("A launch request does not confirm the app opened. Check your TV.", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && target.isNotBlank() && !icon.loading && !icon.saving, onClick = {
                val cleanName = name.trim(); val cleanTarget = target.trim(); val draft = icon.draft
                if (draft == null) {
                    try { onSave(cleanName, cleanTarget, null); onDismiss() }
                    catch (invalid: IllegalArgumentException) { error = invalid.message ?: "Check the app name and launch URI." }
                    catch (_: Exception) { error = "Could not save the shortcut. Please try again." }
                } else {
                    icon.saving = true; error = null
                    scope.launch {
                        try { withContext(Dispatchers.IO) { onSave(cleanName, cleanTarget, draft) }; onDismiss() }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (invalid: IllegalArgumentException) { error = invalid.message ?: "Check the app name and launch URI." }
                        catch (_: Exception) { error = "Could not save the shortcut. Please try again." }
                        finally { icon.saving = false }
                    }
                }
            }) { Text(if (icon.saving) "SAVING…" else "ADD") }
        },
        dismissButton = { TextButton(enabled = !icon.saving, onClick = onDismiss) { Text("CANCEL") } },
    )
}
