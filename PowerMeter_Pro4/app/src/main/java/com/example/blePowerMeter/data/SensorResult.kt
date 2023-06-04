package com.example.blePowerMeter.data

data class SensorResult(
    var force:Int,
    var cadence:Int,
   var connectionState: ConnectionState
)

