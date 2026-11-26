package app.grillprobe

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ProbeServiceTest {
    private val context: Application get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        Store.init(context)
    }

    @Test
    fun disconnectActionStopsTheService() {
        val intent = Intent(context, ProbeService::class.java).setAction("app.grillprobe.DISCONNECT")
        val controller = Robolectric.buildService(ProbeService::class.java, intent).create().startCommand(0, 1)
        assertTrue(shadowOf(controller.get()).isStoppedBySelf)
        controller.destroy()
    }

    @Test
    fun destroyingReleasesEverything() {
        val controller = Robolectric.buildService(ProbeService::class.java).create()
        Store.isConnected = true
        Store.battery = 60
        Store.isAlarmActive = true
        controller.destroy()
        assertNull(ProbeService.instance)
        assertFalse(Store.isConnected)
        assertFalse(Store.isAlarmActive)
        assertNull(Store.battery)
        assertEquals(context.getString(R.string.status_stopped), Store.status)
    }
}
