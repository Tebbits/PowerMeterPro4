package com.example.blePowerMeter.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.blePowerMeter.ui.theme.Teal200
import com.example.blePowerMeter.ui.theme.background1


@Composable
fun StartScreen(
    navController: NavController
) {

    Box(
        modifier = Modifier.fillMaxSize()
            .background(background1),
    ) {
        Column {
            TopBar(navController = navController)
            Spacer(modifier = Modifier.height(50.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .width(250.dp)
                        .height(100.dp)
                        .clip(RoundedCornerShape(size = 16.dp,))
                        .background(Teal200, RoundedCornerShape(size = 16.dp,))
                        .clickable {
                            navController.navigate(Screen.SensorScreen.route) {
                                popUpTo(Screen.StartScreen.route) {
                                    inclusive = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pair Device",
                        fontSize = 35.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}






