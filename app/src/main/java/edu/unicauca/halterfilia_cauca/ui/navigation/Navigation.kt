package edu.unicauca.halterfilia_cauca.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import edu.unicauca.halterfilia_cauca.ui.features.athlete.AthleteScreen
import edu.unicauca.halterfilia_cauca.ui.features.athlete_registration.AthleteRegistrationScreen
import edu.unicauca.halterfilia_cauca.ui.features.bluetooth_connection.BluetoothScreen
import edu.unicauca.halterfilia_cauca.ui.features.bluetooth_connection.BluetoothViewModel
import edu.unicauca.halterfilia_cauca.ui.features.bluetooth_connection.BluetoothViewModelFactory
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

    // Shared BluetoothViewModel
    val application = LocalContext.current.applicationContext as Application
    val bluetoothViewModel: BluetoothViewModel = viewModel(
        factory = BluetoothViewModelFactory(application)
    )

    NavHost(navController = navController, startDestination = startDestination) {
        composable(route = AppScreens.LoginScreen.route) {
            LoginScreen(navController = navController)
        }
        composable(route = AppScreens.RegisterScreen.route) {
            RegisterScreen(navController = navController)
        }
        composable(route = AppScreens.AthleteScreen.route) {
            AthleteScreen(navController = navController)
        }
        composable(route = AppScreens.AthleteRegistrationScreen.route){
            AthleteRegistrationScreen(navController = navController)
        }
        composable(route = AppScreens.BluetoothScreen.route){
            BluetoothScreen(
                navController = navController,
                bluetoothViewModel = bluetoothViewModel
            )
        }
        composable(route = AppScreens.HistoryScreen.route){
            HistoryScreen(navController = navController)
        }
        composable(
            route = AppScreens.MedidasScreen.route + "/{athleteName}/{athleteId}",
            arguments = listOf(
                navArgument("athleteName") { type = NavType.StringType },
                navArgument("athleteId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val athleteName = backStackEntry.arguments?.getString("athleteName") ?: "Deportista"
            val athleteId = backStackEntry.arguments?.getString("athleteId") ?: ""
            MedidasScreen(
                navController = navController,
                athleteName = athleteName,
                athleteDocId = athleteId,
                bluetoothController = bluetoothViewModel.bluetoothController
            )
        }
    }
}
