package com.example.blePowerMeter.data

import com.example.blePowerMeter.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow

interface SensorReceiveManager {

    val data: MutableSharedFlow<Resource<SensorResult>>

    fun reconnect()

    fun disconnect()

    fun startReceiving()

    fun closeConnection()

}