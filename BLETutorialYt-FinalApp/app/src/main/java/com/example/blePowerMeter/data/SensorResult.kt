package com.example.blePowerMeter.data

data class SensorResult(
    val force:Float,
    val angle:Float,
   val acceleration:Float,
   val connectionState: ConnectionState
)

