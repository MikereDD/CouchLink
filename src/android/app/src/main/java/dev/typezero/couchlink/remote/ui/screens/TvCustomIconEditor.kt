package dev.typezero.couchlink.remote.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.typezero.couchlink.remote.R
import dev.typezero.couchlink.remote.tv.launcher.*
import dev.typezero.couchlink.remote.ui.theme.Muted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Drafts are memory-only until save. Cancel/disposal therefore cannot orphan a preview file. */
internal class TvIconEditState(val originalReference: String?) {
    var draft by mutableStateOf<TvIconDraft?>(null)
    var reset by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var saving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    val changed get() = draft != null || reset
    val reference get() = if (reset || draft != null) null else originalReference
}

@Composable
internal fun rememberCustomIconBitmap(storage: TvCustomIconStorage?, reference: String?): Bitmap? {
    // A new keyed slot prevents a reordered/replaced tile from briefly displaying the old icon.
    val result = remember(storage, reference) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(storage, reference) {
        if (storage != null && reference != null) result.value = withContext(Dispatchers.IO) { storage.load(reference) }
    }
    return result.value
}

@Composable
internal fun rememberKnownServiceIconBitmap(
    cache: TvKnownServiceIconCache?,
    serviceId: String?,
): Bitmap? {
    val result = remember(cache, serviceId) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(cache, serviceId) {
        if (cache != null && serviceId != null) {
            result.value = cache.load(serviceId)
        }
    }
    return result.value
}

@Composable
internal fun CustomIconControls(
    state: TvIconEditState,
    storage: TvCustomIconStorage?,
    defaultIconRes: Int = R.drawable.ic_tv_app_generic,
    defaultIconBitmap: Bitmap? = null,
) {
    val scope = rememberCoroutineScope()
    val saved = rememberCustomIconBitmap(storage, state.reference)
    val preview = state.draft?.preview ?: saved
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && storage != null) {
            state.loading = true; state.error = null
            scope.launch {
                try {
                    when (val imported = withContext(Dispatchers.IO) { storage.prepare(uri) }) {
                        is TvIconImportResult.Ready -> { state.draft = imported.draft; state.reset = false }
                        is TvIconImportResult.Rejected -> state.error = imported.message
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                finally { state.loading = false }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StreamingIconCradle(defaultIconRes,
            if (preview == null) "Default icon preview" else if (state.draft != null) "Selected icon preview" else "Saved icon preview", 58, preview ?: defaultIconBitmap)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("APP ICON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(if (state.loading) "Reading image…" else "PNG, JPEG or WebP. Stored locally after saving.", fontSize = 11.sp, color = Muted)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            state.error = null
            try { picker.launch(TvCustomIconStorage.MIME_TYPES.toTypedArray()) }
            catch (_: Exception) { state.error = "No document picker is available." }
        }, enabled = storage != null && !state.loading && !state.saving, modifier = Modifier.weight(1f)) {
            Text("ADD ICON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = { state.draft = null; state.reset = true; state.error = null },
            enabled = !state.loading && !state.saving && (state.draft != null || (!state.reset && state.originalReference != null)),
            modifier = Modifier.weight(1f)) { Text("RESET ICON", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
}

@Composable
internal fun CustomAppIconDialog(shortcut: TvAppShortcut, storage: TvCustomIconStorage,
    onDismiss: () -> Unit, onSave: (TvIconDraft?) -> Unit, defaultIconRes: Int = R.drawable.ic_tv_app_generic, defaultIconBitmap: Bitmap? = null) {
    val state = remember(shortcut.id, shortcut.iconReference) { TvIconEditState(shortcut.iconReference) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!state.saving) onDismiss() },
        title = { Text("Custom app icon", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(shortcut.displayName, fontWeight = FontWeight.SemiBold)
                Text("Changes only the icon. Your name, launch link and position stay unchanged.", fontSize = 12.sp, color = Muted)
                CustomIconControls(state, storage, defaultIconRes, defaultIconBitmap)
            }
        },
        confirmButton = {
            TextButton(enabled = state.changed && !state.loading && !state.saving, onClick = {
                val draft = state.draft
                state.saving = true; state.error = null
                scope.launch {
                    try { withContext(Dispatchers.IO) { onSave(draft) }; onDismiss() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { state.error = "Could not save the icon. Your existing shortcut is unchanged." }
                    finally { state.saving = false }
                }
            }) { Text(if (state.saving) "SAVING…" else "SAVE ICON") }
        },
        dismissButton = { TextButton(enabled = !state.saving, onClick = onDismiss) { Text("CANCEL") } },
    )
}
