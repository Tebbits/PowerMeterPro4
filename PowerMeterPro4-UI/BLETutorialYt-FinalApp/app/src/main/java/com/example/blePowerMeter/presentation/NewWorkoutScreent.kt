package com.example.blePowerMeter.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.blePowerMeter.presentation.permissions.SensorBox
import com.example.blePowerMeter.presentation.permissions.SensorReading
import com.example.blePowerMeter.ui.theme.Gray300
import com.example.blePowerMeter.ui.theme.Purple500
import com.example.blePowerMeter.ui.theme.Teal200
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.Dispatchers
import kotlin.math.max
import kotlin.math.min


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NewWorkoutScreen(viewModel: DeviceViewModel = hiltViewModel(), navController: NavController) {
    var isStarted by remember { mutableStateOf(false) }
    val forceData = viewModel.averageForce

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Gray300), contentAlignment = Alignment.TopStart
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(navController)

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        isStarted = true

                    },
                    enabled = !isStarted,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start Workout")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { isStarted = false },
                    enabled = isStarted,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Workout")
                }
            }



            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .height(200.dp)
            ) {
                if (forceData.isNotEmpty()) {
                    ForceChart(data = forceData)

                }
            }

            PowerBox(actual = viewModel.force, max = 100f, average = 50f)
        }
    }
}

@Composable
fun PowerBox(
    actual: Float,
    max: Float,
    average: Float
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
                text = "Power Measurement",
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
                    measurement = actual,
                    color = Color(0xFFB2DFDB),
                    text = "Actual",

                    )
                Spacer(modifier = Modifier.width(10.dp))
                SensorBox(
                    measurement = max,
                    color = Color(0xFF80CBC4),
                    text = "Max",

                    )
                Spacer(modifier = Modifier.width(10.dp))
                SensorBox(
                    measurement = average,
                    color = Color(0xFF4DB6AC),
                    text = "Average",

                    )
            }
        }
    }
}

@Composable
fun ForceChart(data: List<Float>) {
    if (data.isNotEmpty()) {
        val maxX = data.size.toFloat()
        val maxY = data.maxOrNull() ?: 0f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val linePath = Path()
            linePath.moveTo(0f, data.first() * size.height / maxY)
            for (i in 1 until data.size) {
                val x = i * size.width / maxX
                val y = data[i] * size.height / maxY
                linePath.lineTo(x, y)
            }

            drawPath(
                path = linePath,
                color = Purple500,
                style = Stroke(width = 2f)
            )
        }
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



