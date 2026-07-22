package dev.typezero.couchlink.remote.hid

import android.content.Context

/** Process-wide HID controller shared by the foreground service and UI. */
internal object BluetoothHidRuntime {
    @Volatile
    private var instance: BluetoothHidController? = null

    fun controller(context: Context): BluetoothHidController =
        instance ?: synchronized(this) {
            instance ?: BluetoothHidController(context.applicationContext).also {
                instance = it
            }
        }
}
