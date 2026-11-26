package app.grillprobe

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.UUID

@SuppressLint("MissingPermission")
class GattSession(
    private val context: Context,
    private val device: BluetoothDevice,
    private val listener: Listener,
) {
    interface Listener {
        fun onStatus(text: String)

        fun onReady(
            name: String,
            address: String,
        )

        fun onTemperature(raw: Int?)

        fun onBattery(percent: Int)

        fun onEnded(reason: String)
    }

    private val main = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var characteristics = emptyMap<UUID, BluetoothGattCharacteristic>()
    private var authAttempts = 0
    private var isAuthenticated = false
    private var isClosed = false
    private var lastBatteryMs = 0L
    private var probes = listOf(IGrillProtocol.PROBE)
    private var probeIndex = 0
    private val poll = Runnable { if (isAuthenticated && !read(probes[probeIndex])) scheduleNext() }
    private val connectTimeout = Runnable { end(context.getString(R.string.status_timeout)) }
    private val setupTimeout = Runnable { if (!isAuthenticated) end(context.getString(R.string.status_auth_failed)) }
    private val beginAuth = Runnable { if (!isAuthenticated && !isClosed) startAuth() }
    private val stalled = Runnable { end(context.getString(R.string.status_auth_failed)) }
    private val callback =
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                g: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                main.post { if (g == gatt) onConnectionChange(g, newState) }
            }

            override fun onServicesDiscovered(
                g: BluetoothGatt,
                status: Int,
            ) {
                main.post { if (g == gatt) onServices(g, status) }
            }

            override fun onCharacteristicRead(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                main.post { if (g == gatt) onRead(characteristic.uuid, value, status) }
            }

            @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
            override fun onCharacteristicRead(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                val value = characteristic.value?.copyOf() ?: ByteArray(0)
                main.post { if (g == gatt) onRead(characteristic.uuid, value, status) }
            }

            override fun onCharacteristicWrite(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                main.post { if (g == gatt) onWrite(characteristic.uuid, status) }
            }
        }

    fun open() {
        listener.onStatus(context.getString(R.string.status_connecting, device.name ?: device.address))
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
        main.postDelayed(connectTimeout, CONNECT_TIMEOUT_MS)
    }

    fun close() {
        isClosed = true
        main.removeCallbacksAndMessages(null)
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    private fun end(reason: String) {
        if (isClosed) return
        close()
        listener.onEnded(reason)
    }

    internal fun onConnectionChange(
        g: BluetoothGatt,
        newState: Int,
    ) {
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                main.removeCallbacks(connectTimeout)
                main.postDelayed(setupTimeout, SETUP_TIMEOUT_MS)
                listener.onStatus(context.getString(R.string.status_discovering))
                g.discoverServices()
            }
            BluetoothProfile.STATE_DISCONNECTED -> end(context.getString(R.string.status_disconnected))
            else -> Unit
        }
    }

    internal fun onServices(
        g: BluetoothGatt,
        status: Int,
    ) {
        val found = HashMap<UUID, BluetoothGattCharacteristic>()
        g.services.flatMap { it.characteristics }.forEach { found.putIfAbsent(it.uuid, it) }
        characteristics = found
        probes = IGrillProtocol.PROBES.filter { it in found }.ifEmpty { listOf(IGrillProtocol.PROBE) }
        probeIndex = 0
        when {
            status != BluetoothGatt.GATT_SUCCESS -> end(context.getString(R.string.status_disconnected))
            IGrillProtocol.APP_CHALLENGE !in found || IGrillProtocol.PROBE !in found ->
                end(context.getString(R.string.status_not_igrill))
            else -> {
                listener.onStatus(context.getString(R.string.status_authenticating))
                main.postDelayed(beginAuth, AUTH_START_DELAY_MS)
            }
        }
    }

    private fun startAuth() {
        val text =
            if (authAttempts == 0) {
                context.getString(R.string.status_authenticating)
            } else {
                context.getString(R.string.status_pairing, authAttempts + 1)
            }
        listener.onStatus(text)
        if (!write(IGrillProtocol.APP_CHALLENGE, IGrillProtocol.appChallenge())) retryAuth()
    }

    private fun retryAuth() {
        authAttempts++
        main.removeCallbacks(beginAuth)
        main.postDelayed(beginAuth, AUTH_RETRY_MS)
    }

    internal fun onWrite(
        id: UUID,
        status: Int,
    ) {
        main.removeCallbacks(stalled)
        if (status != BluetoothGatt.GATT_SUCCESS) {
            if (!isAuthenticated) retryAuth()
            return
        }
        when (id) {
            IGrillProtocol.APP_CHALLENGE -> if (!read(IGrillProtocol.DEVICE_CHALLENGE)) retryAuth()
            IGrillProtocol.DEVICE_RESPONSE -> {
                isAuthenticated = true
                main.removeCallbacks(setupTimeout)
                listener.onReady(device.name ?: device.address, device.address)
                main.post(poll)
            }
        }
    }

    internal fun onRead(
        id: UUID,
        value: ByteArray,
        status: Int,
    ) {
        main.removeCallbacks(stalled)
        when {
            status != BluetoothGatt.GATT_SUCCESS -> if (isAuthenticated) scheduleNext() else retryAuth()
            id == IGrillProtocol.DEVICE_CHALLENGE ->
                if (!write(IGrillProtocol.DEVICE_RESPONSE, value)) retryAuth()
            id in probes -> onProbe(value)
            id == IGrillProtocol.BATTERY -> {
                IGrillProtocol.decodeBattery(value)?.let { listener.onBattery(it) }
                scheduleNext()
            }
        }
    }

    private fun onProbe(value: ByteArray) {
        val celsius = IGrillProtocol.decodeTemperature(value)
        if (celsius == null) probeIndex = (probeIndex + 1) % probes.size
        listener.onTemperature(celsius)
        val now = System.currentTimeMillis()
        val batteryDue = device.bondState == BluetoothDevice.BOND_BONDED && now - lastBatteryMs > BATTERY_INTERVAL_MS
        if (batteryDue) lastBatteryMs = now
        if (!(batteryDue && read(IGrillProtocol.BATTERY))) scheduleNext()
    }

    @Suppress("DEPRECATION")
    private fun write(
        id: UUID,
        data: ByteArray,
    ): Boolean {
        val g = gatt ?: return false
        val characteristic = characteristics[id] ?: return false
        val sent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(characteristic, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
                    BluetoothStatusCodes.SUCCESS
            } else {
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                characteristic.value = data
                g.writeCharacteristic(characteristic)
            }
        if (sent) watchForReply()
        return sent
    }

    private fun read(id: UUID): Boolean {
        val characteristic = characteristics[id] ?: return false
        val sent = gatt?.readCharacteristic(characteristic) == true
        if (sent) watchForReply()
        return sent
    }

    private fun watchForReply() {
        main.removeCallbacks(stalled)
        main.postDelayed(stalled, if (isAuthenticated) READ_TIMEOUT_MS else HANDSHAKE_STEP_TIMEOUT_MS)
    }

    private fun scheduleNext() {
        main.removeCallbacks(poll)
        main.postDelayed(poll, POLL_MS)
    }

    private companion object {
        const val POLL_MS = 3_000L
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val SETUP_TIMEOUT_MS = 12_000L
        const val AUTH_START_DELAY_MS = 1_500L
        const val AUTH_RETRY_MS = 1_000L
        const val HANDSHAKE_STEP_TIMEOUT_MS = 3_000L
        const val READ_TIMEOUT_MS = 15_000L
        const val BATTERY_INTERVAL_MS = 60_000L
    }
}
