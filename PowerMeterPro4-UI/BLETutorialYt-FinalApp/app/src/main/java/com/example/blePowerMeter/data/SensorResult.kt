package com.example.blePowerMeter.data

data class SensorResult(
    var force:Float,
    var velocity:Float,
    var angle:Float,
   var connectionState: ConnectionState
)

