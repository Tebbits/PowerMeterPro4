package com.example.blePowerMeter.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blePowerMeter.presentation.permissions.NewWorkoutScreen
import com.example.blePowerMeter.presentation.permissions.SensorScreen

@Composable
fun Navigation(
    onBluetoothStateChanged:()->Unit
) {

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.HomeScreen.route){
        composable(Screen.HomeScreen.route){
            HomeScreen(navController = navController)
        }

        composable(Screen.StartScreen.route){
            StartScreen(navController = navController)
        }

        composable(Screen.DeviceScreen.route){
            DeviceScreen(
                onBluetoothStateChanged,
                navController = navController
            )
        }
        composable(Screen.SensorScreen.route){
            SensorScreen(
                onBluetoothStateChanged,
                navController = navController,
            )
        }
        composable(Screen.NewWorkoutScreen.route){
            NewWorkoutScreen(

                navController = navController,
            )
        }
    }

}

sealed class Screen(val route:String){
    object StartScreen:Screen("start_screen")
    object DeviceScreen:Screen("device_screen")
    object HomeScreen:Screen("home_screen")
    object SensorScreen:Screen("sensor_screen")
    object NewWorkoutScreen:Screen("newWorkout_screen")
}