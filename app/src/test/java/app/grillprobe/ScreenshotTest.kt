package app.grillprobe

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w384dp-h832dp-xxhdpi")
class ScreenshotTest {
    private val context: Application get() = ApplicationProvider.getApplicationContext()
    private var service: ServiceController<ProbeService>? = null

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Budapest"))
        Store.init(context)
        Store.reset()
        Store.setUnit(TemperatureUnit.CELSIUS)
        Store.isConnected = false
        Store.isAlarmActive = false
        Store.battery = null
    }

    @After
    fun tearDown() {
        service?.destroy()
        service = null
        Store.reset()
        Store.setUnit(TemperatureUnit.CELSIUS)
    }

    @Test
    fun firstLaunch() {
        Store.setTarget(null)
        Store.setStatus(context.getString(R.string.status_not_connected))
        capture("01-first-launch")
    }

    @Test
    fun heatingWithCurveEstimate() {
        connected(Cooks.series(45.0, Cooks.newton(6.0, 112.0, 0.02)), Preset.BEEF_BRISKET)
        capture("02-heating")
    }

    @Test
    fun earlyLinearEstimate() {
        connected(Cooks.series(9.0, Cooks.linear(8.0, 2.4)), Preset.BEEF_MEDIUM_RARE)
        capture("03-early")
    }

    @Test
    fun levellingOffBelowTarget() {
        connected(Cooks.series(50.0, Cooks.newton(10.0, 68.0, 0.05)), Preset.CHICKEN_BREAST)
        capture("04-plateau")
    }

    @Test
    fun coolingAfterTheCook() {
        val rise = Cooks.series(30.0, Cooks.newton(8.0, 95.0, 0.05))
        val peak = rise.last()
        val fall =
            Cooks
                .series(8.0, Cooks.linear(peak.second, -0.6))
                .map { (time, value) -> time + (peak.first - Cooks.START_MS) to value }
        connected(rise + fall.drop(1), Preset.PORK_TENDERLOIN)
        capture("05-cooling")
    }

    @Test
    fun targetReachedAlarm() {
        connected(Cooks.series(40.0, Cooks.newton(8.0, 90.0, 0.05)), Preset.CHICKEN_BREAST)
        Store.isAlarmActive = true
        capture("06-alarm")
    }

    @Test
    fun pickerShowsCutsForTheChosenAnimal() {
        connected(Cooks.series(20.0, Cooks.newton(6.0, 112.0, 0.02)), Preset.BEEF_MEDIUM)
        capture("07-picker-open") { activity -> tap(activity, context.getString(R.string.animal_beef)) }
    }

    @Test
    fun fahrenheitDisplay() {
        connected(Cooks.series(45.0, Cooks.newton(6.0, 112.0, 0.02)), Preset.PORK_PULLED)
        Store.setUnit(TemperatureUnit.FAHRENHEIT)
        capture("08-fahrenheit")
    }

    private fun connected(
        samples: List<Sample>,
        preset: Preset,
    ) {
        service = Robolectric.buildService(ProbeService::class.java).create()
        Store.history.addAll(samples.map { Store.Reading(it.first, it.second) })
        Store.setTarget(preset.celsius.toDouble(), preset)
        Store.isConnected = true
        Store.battery = BATTERY
        Store.setStatus(IGrillProtocol.friendlyName("iGrill_MINI02-68AC"))
    }

    private fun tap(
        activity: Activity,
        description: String,
    ) {
        val found = ArrayList<View>()
        activity.window.decorView.findViewsWithText(found, description, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        found.first().performClick()
    }

    private fun capture(
        name: String,
        interact: (Activity) -> Unit = {},
    ) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                shadowOf(Looper.getMainLooper()).idle()
                interact(activity)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(SETTLE_MS))
                activity.window.decorView.captureRoboImage("${System.getProperty("screenshots.dir")}/$name.png")
            }
        }
    }

    private companion object {
        const val BATTERY = 60
        const val SETTLE_MS = 600L
    }
}
