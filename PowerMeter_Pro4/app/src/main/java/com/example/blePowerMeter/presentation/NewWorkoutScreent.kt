package com.example.blePowerMeter.presentation

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

import com.example.blePowerMeter.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.Dispatchers
import kotlin.math.max
import kotlin.math.min


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NewWorkoutScreen(viewModel: DeviceViewModel = hiltViewModel(), navController: NavController) {
    val forceData = viewModel.averageForce


    val textColor = Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141C27)),
        contentAlignment = Alignment.TopStart
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(navController)

            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(background1)
                    .border(
                        BorderStroke(2.dp, background2), RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp), contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Force", style = MaterialTheme.typography.h6, color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SensorBox(
                            measurement = viewModel.force, textColor = textColor, text = "Now"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SensorBox(
                            measurement = viewModel.maxForce.toInt(),
                            textColor = textColor,
                            text = "Max"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SensorBox(
                            measurement = viewModel.totalAverageForce.toInt(),
                            textColor = textColor,
                            text = "Avg"
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cadence",
                            style = MaterialTheme.typography.h6,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SensorBox(
                            measurement = viewModel.cadence, textColor = textColor, text = "Now"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SensorBox(
                            measurement = viewModel.maxCadence.toInt(),
                            textColor = textColor,
                            text = "Max"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SensorBox(
                            measurement = viewModel.totalAverageCadence.toInt(),
                            textColor = textColor,
                            text = "Avg"
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .height(200.dp)
            ) {
                if (forceData.isNotEmpty()) {
                    LineChart(
                        data = forceData,
                        lineColor = Teal200,
                        title = "Force",
                        textColor = textColor
                    )
                } else {
                    Text(
                        text = "No data",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}