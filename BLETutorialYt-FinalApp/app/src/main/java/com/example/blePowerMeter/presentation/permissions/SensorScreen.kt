package com.example.blePowerMeter.presentation.permissions

import android.bluetooth.BluetoothAdapter
import android.os.CountDownTimer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.blePowerMeter.data.ConnectionState
import com.example.blePowerMeter.presentation.DeviceViewModel
import com.example.blePowerMeter.presentation.TopBar
import com.example.blePowerMeter.presentation.permissions.PermissionUtils
import com.example.blePowerMeter.presentation.permissions.SystemBroadcastReceiver
import com.example.blePowerMeter.ui.theme.BackgroundColor
import com.example.blePowerMeter.ui.theme.Purple200
import com.example.blePowerMeter.ui.theme.Purple500
import com.example.blePowerMeter.ui.theme.Teal
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorScreen(
    onBluetoothStateChanged: () -> Unit,
    viewModel: DeviceViewModel = hiltViewModel(),
    navController: NavController,

    ) {

    SystemBroadcastReceiver(systemAction = BluetoothAdapter.ACTION_STATE_CHANGED) { bluetoothState ->
        val action = bluetoothState?.action ?: return@SystemBroadcastReceiver
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            onBluetoothStateChanged()
        }
    }

    val permissionState =
        rememberMultiplePermissionsState(permissions = PermissionUtils.permissions)
    val lifecycleOwner = LocalLifecycleOwner.current
    val bleConnectionState = viewModel.connectionState

    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    permissionState.launchMultiplePermissionRequest()
                    if (permissionState.allPermissionsGranted && bleConnectionState == ConnectionState.Disconnected) {
                        viewModel.reconnect()
                    }
                }
                if (event == Lifecycle.Event.ON_STOP) {
                    if (bleConnectionState == ConnectionState.Connected) {
                        viewModel.disconnect()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    )

    LaunchedEffect(key1 = permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            if (bleConnectionState == ConnectionState.Uninitialized) {
                viewModel.initializeConnection()
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(navController = navController)
            Spacer(modifier = Modifier.height(10.dp))


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(10.dp)
                .border(
                    BorderStroke(
                        5.dp, Teal
                    ),
                    RoundedCornerShape(10.dp)
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Device connection",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp)
                )
                if (bleConnectionState == ConnectionState.Uninitialized) {
                    Button(modifier = Modifier.padding(10.dp),
                        onClick = {
                            if (permissionState.allPermissionsGranted) {
                                viewModel.initializeConnection()
                            }
                        }
                    ) {
                        Text(text = "Connect")
                    }
                } else {
                    Text(
                        text = "Connected",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            }
        }






            SensorReading(
                force = viewModel.force,
                angle = viewModel.angle,
                cadence = viewModel.cadence
            )
            CalibrationBox(viewModel = viewModel)
        }

    }


}


@Composable
fun SensorReading(
    force: Float,
    angle: Float,
    cadence: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .border(5.dp, Teal, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Sensor Measurement",
                style = MaterialTheme.typography.h6
            )
            Spacer(modifier = Modifier.height(16.dp))
            SensorBox(
                measurement = force,
                color = Color(0xFFB2DFDB),
                text = "Force"
            )
            Spacer(modifier = Modifier.height(16.dp))
            SensorBox(
                measurement = angle,
                color = Color(0xFF80CBC4),
                text = "Angle"
            )
            Spacer(modifier = Modifier.height(16.dp))
            SensorBox(
                measurement = cadence,
                color = Color(0xFF4DB6AC),
                text = "Cadence"
            )
        }
    }
}

@Composable
fun SensorBox(
    measurement: Float,
    color: Color,
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color)

            .border(
                BorderStroke(
                    2.dp,
                    Color(0xFF009688)
                ),
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(16.dp)

        ) {
            Text(
                text = "$text: ",
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = measurement.toString(),
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}
@Composable
fun CalibrationBox(viewModel: DeviceViewModel) {
    var weight by remember { mutableStateOf(0f) }
    var calibrationFactor by remember { mutableStateOf(1f) }
    var sensorValues by remember { mutableStateOf(listOf<Float>()) }
    var isCalibrating by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Input weight and start calibration
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Weight (kg): ")
            TextField(
                value = weight.toString(),
                onValueChange = { weight = it.toFloatOrNull() ?: 0f },
                modifier = Modifier.width(64.dp) // Set the width of the TextField to 64 dp
            )
            Spacer(modifier = Modifier.width(16.dp)) // Add some space between the TextField and the Button
            if (viewModel.connectionState == ConnectionState.Connected) {
                Button(onClick = {
                    isCalibrating = true
                    sensorValues = listOf()
                    viewModel.startReceiving()
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(10000L) // Wait for 10 seconds
                        isCalibrating = false
                        viewModel.stopReceiving()
                        calibrationFactor = sensorValues.maxOrNull()?.div(weight) ?: 1f
                    }
                }) {
                    Text("Calibrate")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp)) // Add some space between the input row and the sensor values

        // Show sensor values during calibration
        if (isCalibrating) {
            LaunchedEffect(Unit) {
                while (isCalibrating) {
                    sensorValues += viewModel.force
                    delay(100L) // Collect sensor values every 100 milliseconds
                }
            }
            Text("Sensor values: ${sensorValues.joinToString()}")
        }

        Spacer(modifier = Modifier.height(16.dp)) // Add some space between the sensor values and the calibration factor

        // Show calibration result
        Text("Calibration factor: $calibrationFactor")
    }
}
