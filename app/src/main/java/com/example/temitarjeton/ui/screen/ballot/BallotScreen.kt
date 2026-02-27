package com.example.temitarjeton.ui.screen.ballot

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.temitarjeton.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WINNING_NUMBER = 5
private val RedX = Color(0xFFFF0000)

private val LightBg = Color(0xFFF7FAFB)

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
            .background(LightBg)
            .padding(16.dp)
    ) {

        // ===== FILA 1 (30%) =====
        HeaderRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.30f),
            onStaffReset = onStaffReset
        )

        Spacer(Modifier.height(12.dp))

        // ===== FILA 2 (70%) =====
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
                    .border(2.dp, Color.Black, RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LeftPartyPanel(
                    modifier = Modifier
                        .weight(0.33f)
                        .fillMaxHeight()
                )

                RightNumbersPanel(
                    modifier = Modifier
                        .weight(0.67f)
                        .fillMaxHeight(),
                    enabled = !state.showLoseDialog,
                    onPress = onNumberPressed
                )
            }
        }
    }

    if (state.showLoseDialog) {
        AlertDialog(
            onDismissRequest = onDismissLoseDialog,
            title = { Text("Perdiste 😅", fontSize = 28.sp, fontWeight = FontWeight.Black) },
            text = { Text(state.loseMessage.orEmpty(), fontSize = 20.sp) },
            confirmButton = {
                Button(onClick = onDismissLoseDialog) { Text("Entendido", fontSize = 20.sp) }
            }
        )
    }
}

@Composable
private fun HeaderRow(
    modifier: Modifier,
    onStaffReset: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna 1
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 2.dp
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onStaffReset() })
                    }
                    .padding(10.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.senado_vota_asi),
                    contentDescription = "Al Senado vota así",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
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
                Box(Modifier.fillMaxSize().padding(10.dp)) {
                    Image(
                        painter = painterResource(R.drawable.marca_logo),
                        contentDescription = "Marca el logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp
            ) {
                Box(Modifier.fillMaxSize().padding(10.dp)) {
                    Image(
                        painter = painterResource(R.drawable.marca_numero),
                        contentDescription = "Marca el número",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun LeftPartyPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "PARTIDO ALIANZA VERDE",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )

        val logoShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(logoShape)
                .background(Color(0xFF0E6B3D))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_alianza_verde),
                contentDescription = "Logo Alianza Verde",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            LoopingRedX(
                strokeWidth = 10.dp,
                modifier = Modifier.fillMaxSize()
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
    val numbers = remember { (1..40).toList() }

    var markingNumber by remember { mutableStateOf<Int?>(null) }
    var isMarking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val markProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    Column(
        modifier = modifier
            .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(numbers, key = { it }) { n ->
                val isWinning = (n == WINNING_NUMBER)
                val showXHere = (markingNumber == n) && isMarking

                NumberButton(
                    n = n,
                    enabled = enabled && !isMarking,
                    isWinning = isWinning,
                    showMarkX = showXHere,
                    markProgress = markProgress.value,
                    onPress = { selected ->
                        if (!enabled || isMarking) return@NumberButton

                        isMarking = true
                        markingNumber = selected

                        scope.launch {
                            markProgress.snapTo(0f)
                            markProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 780,
                                    easing = LinearEasing
                                )
                            )

                            delay(80)

                            isMarking = false
                            markingNumber = null

                            onPress(selected)
                        }
                    }
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
    showMarkX: Boolean,
    markProgress: Float,
    onPress: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)

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
        modifier = Modifier
            .height(78.dp)
            .scale(scale)
            .clip(shape)
            .border(3.dp, Color.Black, shape)
            .background(if (enabled) Color(0xFFEFEFEF) else Color(0xFFDADADA), shape)
            .clickable(enabled = enabled) { onPress(n) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = n.toString(),
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
        )

        if (showMarkX) {
            DrawRedX(
                progress = markProgress,
                strokeWidth = 8.dp,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DrawRedX(
    progress: Float,
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val pad = size.minDimension * 0.18f

        val a1 = Offset(pad, pad)
        val b1 = Offset(size.width - pad, size.height - pad)

        val a2 = Offset(size.width - pad, pad)
        val b2 = Offset(pad, size.height - pad)

        val p1 = (progress / 0.5f).coerceIn(0f, 1f)
        val p2 = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)

        if (p1 > 0f) {
            drawLine(
                color = RedX,
                start = a1,
                end = lerp(a1, b1, p1),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        }
        if (p2 > 0f) {
            drawLine(
                color = RedX,
                start = a2,
                end = lerp(a2, b2, p2),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun LoopingRedX(
    strokeWidth: Dp,
    modifier: Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "loopX")

    val progress by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1300
                0f at 0
                1f at 900
                1f at 1150 // pausa
                0f at 1300 // reinicio (snap)
            }
        ),
        label = "loopProgress"
    )

    DrawRedX(
        progress = progress,
        strokeWidth = strokeWidth,
        modifier = modifier
    )
}

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(
        x = a.x + (b.x - a.x) * t,
        y = a.y + (b.y - a.y) * t
    )