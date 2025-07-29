package edu.unicauca.halterfilia_cauca.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import edu.unicauca.halterfilia_cauca.ui.features.athlete.AthletesScreen
import edu.unicauca.halterfilia_cauca.ui.features.athlete_registration.AthleteRegistrationScreen
import edu.unicauca.halterfilia_cauca.ui.features.bluetooth_connection.BluetoothScreen
import edu.unicauca.halterfilia_cauca.ui.features.history.HistoryScreen
import edu.unicauca.halterfilia_cauca.ui.features.login.LoginScreen
import edu.unicauca.halterfilia_cauca.ui.features.measurement.MedidasScreen
import edu.unicauca.halterfilia_cauca.ui.features.register.RegisterScreen

@Composable
fun AppNavigation(){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppScreens.LoginScreen.route ){
        composable(AppScreens.LoginScreen.route){
            LoginScreen(navController)
        }
        composable(AppScreens.RegisterScreen.route){
            RegisterScreen(navController)
        }
        composable(AppScreens.AthleteScreen.route){
            AthletesScreen(navController)
        }
        composable(AppScreens.AthleteRegistrationScreen.route){
            AthleteRegistrationScreen(navController)
        }
        composable(AppScreens.BluetoothScreen.route){
            BluetoothScreen(navController)
        }
        composable(AppScreens.HistoryScreen.route){
            HistoryScreen(navController)
        }
        composable(AppScreens.MedidasScreen.route){
            MedidasScreen(navController)
        }
    }
}
