package com.example.blePowerMeter.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
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



        Box(modifier = Modifier.padding(20.dp)
            .height(200.dp)) {
            if (forceData.isNotEmpty()) {
                ForceChart(data = forceData)

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
                style = Stroke(width = 1f)
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



