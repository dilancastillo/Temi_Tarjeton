package com.example.temitarjeton.ui.screen.ballot

import com.example.temitarjeton.R
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

private const val WINNING_NUMBER = 5

@Composable
fun BallotScreen(
    state: BallotUiState.Ready,
    onNumberPressed: (Int) -> Unit,
    onDismissLoseDialog: () -> Unit,
    onStaffReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(18.dp)
    ) {
        Text(
            text = "TARJETÓN",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onStaffReset() })
                }
        )

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxSize()) {
            // Panel izquierdo (partido + logo simplificado)
            LeftPartyPanel(
                modifier = Modifier
                    .weight(1.35f)
                    .fillMaxHeight()
            )

            Spacer(Modifier.width(18.dp))

            // Panel derecho (botones)
            RightNumbersPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                enabled = !state.showLoseDialog,
                onPress = onNumberPressed
            )
        }

        // Footer
        Spacer(Modifier.height(10.dp))
        Text(
            text = "PREFERENTE",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    if (state.showLoseDialog) {
        val scroll = rememberScrollState()

        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.92f),
            onDismissRequest = onDismissLoseDialog,
            title = {
                Text(
                    text = "Perdiste 😅",
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(min = 140.dp, max = 260.dp)
                        .verticalScroll(scroll)
                ) {
                    Text(
                        text = state.loseMessage.orEmpty(),
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissLoseDialog,
                    modifier = Modifier.height(64.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Entendido",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun LeftPartyPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "PARTIDO ALIANZA VERDE",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )

        // “Logo” estilo tarjetón
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF0E6B3D), RoundedCornerShape(10.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_alianza_verde),
                contentDescription = "Logo Alianza Verde",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = "Selecciona el número ganador",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RightNumbersPanel(
    modifier: Modifier,
    enabled: Boolean,
    onPress: (Int) -> Unit
) {
    val numbers = remember { (1..32).toList() }

    Column(
        modifier = modifier
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(numbers, key = { it }) { n ->
                NumberButton(
                    n = n,
                    enabled = enabled,
                    isWinning = (n == WINNING_NUMBER),
                    onPress = onPress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp) // ajustado para que 5 filas quepan cómodo
                )
            }
        }
    }
}

@Composable
private fun NumberButton(
    n: Int,
    enabled: Boolean,
    isWinning: Boolean,
    onPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    // Pulso arcade SOLO para el número ganador (5)
    val scale = if (isWinning) {
        val inf = rememberInfiniteTransition(label = "pulse")
        inf.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(420),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        ).value
    } else 1.0f

    Box(
        modifier = modifier
            .scale(scale)
            .border(3.dp, Color.Black, shape)
            .background(if (enabled) Color(0xFFEFEFEF) else Color(0xFFDADADA), shape)
            .clickable(enabled = enabled) { onPress(n) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = n.toString(),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black
        )
    }
}