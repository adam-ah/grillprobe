package app.grillprobe

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager

class MainActivity :
    Activity(),
    MainScreen.Actions {
    private val screen by lazy { MainScreen(this, this) }
    private var askedNotifications = false
    private val onStoreChange: () -> Unit = { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(screen.root)
        if (ProbeService.instance == null) connect()
    }

    override fun onResume() {
        super.onResume()
        Store.addListener(onStoreChange)
        render()
    }

    override fun onPause() {
        Store.removeListener(onStoreChange)
        super.onPause()
    }

    override fun connect() {
        val missing = Permissions.missing(this, includeNotifications = !askedNotifications)
        when {
            missing.isNotEmpty() -> {
                askedNotifications = true
                requestPermissions(missing.toTypedArray(), PERMISSION_REQUEST)
            }
            else -> startMonitoring()
        }
    }

    override fun disconnect() {
        stopService(Intent(this, ProbeService::class.java))
    }

    override fun newCook() {
        AlertDialog
            .Builder(this)
            .setMessage(R.string.new_cook_confirm)
            .setPositiveButton(R.string.new_cook) { _, _ -> Store.reset() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun testAlarm() {
        Alarm.start(this, Store.samples().lastOrNull()?.second ?: 0.0)
    }

    override fun stopAlarm() {
        Alarm.stop(this)
    }

    override fun toggleUnit() {
        Store.setUnit(Store.unit.other())
    }

    override fun selectPreset(preset: Preset) {
        Store.setTarget(preset.celsius.toDouble(), preset)
    }

    override fun editTarget() {
        TargetDialog.show(this, Store.target, Store.unit) { Store.setTarget(it) }
    }

    override fun stepTarget(delta: Int) {
        Store.setTarget((Store.target ?: DEFAULT_TARGET) + Store.unit.stepInCelsius(delta))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Permissions.hasRequired(this)) connect() else Store.setStatus(getString(R.string.status_permission))
    }

    private fun startMonitoring() {
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        val canConnect = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        when {
            adapter == null -> Store.setStatus(getString(R.string.status_no_bluetooth))
            !adapter.isEnabled && canConnect -> startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> {
                startForegroundService(Intent(this, ProbeService::class.java))
                render()
            }
        }
    }

    private fun render() {
        screen.render(ScreenState.current())
    }

    private companion object {
        const val PERMISSION_REQUEST = 1
        const val DEFAULT_TARGET = 60.0
    }
}
