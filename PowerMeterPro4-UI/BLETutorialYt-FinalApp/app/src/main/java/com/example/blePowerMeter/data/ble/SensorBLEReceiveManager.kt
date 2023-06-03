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

    val DEVICE_NAME = "Power Meter"
    val SENSOR_SERVICE_UUID = "19b10010-e8f2-537e-4f6c-d104768a1214"
    val SENSOR_CHARACTERISTICS_UUID = "19b10011-e8f2-537e-4f6c-d104768a1214"


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
                    result.device.connectGatt(context, false, gattCallback,BluetoothDevice.TRANSPORT_LE)
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
                    Log.d("LOG","StartReceiving")
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
                Log.d("LOG","GattTable")
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Adjusting MTU space..."))
                }
                gatt.requestMtu(517)
                Log.d("LOG","requested MTU")
            }
        }


        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            val sensorCharacteristic = findCharacteristics(SENSOR_SERVICE_UUID,
                SENSOR_CHARACTERISTICS_UUID)
            Log.d("LOG","MTU: $mtu")
            if (sensorCharacteristic== null) {
                coroutineScope.launch {
                    data.emit(Resource.Error(errorMessage = "Could not find sensor publisher"))
                }
                return
            }
            Log.d("LOG","enableNotification")
            enableNotification(sensorCharacteristic)


        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            var force = 0
            var angle = 0
            var velocity = 0

            // Log.d("LOG","characteristic $characteristic")
            with(characteristic) {
                when (uuid) {
                    UUID.fromString(SENSOR_CHARACTERISTICS_UUID) -> {
                        // Handle force characteristic notification
                        val rawData = characteristic.value
                        // Parsen der Daten für force
                        force = (rawData[0].toInt() and 0xFF)

                        // Parsen der Daten für velocity
                        velocity = (rawData[1].toInt() and 0xFF)

                        // Parsen der Daten für angle
                        angle = (rawData[2].toInt() and 0xFF) or ((rawData[3].toInt() and 0xFF) shl 8)


                        Log.d("rawData",rawData.contentToString())
                        Log.d("force", "$force")
                        Log.d("velocity", "$velocity")
                        Log.d("angle", "$angle")
                    }

                    else -> Unit
                }
            }
            val sensorResult = SensorResult(
                force.toFloat(), velocity.toFloat(), angle.toFloat(), ConnectionState.Connected
            )

            coroutineScope.launch {
                data.emit(Resource.Success(data = sensorResult))
            }
        }}

    private fun enableNotification(characteristic: BluetoothGattCharacteristic){
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        val payload = when {
            characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> return
        }

        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if(gatt?.setCharacteristicNotification(characteristic, true) == false){
                Log.d("BLEReceiveManager","set characteristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, payload)
        }
    }

    private fun writeDescription(descriptor: BluetoothGattDescriptor, payload: ByteArray){
        gatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    private fun findCharacteristics(serviceUUID: String, characteristicsUUID:String):BluetoothGattCharacteristic?{
        return gatt?.services?.find { service ->
            service.uuid.toString() == serviceUUID
        }?.characteristics?.find { characteristics ->
            characteristics.uuid.toString() == characteristicsUUID
        }
    }

    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Scanning Ble devices..."))
        }
        isScanning = true
        bleScanner.startScan(null,scanSettings,scanCallback)
    }

    override fun reconnect() {
        gatt?.connect()
    }

    override fun disconnect() {
        gatt?.disconnect()
    }



    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        val characteristic = findCharacteristics(SENSOR_SERVICE_UUID, SENSOR_CHARACTERISTICS_UUID)
        if(characteristic != null){
            disconnectCharacteristic(characteristic)
        }
        gatt?.close()
    }

    private fun disconnectCharacteristic(characteristic: BluetoothGattCharacteristic){
        val cccdUuid = UUID.fromString(CCCD_DESCRIPTOR_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccdDescriptor ->
            if(gatt?.setCharacteristicNotification(characteristic,false) == false){
                Log.d("TempHumidReceiveManager","set charateristics notification failed")
                return
            }
            writeDescription(cccdDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        }
    }

    override fun stopReceiving() {
        TODO("Not yet implemented")
    }

}