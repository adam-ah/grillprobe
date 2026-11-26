package app.grillprobe

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IGrillProtocolTest {
    @Test
    fun decodesLittleEndianTemperature() {
        assertEquals(25, IGrillProtocol.decodeTemperature(bytes(0x19, 0x00)))
        assertEquals(300, IGrillProtocol.decodeTemperature(bytes(0x2C, 0x01)))
    }

    @Test
    fun decodesNegativeTemperature() {
        assertEquals(-5, IGrillProtocol.decodeTemperature(bytes(0xFB, 0xFF)))
    }

    @Test
    fun unpluggedProbeDecodesToNull() {
        assertNull(IGrillProtocol.decodeTemperature(bytes(0x30, 0xF8)))
    }

    @Test
    fun shortPayloadDecodesToNull() {
        assertNull(IGrillProtocol.decodeTemperature(bytes(0x19)))
        assertNull(IGrillProtocol.decodeBattery(ByteArray(0)))
    }

    @Test
    fun decodesBatteryAsUnsignedByte() {
        assertEquals(60, IGrillProtocol.decodeBattery(bytes(0x3C)))
        assertEquals(200, IGrillProtocol.decodeBattery(bytes(0xC8)))
    }

    @Test
    fun recognisesIGrillNames() {
        assertTrue(IGrillProtocol.isIGrill("iGrill_MINI02-68AC"))
        assertTrue(IGrillProtocol.isIGrill("IGRILL_V3"))
        assertFalse(IGrillProtocol.isIGrill("Galaxy Buds"))
        assertFalse(IGrillProtocol.isIGrill(null))
    }

    @Test
    fun showsFriendlyModelNames() {
        assertEquals("iGrill Mini", IGrillProtocol.friendlyName("iGrill_MINI02-68AC"))
        assertEquals("iGrill 2", IGrillProtocol.friendlyName("iGrill_V2-1234"))
        assertEquals("iGrill 3", IGrillProtocol.friendlyName("iGrill_V3"))
        assertEquals("Weird Probe", IGrillProtocol.friendlyName("Weird_Probe"))
    }

    @Test
    fun appChallengeIsSixteenZeroBytes() {
        assertArrayEquals(ByteArray(16), IGrillProtocol.appChallenge())
    }

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
}
