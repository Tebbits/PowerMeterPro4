package com.example.blePowerMeter.presentation

import android.graphics.drawable.Icon
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.blePowerMeter.ui.theme.*


@Composable
fun HomeScreen(
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background1),
        contentAlignment = Alignment.TopStart
    ) {
        Column {
            TopBar(navController = navController)
            Spacer(modifier = Modifier.height(50.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HomeButton(
                        navController = navController,
                        color = Teal200,
                        name = "Device",
                        icon = Icons.Outlined.Settings,
                        screen = Screen.StartScreen
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HomeButton(
                        navController = navController,
                        color = Teal200,
                        name = "New Workout",
                        icon = Icons.Default.Add,
                        screen = Screen.NewWorkoutScreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(50.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HomeButton(
                        navController = navController,
                        color = Teal200,
                        name = "Info",
                        icon = Icons.Default.Info,
                        screen = Screen.HomeScreen
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HomeButton(
                        navController = navController,
                        color = Teal200,
                        name = "Home",
                        icon = Icons.Outlined.Settings,
                        screen = Screen.SensorScreen
                    )
                }
            }
        }
    }
}



@Composable
fun TopBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(Teal200)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    Icons.Outlined.ArrowBack,
                    contentDescription = "Back Icon"
                )
            }
            Row {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "Help Icon"
                    )
                }
                IconButton(
                    onClick = { navController.navigate(Screen.HomeScreen.route) },
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        Icons.Outlined.Home,
                        contentDescription = "Home Icon"
                    )
                }
            }
        }
    }
}


@Composable
fun HomeButton(
    navController: NavController,
    color: Color,
    name: String,
    icon: ImageVector,
    screen: Screen
) {
    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable {
                navController.navigate(screen.route) {
                    popUpTo(Screen.HomeScreen.route) {
                        inclusive = true
                    }
                }
            }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$name Icon",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@Composable
fun LineChart(
    data: List<Float>,
    lineColor: Color,
    title: String,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            color = textColor,
            modifier = Modifier.padding(8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(top = 50.dp) // Increase top padding to create space for the title
        ) {
            if (data.isNotEmpty()) {
                val maxX = data.size.toFloat()
                val maxY = data.maxOrNull() ?: 0f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val linePath = Path()
                    linePath.moveTo(0f, size.height)
                    for (i in 0 until data.size) {
                        val x = i * size.width / (maxX - 1)
                        val y = size.height - data[i] * size.height / maxY
                        linePath.lineTo(x, y)
                    }

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}


@Composable
fun SensorBox(
    measurement: Int, textColor: Color, text: String
) {
    val background = Color(0xFF475162)

    Box(
        modifier = Modifier
            .height(40.dp)
            .width(120.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(
                BorderStroke(2.dp, Teal200), RoundedCornerShape(10.dp)
            ), contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.subtitle1,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = measurement.toString(),
                style = MaterialTheme.typography.h6,
                color = textColor
            )
        }
    }
}


@Composable
fun SensorReading(
    force: Int,
    cadence: Int,
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
                    measurement = cadence,
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




