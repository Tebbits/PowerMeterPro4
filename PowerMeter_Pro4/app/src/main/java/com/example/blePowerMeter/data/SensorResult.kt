package com.example.blePowerMeter.data

data class SensorResult(
    var force:Int,
    var velocity:Int,
    var angle:Int,
   var connectionState: ConnectionState
)

