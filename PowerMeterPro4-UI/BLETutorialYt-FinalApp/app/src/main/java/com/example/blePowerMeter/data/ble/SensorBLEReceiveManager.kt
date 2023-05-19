package com.example.blePowerMeter.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ContentValues.TAG
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
    // Constants
    val DEVICE_NAME = "BLE_Sensor"
    val SENSOR_SERVICE_UIID = "19b10010-e8f2-537e-4f6c-d104768a1214"
    val SENSOR_CHARACTERISTICS_UUID = "19b10011-e8f2-537e-4f6c-d104768a1214"

    // MutableSharedFlow for emitting sensor data
    override val data: MutableSharedFlow<Resource<SensorResult>> = MutableSharedFlow()

    // Bluetooth scanner
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    // Scan settings for Bluetooth scanning
    private val scanSettings =
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

    // BluetoothGatt instance for device connection
    private var gatt: BluetoothGatt? = null

    // Flag to track scanning status
    private var isScanning = false

    // Coroutine scope for launching coroutines
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // Scan callback for handling scan results
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

    // Variables for connection attempts
    private var currentConnectionAttempt = 1
    private var MAXIMUM_CONNECTION_ATTEMPTS = 5

    // BluetoothGattCallback for handling connection state changes and data
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
                    // Emit sensor result indicating disconnection
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
                // Handle connection failure and attempt reconnection
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
                    // Emit error message if maximum connection attempts exceeded
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

        // Handle MTU (Maximum Transmission Unit) change event
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {

            val characteristic =
                findCharacteristics(SENSOR_SERVICE_UIID, SENSOR_CHARACTERISTICS_UUID)
            if (characteristic == null) {
                // Emit error message if the sensor publisher characteristic is not found
                coroutineScope.launch {
                    data.emit(Resource.Error(errorMessage = "Could not find sensor publisher"))
                }
                return
            }
            enableNotification(characteristic)
        }

        // Handle characteristic changed event
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic
        ) {

            with(characteristic) {
                when (uuid) {
                    UUID.fromString(SENSOR_CHARACTERISTICS_UUID) -> {
                        // Parse the received sensor data
                        val rawData = characteristic.value
                        val force = rawData[1].toFloat()
                        val angle = 13f
                        val acceleration = 13f
                        // Create a SensorResult object with the parsed data
                        val sensorResult = SensorResult(
                            force, angle, acceleration, ConnectionState.Connected
                        )
                        // Emit the sensor result
                        coroutineScope.launch {
                            data.emit(Resource.Success(data = sensorResult))
                        }
                    }
                    else -> Unit
                }
            }
        }


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
    private fun findCharacteristics(
        serviceUUID: String, characteristicsUUID: String
    ): BluetoothGattCharacteristic? {
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
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
        val characteristic = findCharacteristics(SENSOR_SERVICE_UIID, SENSOR_CHARACTERISTICS_UUID)
        if (characteristic != null) {
            disconnectCharacteristic(characteristic)
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