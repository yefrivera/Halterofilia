package edu.unicauca.halterfilia_cauca.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import edu.unicauca.halterfilia_cauca.ui.features.athlete.AthletesScreen
import edu.unicauca.halterfilia_cauca.ui.features.athlete_registration.AthleteRegistrationScreen
import edu.unicauca.halterfilia_cauca.ui.features.bluetooth_connection.BluetoothScreen
import edu.unicauca.halterfilia_cauca.ui.features.history.HistoryScreen
import edu.unicauca.halterfilia_cauca.ui.features.login.LoginScreen
import edu.unicauca.halterfilia_cauca.ui.features.measurement.MedidasScreen
import edu.unicauca.halterfilia_cauca.ui.features.register.RegisterScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val startDestination = if (auth.currentUser != null) {
        AppScreens.AthleteScreen.route
    } else {
        AppScreens.LoginScreen.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(route = AppScreens.LoginScreen.route) {
            LoginScreen(navController = navController)
        }
        composable(route = AppScreens.RegisterScreen.route) {
            RegisterScreen(navController = navController)
        }
        composable(route = AppScreens.AthleteScreen.route) {
            AthletesScreen(navController = navController)
        }
        composable(route = AppScreens.AthleteRegistrationScreen.route){
            AthleteRegistrationScreen(navController = navController)
        }
        composable(route = AppScreens.BluetoothScreen.route){
            BluetoothScreen(navController = navController)
        }
        composable(route = AppScreens.HistoryScreen.route){
            HistoryScreen(navController = navController)
        }
        composable(route = AppScreens.MedidasScreen.route) {
            MedidasScreen(navController = navController)
        }
    }
}