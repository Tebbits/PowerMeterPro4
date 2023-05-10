package com.example.blePowerMeter.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
                onBluetoothStateChanged
            )
        }
    }

}

sealed class Screen(val route:String){
    object StartScreen:Screen("start_screen")
    object DeviceScreen:Screen("device_screen")
    object HomeScreen:Screen("home_screen")
}