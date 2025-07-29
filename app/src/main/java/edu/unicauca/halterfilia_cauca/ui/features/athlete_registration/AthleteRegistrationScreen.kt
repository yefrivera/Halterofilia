package edu.unicauca.halterfilia_cauca.ui.features.athlete_registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.unicauca.halterfilia_cauca.ui.theme.HalterofiliaCaucaTheme

import androidx.navigation.NavController
import edu.unicauca.halterfilia_cauca.ui.navigation.AppScreens

/**
 * Composable Stateful
 */
@Composable
fun AthleteRegistrationScreen(
    navController: NavController,
    newAthleteViewModel: NewAthleteViewModel = viewModel()
) {
    NewAthleteContent(
        fullName = newAthleteViewModel.fullName,
        age = newAthleteViewModel.age,
        height = newAthleteViewModel.height,
        weight = newAthleteViewModel.weight,
        onFullNameChange = { newAthleteViewModel.onFullNameChange(it) },
        onAgeChange = { newAthleteViewModel.onAgeChange(it) },
        onHeightChange = { newAthleteViewModel.onHeightChange(it) },
        onWeightChange = { newAthleteViewModel.onWeightChange(it) },
        onAddAthleteClicked = {
            // Ejemplo: Navegar de vuelta a la pantalla de atletas
            navController.popBackStack()
        },
        onNavigateBack = {
            navController.popBackStack()
        }
    )
}

/**
 * Composable Stateless que dibuja el formulario
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAthleteContent(
    fullName: String,
    age: String,
    height: String,
    weight: String,
    onFullNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onAddAthleteClicked: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nuevo deportista") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Hacemos la columna desplazable
        ) {
            // Campos de texto del formulario
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text("Nombres y apellidos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text("Edad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = height,
                onValueChange = onHeightChange,
                label = { Text("Estatura") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Peso") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.weight(1f)) // Empuja el botón hacia abajo

            // Botón para agregar
            Button(
                onClick = onAddAthleteClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-30).dp),
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agregar")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Agregar", fontSize = 16.sp)
            }
        }
    }
}

/**
 * Previsualización
 */
@Preview(showBackground = true)
@Composable
fun NewAthleteScreenPreview() {
    HalterofiliaCaucaTheme {
        NewAthleteContent(
            fullName = "Yefri Estiven Vera",
            age = "25",
            height = "180",
            weight = "80",
            onFullNameChange = {},
            onAgeChange = {},
            onHeightChange = {},
            onWeightChange = {},
            onAddAthleteClicked = {},
            onNavigateBack = {}
        )
    }
}