

package com.example.blePowerMeter.presentation.permissions

        import android.graphics.Color
        import android.graphics.Paint
        import androidx.compose.foundation.Canvas
        import androidx.compose.foundation.layout.*
        import androidx.compose.material.*
        import androidx.compose.runtime.*
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.geometry.Offset
        import androidx.compose.ui.geometry.Size
        import androidx.compose.ui.graphics.Color.Companion.Black
        import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
        import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight.Companion.Black
        import androidx.compose.ui.unit.dp
        import androidx.hilt.navigation.compose.hiltViewModel
        import androidx.lifecycle.ViewModel
        import androidx.lifecycle.viewmodel.compose.viewModel
        import androidx.navigation.NavController
        import com.example.blePowerMeter.presentation.DeviceViewModel
        import com.example.blePowerMeter.presentation.TopBar
        import com.example.blePowerMeter.ui.theme.Purple200
        import com.google.accompanist.permissions.ExperimentalPermissionsApi
        import kotlinx.coroutines.delay
        import kotlin.math.abs



        @OptIn(ExperimentalPermissionsApi::class)
        @Composable
        fun NewWorkoutScreen(viewModel: DeviceViewModel = hiltViewModel(), navController: NavController) {
            var isStarted by remember { mutableStateOf(false) }
            var chartData by remember { mutableStateOf(emptyList<Float>()) }

            Column(Modifier.fillMaxSize()) {
                TopBar(navController)

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { isStarted = true },
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

                if (isStarted) {
                    chartData += viewModel.force

                }
                LinearChart(dataValues = chartData)
            }
        }
@Composable
fun LinearChart(
    modifier: Modifier = Modifier.fillMaxWidth().height(200.dp),
    dataValues: List<Float>
) {
    if (dataValues.isEmpty()) return

    // Compute the time elapsed between data points
    val elapsedTime = (dataValues.size - 1) * 100 // Assuming data is updated every 100ms

    Canvas(modifier = modifier) {
        val totalRecords = dataValues.size
        val lineDistance: Float = size.width / (totalRecords - 1)
        val cHeight = size.height

        // Draw the y-axis
        drawLine(
            start = Offset(x = 0f, y = 0f),
            end = Offset(x = 0f, y = cHeight),
            color = Purple200,
            strokeWidth = 2f
        )

        // Draw the x-axis
        drawLine(
            start = Offset(x = 0f, y = cHeight),
            end = Offset(x = size.width, y = cHeight),
            color = Purple200,
            strokeWidth = 2f
        )

        // Draw the data points
        var currentLineDistance = 0f
        var previousDataPoint: Offset? = null
        dataValues.forEachIndexed { index, dataRate ->
            val dataPoint = Offset(
                x = currentLineDistance,
                y = calculateYCoordinate(
                    higherValue = 100f,
                    currentValue = dataRate,
                    canvasHeight = cHeight
                )
            )
            drawCircle(
                color = Purple200,
                radius = 8f,
                center = dataPoint
            )

            if (previousDataPoint != null) {
                drawLine(
                    start = previousDataPoint!!,
                    end = dataPoint,
                    color = Purple200,
                    strokeWidth = 4f
                )
            }

            currentLineDistance += lineDistance
            previousDataPoint = dataPoint
        }

        // Draw the x-axis labels
        val xLabelStep = size.width / 5 // Draw 5 labels
        for (i in 0..4) {
            val xLabel = (elapsedTime / 4 * i).toString()
            drawContext.canvas.nativeCanvas.drawText(
                xLabel,
                xLabelStep * i,
                size.height + 30f,
                Paint().apply {
                    color = Purple200.toArgb()
                    textSize = 28f
                }
            )
        }

        // Draw the y-axis label
        val yLabel = "Value"
        drawContext.canvas.nativeCanvas.drawText(
            yLabel,
            0f,
            0f,
            Paint().apply {
                color = Purple200.toArgb()
                textSize = 28f
            }
        )

        // Draw the y-axis labels
        val yLabelStep = cHeight / 4 // Draw 4 labels
        for (i in 1..4) {
            val yLabelValue = (100 - 25 * i).toString()
            drawContext.canvas.nativeCanvas.drawText(
                yLabelValue,
                -50f,
                yLabelStep * i,
                Paint().apply {
                    color = Purple200.toArgb()
                    textSize = 28f
                }
            )
        }
    }
}

fun calculateYCoordinate(
    higherValue: Float,
    currentValue: Float,
    canvasHeight: Float
): Float {
    val maxAndCurrentValueDifference = higherValue - currentValue
    val relativeScreen = canvasHeight / higherValue
    return maxAndCurrentValueDifference * relativeScreen
}







@Composable
fun ForceChart(data: List<Float>, modifier: Modifier = Modifier, viewModel: DeviceViewModel) {
    val viewModel = viewModel
    val maxValue = remember { data.map { abs(it) }.maxOrNull() ?: 0f }
    val canvasWidth = remember { 1000f }
    val canvasHeight = remember { 200f }
    val barWidth = remember { canvasWidth / (data.size - 1) }

    Canvas(modifier = modifier) {
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
            color = androidx.compose.ui.graphics.Color.Black,
            start = Offset(0f, canvasHeight),
            end = Offset(canvasWidth, canvasHeight),
            strokeWidth = 4f
        )
        drawLine(
            color = androidx.compose.ui.graphics.Color.Black,
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

    LaunchedEffect(data) {
        while (true) {
            val newData = viewModel.force
            data.toMutableList().apply {
                add(newData)
                if (size > 100) removeAt(0)
            }
            delay(100L) // Collect sensor values every 100 milliseconds
        }
    }
}