package com.example.blePowerMeter.presentation.permissions

import android.bluetooth.BluetoothAdapter
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.blePowerMeter.data.ConnectionState
import com.example.blePowerMeter.presentation.DeviceViewModel
import com.example.blePowerMeter.presentation.TopBar
import com.example.blePowerMeter.ui.theme.Purple200
import com.example.blePowerMeter.ui.theme.Teal200
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.example.blePowerMeter.ui.theme.Gray300


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

    DisposableEffect(key1 = lifecycleOwner, effect = {
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
    })

    LaunchedEffect(key1 = permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            if (bleConnectionState == ConnectionState.Uninitialized) {
                viewModel.initializeConnection()
            }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray300), contentAlignment = Alignment.TopStart
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(navController = navController)



            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(10.dp)
                    .border(
                        BorderStroke(
                            3.dp, Teal200
                        ), RoundedCornerShape(10.dp)
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
                    if (bleConnectionState != ConnectionState.Disconnected) {
                        Button(modifier = Modifier.padding(10.dp), onClick = {
                            if (permissionState.allPermissionsGranted) {
                                viewModel.initializeConnection()
                            }
                        }) {
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
                force = viewModel.force, angle = viewModel.angle, velocity = viewModel.velocity
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray300)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .border(3.dp, Teal200, RoundedCornerShape(10.dp))
            ) {
                CalibrationBox(viewModel = viewModel)
            }
        }

    }


}


@Composable
fun SensorReading(
    force: Float,
    angle: Float,
    velocity: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Gray300)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .border(3.dp, Teal200, RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)

        ) {
            Text(
                text = "Sensor Measurement",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorBox(
                    measurement = force,
                    color = Color(0xFFB2DFDB),
                    text = "Force",

                    )
                Spacer(modifier = Modifier.width(10.dp))
                SensorBox(
                    measurement = velocity,
                    color = Color(0xFF80CBC4),
                    text = "Velocity",

                    )
                Spacer(modifier = Modifier.width(10.dp))
                SensorBox(
                    measurement = angle,
                    color = Color(0xFF4DB6AC),
                    text = "angle",

                    )
            }
        }
    }
}

@Composable
fun SensorBox(
    measurement: Float, color: Color, text: String
) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .height(80.dp)
            .border(
                BorderStroke(
                    2.dp, Color(0xFF009688)
                ), RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.subtitle1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = measurement.toString(),
                style = MaterialTheme.typography.h5
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

    Column(
        modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Device calibration",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(5.dp)
        )
        if (viewModel.connectionState == ConnectionState.Connected) {
        // Input weight and start calibration
        Row(verticalAlignment = Alignment.CenterVertically) {

                Text("Weight (kg): ")
            TextField(
                value = weight.toString(),
                onValueChange = { weight = it.toFloatOrNull() ?: 0f },
                modifier = Modifier.width(64.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        isCalibrating = true
                        sensorValues = listOf()
                        viewModel.startReceiving()
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(10000L) // Wait for 10 seconds
                            isCalibrating = false
                            viewModel.stopReceiving()
                            calibrationFactor = sensorValues.maxOrNull()?.div(weight) ?: 1f
                        }
                    }, enabled = !isCalibrating
                ) {
                    Text("Calibrate")
                }
            }

        Text(
            "Calibration factor: $calibrationFactor",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 16.dp)
        )}
        // Show sensor values during calibration
        if (isCalibrating) {
            LaunchedEffect(Unit) {
                while (isCalibrating) {
                    sensorValues += viewModel.force
                    delay(100L) // Collect sensor values every 100 milliseconds
                }
            }
            Text(
                "Calibrating...",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // Show calibration result
        if (!isCalibrating && calibrationFactor != 1f) {
            LineChart(
                data = sensorValues,
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
            )

        }
    }
}

@Composable
fun LineChart(data: List<Float>, modifier: Modifier = Modifier) {
    val maxValue = remember { data.map { abs(it) }.maxOrNull() ?: 0f }

    Canvas(modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = canvasWidth / (data.size - 1)

        // Draw the bars
        data.forEachIndexed { index, value ->
            val barHeight = abs(value) / maxValue * canvasHeight
            drawRect(
                color = Purple200,
                topLeft = Offset(index * barWidth, canvasHeight - barHeight),
                size = Size(barWidth, barHeight)
            )
        }

        // Draw the axis
        drawLine(
            color = Color.Black,
            start = Offset(0f, canvasHeight),
            end = Offset(canvasWidth, canvasHeight),
            strokeWidth = 4f
        )
        drawLine(
            color = Color.Black,
            start = Offset(0f, 0f),
            end = Offset(0f, canvasHeight),
            strokeWidth = 4f
        )

        // Draw the y-axis labels
        val yAxisLabels = listOf("0", "0.25", "0.5", "0.75", "1")
        yAxisLabels.forEachIndexed { index, label ->
            val labelY = index.toFloat() / (yAxisLabels.size - 1) * canvasHeight
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = 5
                }
                canvas.nativeCanvas.drawText(
                    label, 0f, labelY, paint
                )
            }
        }
    }
}
