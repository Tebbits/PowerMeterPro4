package com.example.blePowerMeter.presentation.permissions

import android.bluetooth.BluetoothAdapter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    var connected by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(50.dp))
            Button(onClick = { connected = true }) {
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
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
                        modifier = Modifier.weight(1f)
                            .padding(10.dp)
                    )
                    if (!connected) {
                        Button(
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
            if (connected) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SensorReading(
                        force = viewModel.force,
                        angle = viewModel.angle,
                        cadence = viewModel.cadence
                    )
                }
            }
        }


        DisposableEffect(viewModel.connectionState) {
            val connectionState = viewModel.connectionState
            if (connectionState == ConnectionState.Connected) {
                connected = true
            }

            onDispose {
                connected = false
            }
        }


}}


@Composable
fun SensorReading(
    force: Float,
    angle: Float,
    cadence: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
                style = MaterialTheme.typography.body1
            )
        }
    }
}
