package com.example.blePowerMeter.data

data class SensorResult(
    val force:Float,
    val angle:Float,
   val cadence:Float,
   val connectionState: ConnectionState
)

