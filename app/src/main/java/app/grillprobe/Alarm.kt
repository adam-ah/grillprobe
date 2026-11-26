package app.grillprobe

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Alarm {
    const val CHANNEL_STATUS = "status"
    private const val CHANNEL_ALARM = "alarm"
    private const val NOTIFICATION_ID = 2
    private const val AUTO_STOP_MS = 3 * 60_000L
    private const val VIBRATE_ON_MS = 600L
    private const val VIBRATE_OFF_MS = 400L
    private val VIBRATION = longArrayOf(0, VIBRATE_ON_MS, VIBRATE_OFF_MS)

    private val main = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var appContext: Context? = null
    private val autoStop = Runnable { appContext?.let { stop(it) } }

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val status =
            NotificationChannel(
                CHANNEL_STATUS,
                context.getString(R.string.channel_status),
                NotificationManager.IMPORTANCE_LOW,
            )
        val alarm =
            NotificationChannel(
                CHANNEL_ALARM,
                context.getString(R.string.target_reached),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
        manager.createNotificationChannels(listOf(status, alarm))
    }

    fun start(
        context: Context,
        celsius: Double,
    ) {
        val app = context.applicationContext
        appContext = app
        if (Store.isAlarmActive) return
        Store.isAlarmActive = true
        ringtone = RingtoneManager.getRingtone(app, alarmSound())?.apply {
            audioAttributes =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            isLooping = true
            play()
        }
        vibrator =
            app.getSystemService(VibratorManager::class.java).defaultVibrator.also {
                it.vibrate(VibrationEffect.createWaveform(VIBRATION, 0))
            }
        app.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(app, celsius))
        main.removeCallbacks(autoStop)
        main.postDelayed(autoStop, AUTO_STOP_MS)
        Store.changed()
    }

    fun stop(context: Context) {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        main.removeCallbacks(autoStop)
        if (Store.isAlarmActive) {
            Store.isAlarmActive = false
            Store.changed()
        }
    }

    private fun alarmSound() =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun notification(
        app: Context,
        celsius: Double,
    ): Notification {
        val open =
            PendingIntent.getActivity(
                app,
                0,
                Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val stop =
            PendingIntent.getBroadcast(app, 0, Intent(app, AlarmStopReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
        val noIcon: Icon? = null
        return Notification
            .Builder(app, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_probe)
            .setContentTitle(app.getString(R.string.target_reached))
            .setContentText(Formatter(app.resources).temperature(celsius, Store.unit))
            .setContentIntent(open)
            .setDeleteIntent(stop)
            .addAction(Notification.Action.Builder(noIcon, app.getString(R.string.stop_alarm), stop).build())
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
    }
}

class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Store.init(context)
        Alarm.stop(context)
    }
}
