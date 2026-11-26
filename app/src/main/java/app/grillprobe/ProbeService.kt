package app.grillprobe

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class ProbeService :
    Service(),
    IGrillScanner.Listener,
    GattSession.Listener {
    private val main = Handler(Looper.getMainLooper())
    private val scanner by lazy { IGrillScanner(this, this) }
    private val format by lazy { Formatter(resources) }
    private var session: GattSession? = null
    private var isStopped = false
    private var connectedText = ""
    private var failures = 0
    private val onStoreChange: () -> Unit = { updateNotification() }
    private val rescan = Runnable { if (!isStopped && session == null) scanner.start() }

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        instance = this
        Store.addListener(onStoreChange)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        if (session == null && !scanner.isActive) scanner.start()
        return START_STICKY
    }

    override fun onDestroy() {
        isStopped = true
        main.removeCallbacksAndMessages(null)
        Store.removeListener(onStoreChange)
        scanner.stop()
        session?.close()
        session = null
        instance = null
        Alarm.stop(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Store.isConnected = false
        Store.battery = null
        if (failures < MAX_FAILURES) Store.setStatus(getString(R.string.status_stopped))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onFound(device: BluetoothDevice) {
        scanner.stop()
        session = GattSession(this, device, this).also { it.open() }
    }

    override fun onStatus(text: String) {
        Store.setStatus(text)
    }

    override fun onReady(
        name: String,
        address: String,
    ) {
        Store.setAddress(address)
        Store.deviceName = name
        Store.isConnected = true
        failures = 0
        connectedText = IGrillProtocol.friendlyName(name)
        Store.setStatus(connectedText)
    }

    override fun onTemperature(raw: Int?) {
        if (raw == null) {
            Store.setStatus(getString(R.string.status_no_probe))
            return
        }
        if (Store.status != connectedText) Store.setStatus(connectedText)
        Store.add(raw.toDouble())
        checkAlarm()
    }

    override fun onBattery(percent: Int) {
        Store.battery = percent
        Store.changed()
    }

    override fun onEnded(reason: String) {
        session = null
        Store.isConnected = false
        main.removeCallbacks(rescan)
        failures++
        if (failures >= MAX_FAILURES) {
            Store.setStatus(getString(R.string.status_gave_up))
            stopSelf()
            return
        }
        Store.setStatus(reason)
        if (!isStopped) main.postDelayed(rescan, Backoff.delayMs(failures))
    }

    private fun checkAlarm() {
        val target = Store.target ?: return
        val celsius = Store.history.last().celsius
        if (celsius >= target && Store.isAlarmArmed) {
            Store.isAlarmArmed = false
            Alarm.start(this, celsius)
        } else if (celsius < target - ALARM_REARM_MARGIN) {
            Store.isAlarmArmed = true
        }
    }

    private fun buildNotification(): Notification {
        val samples = Store.samples()
        val target = Store.target
        val prediction = Predictor.predict(samples, target)
        val current = samples.lastOrNull()?.second
        val title =
            when {
                !Store.isConnected || current == null -> Store.status
                target == null -> format.temperature(current, Store.unit)
                else ->
                    getString(
                        R.string.notification_title,
                        format.temperature(current, Store.unit),
                        format.wholeTemperature(target, Store.unit),
                    )
            }
        val best = prediction?.etas?.best
        val text =
            when {
                prediction != null && best != null ->
                    getString(
                        R.string.notification_eta,
                        format.duration(best),
                        format.clock(prediction.nowMs + (best * MS_PER_MIN).toLong()),
                    )
                Store.isConnected -> getString(R.string.monitoring)
                else -> getString(R.string.app_name)
            }
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val disconnect =
            PendingIntent.getService(
                this,
                0,
                Intent(this, ProbeService::class.java).setAction(ACTION_DISCONNECT),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val disconnectAction =
            Notification.Action
                .Builder(Icon.createWithResource(this, R.drawable.ic_power), getString(R.string.disconnect), disconnect)
                .build()
        return Notification
            .Builder(this, Alarm.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_probe)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .addAction(disconnectAction)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        if (isStopped) return
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val ACTION_DISCONNECT = "app.grillprobe.DISCONNECT"
        private const val MAX_FAILURES = 8
        private const val ALARM_REARM_MARGIN = 2.0
        private const val MS_PER_MIN = 60_000.0

        var instance: ProbeService? = null
            private set
    }
}
