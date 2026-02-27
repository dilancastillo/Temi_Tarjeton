package com.example.temitarjeton.ui.screen.ballot

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.temitarjeton.R

private const val WINNING_NUMBER = 5

private val Teal = Color(0xFF0A5A66)
private val Green = Color(0xFF74B62E)
private val LightBg = Color(0xFFF7FAFB)

@Composable
fun BallotScreen(
    state: BallotUiState.Ready,
    onNumberPressed: (Int) -> Unit,
    onDismissLoseDialog: () -> Unit,
    onAnyInteraction: () -> Unit = {},
    onStaffReset: () -> Unit,
) {

    val anyTouchModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            onAnyInteraction()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .then(anyTouchModifier)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.30f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp
            ) {
                Image(
                    painter = painterResource(R.drawable.senado_vota_asi),
                    contentDescription = "Al Senado vota así",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp
                ) {
                    Image(
                        painter = painterResource(R.drawable.marca_logo),
                        contentDescription = "Marca el logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 2.dp
                ) {
                    Image(
                        painter = painterResource(R.drawable.marca_numero),
                        contentDescription = "Marca el número",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.70f),
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Teal, RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LeftTicketPanel(
                    modifier = Modifier
                        .weight(0.30f)
                        .fillMaxHeight()
                )
                NumbersTicketGrid(
                    modifier = Modifier
                        .weight(0.70f)
                        .fillMaxHeight(),
                    enabled = !state.showLoseDialog,
                    onPress = { n ->
                        onAnyInteraction()
                        onNumberPressed(n)
                    }
                )
            }
        }
    }

    if (state.showLoseDialog) {
        LoseDialogLarge(
            message = state.loseMessage.orEmpty(),
            onDismiss = onDismissLoseDialog
        )
    }
}

@Composable
private fun LeftTicketPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(2.dp, Color(0xFF111111), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "ALIANZA POR COLOMBIA",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF111111)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF0E6B3D))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_alianza_verde),
                    contentDescription = "Logo Alianza Verde",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Text(
            text = "PREFERENTE",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF111111),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun NumbersTicketGrid(
    modifier: Modifier,
    enabled: Boolean,
    onPress: (Int) -> Unit
) {
    val numbers = remember { (1..40).toList() }

    Column(
        modifier = modifier
            .border(2.dp, Color(0xFF111111), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8), // 8 columnas x 5 filas = 40
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(numbers, key = { it }) { n ->
                TicketNumberCell(
                    n = n,
                    enabled = enabled,
                    isWinning = (n == WINNING_NUMBER),
                    onPress = onPress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                )
            }
        }
    }
}

@Composable
private fun TicketNumberCell(
    n: Int,
    enabled: Boolean,
    isWinning: Boolean,
    onPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    // Pulso arcade para el ganador
    val scale = if (isWinning) {
        val inf = rememberInfiniteTransition(label = "pulse")
        inf.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(380),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        ).value
    } else 1.0f

    val borderColor = when {
        isWinning -> Green
        else -> Color(0xFFB9C2C6)
    }

    val bgColor = when {
        !enabled -> Color(0xFFE6E6E6)
        isWinning -> Color(0xFFEAF7DA)
        else -> Color(0xFFF3F5F6)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .border(3.dp, borderColor, shape)
            .background(bgColor, shape)
            .pointerInput(enabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (enabled) onPress(n)
                    // no consumimos el evento (down) para mantener fluidez
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = n.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Teal
        )
    }
}

@Composable
private fun LoseDialogLarge(
    message: String,
    onDismiss: () -> Unit
) {
    val scroll = rememberScrollState()

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.92f),
        onDismissRequest = onDismiss,
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
                    text = message,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
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