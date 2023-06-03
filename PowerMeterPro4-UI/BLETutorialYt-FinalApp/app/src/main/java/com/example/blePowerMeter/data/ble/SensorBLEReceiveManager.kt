package com.example.blePowerMeter.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.blePowerMeter.data.ConnectionState
import com.example.blePowerMeter.data.SensorResult
import com.example.blePowerMeter.data.SensorReceiveManager
import com.example.blePowerMeter.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@SuppressLint("MissingPermission")
class SensorBLEReceiveManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter, private val context: Context
) : SensorReceiveManager {

    val DEVICE_NAME = "Power Meter"
    val SENSOR_SERVICE_UUID = "19b10010-e8f2-537e-4f6c-d104768a1214"
    val FORCE_CHARACTERISTICS_UUID = "19b10012-e8f2-537e-4f6c-d104768a1214"
    val ANGLE_CHARACTERISTICS_UUID = "19b10013-e8f2-537e-4f6c-d104768a1214"
    val CADENCE_CHARACTERISTICS_UUID = "19b10011-e8f2-537e-4f6c-d104768a1214"

    override val data: MutableSharedFlow<Resource<SensorResult>> = MutableSharedFlow()

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings =
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

    private var gatt: BluetoothGatt? = null

    private var isScanning = false

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val scanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.name == DEVICE_NAME) {
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Connecting to device..."))
                }
                if (isScanning) {
                    result.device.connectGatt(context, false, gattCallback)
                    isScanning = false
                    bleScanner.stopScan(this)
                }
            }
        }
    }

    private var currentConnectionAttempt = 1
    private var MAXIMUM_CONNECTION_ATTEMPTS = 5

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    coroutineScope.launch {
                        data.emit(Resource.Loading(message = "Discovering Services..."))
                    }
                    gatt.discoverServices()
                    this@SensorBLEReceiveManager.gatt = gatt
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    coroutineScope.launch {
                        data.emit(
                            Resource.Success(
                                data = SensorResult(
                                    0f, 0f, 0f, ConnectionState.Disconnected
                                )
                            )
                        )
                    }
                    gatt.close()
                }
            } else {
                gatt.close()
                currentConnectionAttempt += 1
                coroutineScope.launch {
                    data.emit(
                        Resource.Loading(
                            message = "Attempting to connect $currentConnectionAttempt/$MAXIMUM_CONNECTION_ATTEMPTS"
                        )
                    )
                }
                if (currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS) {
                    startReceiving()
                } else {
                    coroutineScope.launch {
                        data.emit(Resource.Error(errorMessage = "Could not connect to BLE device"))
                    }
                }
            }
        }


        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                printGattTable()
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Adjusting MTU space..."))
                }
                gatt.requestMtu(23)
            }
        }


        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            val characteristics = findCharacteristics(
                FORCE_CHARACTERISTICS_UUID, ANGLE_CHARACTERISTICS_UUID, CADENCE_CHARACTERISTICS_UUID
            )
            if (characteristics.isNullOrEmpty()) {
                coroutineScope.launch {
                    data.emit(Resource.Error(errorMessage = "Could not find sensor publisher"))
                }
                return
            }
            characteristics.forEach { characteristic ->
                enableNotification(characteristic)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            var force = 0
            var angle = 0
            var cadence = 0
            with(characteristic) {
                when (uuid) {
                    UUID.fromString(FORCE_CHARACTERISTICS_UUID) -> {
                        // Handle force characteristic notification
                        val rawData = characteristic.value
                        force =
                            (rawData[0].toInt() and 0xFF) or ((rawData[1].toInt() and 0xFF) shl 8)
                        Log.d("force", "$force")
                    }
                    UUID.fromString(ANGLE_CHARACTERISTICS_UUID) -> {
                        // Handle angle characteristic notification
                        val rawData = characteristic.value
                        angle =
                            (rawData[0].toInt() and 0xFF) or ((rawData[1].toInt() and 0xFF) shl 8)
                        Log.d("angle", "$angle")
                    }
                    UUID.fromString(CADENCE_CHARACTERISTICS_UUID) -> {
                        // Handle cadence characteristic notification
                        val rawData = characteristic.value
                        cadence =
                            (rawData[0].toInt() and 0xFF) or ((rawData[1].toInt() and 0xFF) shl 8)
                        Log.d("cadence", "$cadence")
                    }
                    else -> Unit
                }
            }
            val sensorResult = SensorResult(
                force.toFloat(), angle.toFloat(), cadence.toFloat(), ConnectionState.Connected
            )
            coroutineScope.launch {
                data.emit(Resource.Success(data = sensorResult))
            }
        }



        // Other methods and helper functions...

    }


    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return

        }

        // Get the Client Characteristic Configuration Descriptor (CCCD) for the characteristic
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            // Set characteristic notification for the GATT server
            if (gatt?.setCharacteristicNotification(characteristic, true) == false) {
                Log.d("BLEReceiveManager", "set characteristics notification failed")
                return
            }
            // Write the CCCD descriptor with the payload to enable notifications or indications
            writeDescription(cccdDescriptor, payload)


        }

    }

    // Write the descriptor value to the GATT server
    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray) {

        gatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)

        } ?: error("Not connected to a BLE device!")

    }

    // Find the BluetoothGattCharacteristic based on the service UUID and characteristic UUID
    private fun findCharacteristics(vararg characteristicsUUIDs: String): List<BluetoothGattCharacteristic>? {
        return gatt?.services?.flatMap { service ->
            characteristicsUUIDs.mapNotNull { characteristicsUUID ->
                service.characteristics?.find { characteristic ->
                    characteristic.uuid.toString() == characteristicsUUID
                }
            }
        }
    }


    // Start receiving sensor data by initiating the BLE device scan
    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Scanning BLE devices..."))
        }
        isScanning = true
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    override fun reconnect() {
        gatt?.connect()
    }

    override fun disconnect() {
        gatt?.disconnect()
    }


    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        val characteristic = findCharacteristics(SENSOR_SERVICE_UUID, FORCE_CHARACTERISTICS_UUID)
        if (characteristic != null) {
            return
        }
        gatt?.close()
    }

    // Disable notifications for the characteristic and write the CCCD descriptor
    private fun disconnectCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if (gatt?.setCharacteristicNotification(characteristic, false) == false) {
                Log.d("SensorReceiveManager", "set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        }
    }

    // Stop receiving sensor data by stopping the BLE device scan
    override fun stopReceiving() {
        if (isScanning) {
            isScanning = false
            bleScanner.stopScan(scanCallback)
        }
    }


}