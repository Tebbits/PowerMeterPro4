package com.example.blePowerMeter.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blePowerMeter.data.ConnectionState
import com.example.blePowerMeter.data.SensorReceiveManager
import com.example.blePowerMeter.data.SensorResult
import com.example.blePowerMeter.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private var sensorReceiveManager: SensorReceiveManager
) : ViewModel(){

    var initializingMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var force by mutableStateOf(0)
        private set

    var angle by mutableStateOf(0)
        private set
    var velocity by mutableStateOf(0)
        private set

    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Uninitialized)

    private val forceData = SensorDataList()
    val averageForce: List<Float> get() = forceData.getAverageData()
    val maxForce: Float get() = forceData.getMaxData()
    val totalAverageForce: Float get() = forceData.getTotalAverageData()

    private val velocityData = SensorDataList()
    val averageVelocity: List<Float> get() = velocityData.getAverageData()
    val maxVelocity: Float get() = velocityData.getMaxData()
    val totalAverageVelocity: Float get() = velocityData.getTotalAverageData()

    private fun addSensorData(value: Float, dataList: SensorDataList) {
        dataList.addData(value)
    }

    private class SensorDataList {
        private val data = mutableStateListOf<Float>()
        private val averagedData = mutableStateListOf<Float>()
        private var maxData: Float = Float.MIN_VALUE

        fun addData(value: Float) {
            data.add(value)
            if (value > maxData) {
                maxData = value
            }
        }

        fun getAverageData(n: Int = 10): List<Float> {
            val lastNData = data.takeLast(n)
            if (lastNData.isNotEmpty()) {
                val averageData = lastNData.sum() / lastNData.size
                averagedData.add(averageData)
            }
            return averagedData
        }

        fun getMaxData(): Float {
            return maxData
        }

        fun getTotalAverageData(): Float {
            if (data.isNotEmpty()) {
                return data.sum() / data.size
            }
            return 0f
        }
    }
    private fun subscribeToChanges(){
        viewModelScope.launch {
            sensorReceiveManager.data.collect{ result ->
                when(result){
                    is Resource.Success -> {
                        force = result.data.force
                        angle = result.data.angle
                        velocity = result.data.velocity
                        connectionState = result.data.connectionState
                        forceData.addData(force.toFloat())
                        velocityData.addData(velocity.toFloat())


                    }

                    is Resource.Loading -> {
                        initializingMessage = result.message
                        connectionState = ConnectionState.CurrentlyInitializing

                    }

                    is Resource.Error -> {
                        errorMessage = result.errorMessage
                        connectionState = ConnectionState.Uninitialized
                    }
                }
            }
        }
    }

    fun disconnect(){
        sensorReceiveManager.disconnect()
    }

    fun reconnect(){
        sensorReceiveManager.reconnect()
    }

    fun initializeConnection(){
        errorMessage = null
        subscribeToChanges()
        sensorReceiveManager.startReceiving()
    }

    override fun onCleared() {
        super.onCleared()
        sensorReceiveManager.closeConnection()
    }


    fun startReceiving() {
        viewModelScope.launch {
            sensorReceiveManager.startReceiving()
        }
    }

    fun stopReceiving() {
        viewModelScope.launch {
            sensorReceiveManager.stopReceiving()
        }
    }

init {
    subscribeToChanges()}}


