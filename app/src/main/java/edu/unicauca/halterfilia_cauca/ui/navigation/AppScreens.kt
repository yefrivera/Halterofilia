package edu.unicauca.halterfilia_cauca.ui.navigation

sealed class AppScreens(val route: String) {
    object LoginScreen : AppScreens("login_screen")
    object RegisterScreen : AppScreens("register_screen")
    object AthleteScreen : AppScreens("athlete_screen")
    object AthleteRegistrationScreen : AppScreens("athlete_registration_screen")
    object BluetoothScreen : AppScreens("bluetooth_screen")
    object HistoryScreen : AppScreens("history_screen")
    object MedidasScreen : AppScreens("medidas_screen")
}
