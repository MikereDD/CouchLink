package dev.typezero.couchlink.remote.hid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import dev.typezero.couchlink.remote.MainActivity
import dev.typezero.couchlink.remote.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Owns Bluetooth HID registration while CouchLink is backgrounded.
 *
 * The Activity may be paused, recreated, or removed from Recents without
 * unregistering the HID application. START_STICKY lets Android recreate the
 * service after process pressure; BluetoothHidController then reconnects to
 * the last selected Windows host.
 */
class BluetoothHidService : Service() {
    private val serviceJob: Job = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private lateinit var controller: BluetoothHidController
    private var initialized = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        controller = BluetoothHidRuntime.controller(applicationContext)

        // A connectedDevice foreground service requires BLUETOOTH_CONNECT on
        // Android 14+. If the OS re-creates this service (START_STICKY) after the
        // permission was revoked, promoting to foreground would throw. Bail out
        // cleanly; the Activity restarts the service once permission is restored.
        if (!controller.hasRequiredPermissions()) {
            stopSelf()
            return
        }
        try {
            startForeground(NOTIFICATION_ID, buildNotification(controller.state.value))
        } catch (exception: Exception) {
            stopSelf()
            return
        }
        initialized = true
        serviceScope.launch {
            controller.state.collectLatest { state ->
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(state))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!initialized || !controller.hasRequiredPermissions()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        controller.start()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        controller.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bluetooth input",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps CouchLink connected as a Bluetooth keyboard and mouse."
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(state: BluetoothHidController.State): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val detail = when {
            state.connected -> "Connected to ${state.connectedHost?.name ?: "Windows"}"
            state.connecting -> "Reconnecting to Windows…"
            state.registered -> "Ready to reconnect"
            else -> "Preparing Bluetooth input…"
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("CouchLink Bluetooth input")
            .setContentText(detail)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "couchlink_bluetooth_hid"
        private const val NOTIFICATION_ID = 2103

        fun start(context: Context) {
            val intent = Intent(context, BluetoothHidService::class.java)
            context.startForegroundService(intent)
        }
    }
}
