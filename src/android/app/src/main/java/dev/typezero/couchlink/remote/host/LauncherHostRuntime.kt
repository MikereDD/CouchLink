package dev.typezero.couchlink.remote.host

import android.content.Context

/**
 * Process-wide launcher host client, mirroring BluetoothHidRuntime. Using a single
 * long-lived instance (rather than one created per Activity) keeps its coroutine
 * scope from leaking across configuration changes and preserves the host session
 * across rotation.
 */
internal object LauncherHostRuntime {
    @Volatile
    private var instance: LauncherHostClient? = null

    fun client(context: Context): LauncherHostClient =
        instance ?: synchronized(this) {
            instance ?: LauncherHostClient(context.applicationContext).also { instance = it }
        }
}
