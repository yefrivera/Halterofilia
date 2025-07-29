package edu.unicauca.halterfilia_cauca.ui.navigation

sealed class AppScreens(val route: String) {
    object LoginScreen : AppScreens("login")
    object RegisterScreen : AppScreens("register")
    object AthleteScreen : AppScreens("athlete")
    object AthleteRegistrationScreen : AppScreens("athlete_registration")
    object BluetoothScreen : AppScreens("bluetooth")
    object HistoryScreen : AppScreens("history")
    object MedidasScreen : AppScreens("medidas")
}
