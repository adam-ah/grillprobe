package app.grillprobe

object Backoff {
    private const val BASE_MS = 2_000L
    private const val MAX_MS = 60_000L
    private const val MAX_DOUBLINGS = 5

    fun delayMs(failures: Int): Long {
        val doublings = (failures - 1).coerceIn(0, MAX_DOUBLINGS)
        return (BASE_MS shl doublings).coerceAtMost(MAX_MS)
    }
}
