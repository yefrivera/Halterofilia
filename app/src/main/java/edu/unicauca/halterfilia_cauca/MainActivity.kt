package edu.unicauca.halterfilia_cauca

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

import edu.unicauca.halterfilia_cauca.ui.features.login.LoginScreen // Importa tu pantalla
import edu.unicauca.halterfilia_cauca.ui.navigation.Navigation
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme // Import tu tema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HalterofiliaCaucaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigation() // Aquí llamas a tu pantalla
                }
            }
        }
    }
}