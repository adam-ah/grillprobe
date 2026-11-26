package app.grillprobe

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import java.io.File

object Store {
    class Reading(
        val time: Long,
        val celsius: Double,
    )

    private const val RESUME_MS = 30 * 60_000L
    private const val KEY_TARGET = "target"
    private const val KEY_PRESET = "preset"
    private const val KEY_UNIT = "unit"
    private const val KEY_ADDRESS = "address"

    val history = ArrayList<Reading>()
    var status = ""
        private set
    var isConnected = false
    var battery: Int? = null
    var deviceName: String? = null
    var isAlarmActive = false
    var isAlarmArmed = true
    var target: Double? = null
        private set
    var preset: Preset? = null
        private set
    var unit = TemperatureUnit.CELSIUS
        private set
    var deviceAddress: String? = null
        private set

    private val listeners = mutableListOf<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())
    private var prefs: SharedPreferences? = null
    private var file: File? = null

    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val preferences = app.getSharedPreferences("grill", Context.MODE_PRIVATE)
        prefs = preferences
        status = app.getString(R.string.status_not_connected)
        target = preferences.getString(KEY_TARGET, null)?.toDoubleOrNull()
        preset = Preset.named(preferences.getString(KEY_PRESET, null))
        unit = TemperatureUnit.fromCode(preferences.getString(KEY_UNIT, null))
        deviceAddress = preferences.getString(KEY_ADDRESS, null)
        val cook = File(app.filesDir, "cook.csv")
        file = cook
        if (cook.exists() && System.currentTimeMillis() - cook.lastModified() < RESUME_MS) load(cook) else cook.delete()
        Alarm.createChannels(app)
    }

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun changed() {
        if (Looper.myLooper() == Looper.getMainLooper()) notifyListeners() else main.post { notifyListeners() }
    }

    fun add(celsius: Double) {
        val time = System.currentTimeMillis()
        history.add(Reading(time, celsius))
        file?.appendText("$time,$celsius\n")
        changed()
    }

    fun samples(): List<Sample> = history.map { it.time to it.celsius }

    fun setTarget(
        celsius: Double?,
        chosen: Preset? = null,
    ) {
        target = celsius
        preset = chosen
        isAlarmArmed = true
        prefs
            ?.edit()
            ?.putString(KEY_TARGET, celsius?.toString())
            ?.putString(KEY_PRESET, chosen?.name)
            ?.apply()
        changed()
    }

    fun setUnit(value: TemperatureUnit) {
        unit = value
        prefs?.edit()?.putString(KEY_UNIT, value.code)?.apply()
        changed()
    }

    fun setAddress(address: String) {
        deviceAddress = address
        prefs?.edit()?.putString(KEY_ADDRESS, address)?.apply()
    }

    fun setStatus(text: String) {
        status = text
        changed()
    }

    fun reset() {
        history.clear()
        file?.delete()
        isAlarmArmed = true
        changed()
    }

    private fun notifyListeners() {
        listeners.toList().forEach { it() }
    }

    private fun load(cook: File) {
        cook.forEachLine { line ->
            val parts = line.split(',')
            val time = parts.getOrNull(0)?.toLongOrNull()
            val celsius = parts.getOrNull(1)?.toDoubleOrNull()
            if (time != null && celsius != null) history.add(Reading(time, celsius))
        }
    }
}
