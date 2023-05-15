package com.example.blePowerMeter.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.blePowerMeter.data.ConnectionState
import com.example.blePowerMeter.data.SensorReceiveManager
import com.example.blePowerMeter.data.SensorResult
import com.example.blePowerMeter.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val sensorReceiveManager: SensorReceiveManager
) : ViewModel() {

    var force: Float = 0f
    var angle: Float = 0f
    var cadence: Float = 0f
    var connectionState: ConnectionState = ConnectionState.Uninitialized
    var initializingMessage: String? = null
    var errorMessage: String? = null

    private fun subscribeToChanges(){
        viewModelScope.launch {
            sensorReceiveManager.data.collect{ result ->
                when(result){
                    is Resource.Success -> {
                        force = result.data.force
                        angle = result.data.angle
                        cadence = result.data.cadence
                        connectionState = result.data.connectionState

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
        timer.cancel()
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

    private val _force = MutableLiveData<Float>()
    var forceData: LiveData<Float> = _force

    private val _chartData = MutableLiveData<List<ChartData>>(emptyList())
    val chartData: LiveData<List<ChartData>> = _chartData

    private val timer = Timer()

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val forceValue = readSensor()
                _force.postValue(forceValue)
                _chartData.postValue(
                    _chartData.value.orEmpty() + ChartData(System.currentTimeMillis(), forceValue)
                )
            }
        }, 0L, 100L)
    }

    private fun readSensor(): Float {
        return force
    }

    data class ChartData(
        val timestamp: Long,
        val forceValue: Float
    )
}


