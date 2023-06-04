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
import com.example.blePowerMeter.presentation.*
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
                    .border(BorderStroke(2.dp, background2), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Device Connection:",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                    when (viewModel.connectionState) {
                        ConnectionState.CurrentlyInitializing -> {
                            Text(
                                text = "Connecting...",
                                style = MaterialTheme.typography.body1,
                                color = textColor
                            )
                        }
                        ConnectionState.Connected -> {
                            Text(
                                text = "Connected",
                                style = MaterialTheme.typography.body1,
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
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(background1)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (viewModel.connectionState) {
                        ConnectionState.CurrentlyInitializing -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                viewModel.initializingMessage?.let {
                                    Text(text = it, color = textColor)
                                }
                            }
                        }
                        ConnectionState.Uninitialized -> {
                            if (!permissionState.allPermissionsGranted) {
                                Text(
                                    text = "Go to the app settings and allow the missing permissions.",
                                    style = MaterialTheme.typography.body2,
                                    textAlign = TextAlign.Center,
                                    color = textColor
                                )
                            } else if (viewModel.errorMessage != null) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = viewModel.errorMessage!!, color = textColor)
                                    Button(
                                        onClick = {
                                            if (permissionState.allPermissionsGranted) {
                                                viewModel.initializeConnection()
                                            }
                                        }
                                    ) {
                                        Text(text = "Try again", color = textColor)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(background1)
                        .border(BorderStroke(2.dp, background2), RoundedCornerShape(16.dp)),

                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sensor Readings",
                            style = MaterialTheme.typography.h6,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SensorBox(
                                measurement = viewModel.force,
                                text = "Force",
                                textColor = textColor
                            )
                            SensorBox(
                                measurement = viewModel.cadence,
                                text = "Cadence",
                                textColor = textColor
                            )
                        }
                    }
                }

                LineChart(
                    data = viewModel.averageForce,
                    lineColor = Teal200,
                    title = "Force",
                    textColor = textColor,

                )

                LineChart(
                    data = viewModel.averageCadence,
                    lineColor = Purple200,
                    title = "Velocity",
                    textColor = textColor,

                )
            }
        }
    }}
