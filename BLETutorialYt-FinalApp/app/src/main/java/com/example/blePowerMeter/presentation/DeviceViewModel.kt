package com.example.blePowerMeter.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    var force by mutableStateOf(0f)
        private set

    var angle by mutableStateOf(0f)
        private set
    var cadence by mutableStateOf(0f)
        private set

    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Uninitialized)


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

}

