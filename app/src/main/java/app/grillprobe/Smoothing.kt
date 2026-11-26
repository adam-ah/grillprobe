package app.grillprobe

fun centeredMovingAverage(
    values: DoubleArray,
    radius: Int,
): DoubleArray {
    val prefix = DoubleArray(values.size + 1)
    values.forEachIndexed { i, value -> prefix[i + 1] = prefix[i] + value }
    return DoubleArray(values.size) { i ->
        val half = minOf(radius, i, values.size - 1 - i)
        (prefix[i + half + 1] - prefix[i - half]) / (2 * half + 1)
    }
}
