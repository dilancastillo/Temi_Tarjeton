package com.example.temitarjeton.ui.screen.final

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FinalScreen(
    onSpeakFinal: () -> Unit,
    onBackToStart: () -> Unit
) {
    LaunchedEffect(Unit) { onSpeakFinal() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡GRACIAS!", fontSize = 56.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(14.dp))
        Text("Vota 5. ¡Vota por John Amaya.!", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(30.dp))
        Button(onClick = onBackToStart) { Text("Volver al inicio") }
    }
}