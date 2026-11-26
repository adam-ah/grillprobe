package app.grillprobe

import java.util.Locale
import java.util.UUID

object IGrillProtocol {
    val APP_CHALLENGE: UUID = UUID.fromString("64ac0002-4a4b-4b58-9f37-94d3c52ffdf7")
    val DEVICE_CHALLENGE: UUID = UUID.fromString("64ac0003-4a4b-4b58-9f37-94d3c52ffdf7")
    val DEVICE_RESPONSE: UUID = UUID.fromString("64ac0004-4a4b-4b58-9f37-94d3c52ffdf7")
    val PROBE: UUID = UUID.fromString("06ef0002-2e06-4b79-9e33-fce2c42805ec")
    val PROBES: List<UUID> =
        listOf(
            PROBE,
            UUID.fromString("06ef0004-2e06-4b79-9e33-fce2c42805ec"),
            UUID.fromString("06ef0006-2e06-4b79-9e33-fce2c42805ec"),
            UUID.fromString("06ef0008-2e06-4b79-9e33-fce2c42805ec"),
        )
    val BATTERY: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    private const val NAME_PREFIX = "igrill"
    private const val CHALLENGE_SIZE = 16
    private const val NO_PROBE_BELOW = -1000
    private const val BYTE_MASK = 0xFF
    private const val BITS_PER_BYTE = 8

    fun appChallenge(): ByteArray = ByteArray(CHALLENGE_SIZE)

    fun isIGrill(name: String?): Boolean = name?.lowercase(Locale.ROOT)?.startsWith(NAME_PREFIX) == true

    fun decodeTemperature(value: ByteArray): Int? {
        if (value.size < 2) return null
        val raw = ((value[0].toInt() and BYTE_MASK) or (value[1].toInt() shl BITS_PER_BYTE)).toShort().toInt()
        return raw.takeIf { it > NO_PROBE_BELOW }
    }

    fun decodeBattery(value: ByteArray): Int? = value.firstOrNull()?.toInt()?.and(BYTE_MASK)

    fun friendlyName(name: String): String {
        val model = name.substringAfter('_', "").substringBefore('-').uppercase(Locale.ROOT)
        return when {
            model.startsWith("MINI") -> "iGrill Mini"
            model.startsWith("V2") -> "iGrill 2"
            model.startsWith("V3") -> "iGrill 3"
            else -> name.replace('_', ' ')
        }
    }
}
