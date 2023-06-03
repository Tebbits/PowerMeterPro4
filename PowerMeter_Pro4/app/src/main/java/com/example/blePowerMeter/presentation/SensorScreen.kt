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
import com.example.blePowerMeter.presentation.LineChart
import com.example.blePowerMeter.presentation.TopBar
import com.example.blePowerMeter.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs


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

    val background1 = Color(0xFF1F2D3B)
    val background2 = Color(0xFF475162)
    val textColor = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background1),
        contentAlignment = Alignment.TopStart
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
                    .background(background1)
                    .border(
                        BorderStroke(3.dp, background2), RoundedCornerShape(10.dp)
                    )
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
                            .padding(10.dp),
                        color = textColor
                    )
                    when (viewModel.connectionState) {
                        ConnectionState.CurrentlyInitializing -> {
                            Text(
                                text = "Connecting...",
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(end = 16.dp),
                                color = textColor
                            )
                        }
                        ConnectionState.Connected -> {
                            Text(
                                text = "Connected",
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(end = 16.dp),
                                color = textColor
                            )
                        }
                        else -> {
                            // Do nothing
                        }
                    }
                }
            }

            if (viewModel.connectionState != ConnectionState.Connected) {
                Spacer(modifier = Modifier.height(50.dp))
                Box(
                    modifier = Modifier
                        .size(250.dp, 200.dp)
                        .aspectRatio(1f)
                        .background(background1)
                ) {
                    when (viewModel.connectionState) {
                        ConnectionState.CurrentlyInitializing -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                viewModel.initializingMessage?.let {
                                    Text(
                                        text = it
                                    )
                                }
                            }
                        }
                        ConnectionState.Uninitialized -> {
                            if (!permissionState.allPermissionsGranted) {
                                Text(
                                    text = "Go to the app settings and allow the missing permissions.",
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center,
                                    color = textColor
                                )
                            } else if (viewModel.errorMessage != null) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = viewModel.errorMessage!!, color = textColor
                                    )
                                    Button(onClick = {
                                        if (permissionState.allPermissionsGranted) {
                                            viewModel.initializeConnection()
                                        }
                                    }) {
                                        Text(
                                            "Try again", color = textColor
                                        )
                                    }
                                }
                            }
                        }

                    }
                }
            } else {

                SensorReading(
                    force = viewModel.force,
                    angle = viewModel.angle,
                    velocity = viewModel.velocity,
                    textColor = textColor,
                    background = background1
                )

                    LineChart(data = viewModel.averageForce, lineColor = Teal200, title = "Force",textColor = textColor)

                LineChart(data = viewModel.averageVelocity, lineColor = Purple200, title = "Velocity",textColor = textColor )

            }
        }
    }
}
@Composable
fun SensorReading(
    force: Int,
    angle: Int,
    velocity: Int,
    textColor: Color,
    background: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .border(3.dp, background2, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = "Sensor Measurement",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 5.dp),
                color = textColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SensorContainer(
                    measurement = force,
                    background = background,
                    text = "Force",
                    textColor = textColor
                )
                SensorContainer(
                    measurement = velocity,
                    background = background,
                    text = "Velocity",
                    textColor = textColor
                )
                SensorContainer(
                    measurement = angle,
                    background = background,
                    text = "Angle",
                    textColor = textColor
                )
            }
        }
    }
}

@Composable
fun SensorContainer(
    measurement: Int,
    background: Color,
    text: String,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(2.dp, Teal200, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.subtitle2,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = measurement.toString(),
                style = MaterialTheme.typography.body1,
                color = textColor
            )
        }
    }
}
