package app.grillprobe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper

@SuppressLint("MissingPermission")
class IGrillScanner(
    private val context: Context,
    private val listener: Listener,
) {
    interface Listener {
        fun onFound(device: BluetoothDevice)

        fun onStatus(text: String)
    }

    var isActive = false
        private set

    private val main = Handler(Looper.getMainLooper())
    private var attempts = 0
    private var isStopped = false
    private val timeout = Runnable { onTimeout() }
    private val restart = Runnable { start() }
    private val callback =
        object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult,
            ) {
                main.post { onResult(result) }
            }

            override fun onScanFailed(errorCode: Int) {
                main.post { onFailed(errorCode) }
            }
        }

    fun start() {
        isStopped = false
        if (isActive) return
        val scanner = context.getSystemService(BluetoothManager::class.java)?.adapter?.takeIf {
            it.isEnabled
        }?.bluetoothLeScanner
        if (scanner == null) {
            listener.onStatus(context.getString(R.string.status_bluetooth_off))
            retry(BLUETOOTH_OFF_RETRY_MS)
            return
        }
        val address = Store.deviceAddress
        val filters = address?.takeIf {
            attempts % NAME_SCAN_EVERY != NAME_SCAN_EVERY - 1
        }?.let { listOf(ScanFilter.Builder().setDeviceAddress(it).build()) }
        attempts++
        isActive = true
        listener.onStatus(context.getString(if (address == null) R.string.status_scanning else R.string.status_looking))
        scanner.startScan(
            filters,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            callback
        )
        main.postDelayed(timeout, SCAN_TIMEOUT_MS)
    }

    fun stop() {
        isStopped = true
        main.removeCallbacks(restart)
        halt()
    }

    private fun halt() {
        main.removeCallbacks(timeout)
        if (!isActive) return
        isActive = false
        try {
            context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner?.stopScan(callback)
        } catch (ignored: IllegalStateException) {
            listener.onStatus(context.getString(R.string.status_bluetooth_off))
        }
    }

    internal fun onResult(result: ScanResult) {
        if (!isActive) return
        val name = result.scanRecord?.deviceName ?: result.device.name
        if (result.device.address != Store.deviceAddress && !IGrillProtocol.isIGrill(name)) return
        halt()
        listener.onFound(result.device)
    }

    private fun onTimeout() {
        halt()
        listener.onStatus(context.getString(R.string.status_not_found))
        retry(NOT_FOUND_RETRY_MS)
    }

    internal fun onFailed(errorCode: Int) {
        isActive = false
        main.removeCallbacks(timeout)
        listener.onStatus(context.getString(R.string.status_scan_failed, errorCode))
        retry(BLUETOOTH_OFF_RETRY_MS)
    }

    private fun retry(delayMs: Long) {
        if (isStopped) return
        main.removeCallbacks(restart)
        main.postDelayed(restart, delayMs)
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 20_000L
        const val NOT_FOUND_RETRY_MS = 3_000L
        const val BLUETOOTH_OFF_RETRY_MS = 5_000L
        const val NAME_SCAN_EVERY = 3
    }
}
