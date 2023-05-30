package com.example.blePowerMeter.data

data class SensorResult(
    var force:Float,
    var angle:Float,
   var cadence:Float,
   var connectionState: ConnectionState
)

