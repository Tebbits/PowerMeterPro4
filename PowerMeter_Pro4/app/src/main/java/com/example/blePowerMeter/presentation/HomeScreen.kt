package com.example.blePowerMeter.presentation

import android.graphics.drawable.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
                    Button(
                        navController = navController,
                        color = Teal200,
                        name = "Device",
                        icon = Icons.Outlined.Settings,
                        screen = Screen.StartScreen
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
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
                    Button(
                        navController = navController,
                        color = Teal200,
                        name = "Info",
                        icon = Icons.Default.Info,
                        screen = Screen.HomeScreen
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
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
fun Button(
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






