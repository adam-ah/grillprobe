package app.grillprobe

data class ScreenState(
    val status: String,
    val isConnected: Boolean,
    val isServiceRunning: Boolean,
    val battery: Int?,
    val unit: TemperatureUnit,
    val target: Double?,
    val preset: Preset?,
    val samples: List<Sample>,
    val prediction: Prediction?,
    val isAlarmActive: Boolean,
) {
    val temperature: Double? get() = samples.lastOrNull()?.second

    val isReached: Boolean get() = temperature?.let { current -> target?.let { current >= it } } == true

    val reachedAt: Long? get() = target?.let { goal -> samples.firstOrNull { it.second >= goal }?.first }

    companion object {
        fun current(): ScreenState {
            val samples = Store.samples()
            return ScreenState(
                status = Store.status,
                isConnected = Store.isConnected,
                isServiceRunning = ProbeService.instance != null,
                battery = Store.battery,
                unit = Store.unit,
                target = Store.target,
                preset = Store.preset,
                samples = samples,
                prediction = Predictor.predict(samples, Store.target),
                isAlarmActive = Store.isAlarmActive,
            )
        }
    }
}
