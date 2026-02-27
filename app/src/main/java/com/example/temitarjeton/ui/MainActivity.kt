package com.example.temitarjeton.ui

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.example.temitarjeton.data.audio.SfxPlayer
import com.example.temitarjeton.data.temi.TemiRobotController
import com.example.temitarjeton.data.video.VideoProvider
import com.example.temitarjeton.ui.navigation.AppNavGraph
import com.example.temitarjeton.ui.navigation.Routes
import com.example.temitarjeton.ui.screen.attract.AttractScreen
import com.example.temitarjeton.ui.screen.ballot.BallotEvent
import com.example.temitarjeton.ui.screen.ballot.BallotScreen
import com.example.temitarjeton.ui.screen.ballot.BallotUiState
import com.example.temitarjeton.ui.screen.ballot.BallotViewModel
import com.example.temitarjeton.ui.screen.video.VideoScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {

    private val vm: BallotViewModel by viewModels()

    // Opción 1: kiosco OFF (para poder salir con el menú superior).
    private val kioskEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!kioskEnabled) {
            runCatching { stopLockTask() }
        } else {
            runCatching { startLockTask() }
        }

        setContent {
            val nav = rememberNavController()

            val temi = remember { TemiRobotController() }
            val sfx = remember { SfxPlayer(this) }
            val videoProvider = remember { VideoProvider(this) }

            // ---- Inactividad (15s) ----
            var lastInteractionMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
            fun markInteraction() {
                lastInteractionMs = SystemClock.elapsedRealtime()
            }

            // Seguimos el route actual para decidir qué hacer con eventos del robot.
            var currentRoute by remember { mutableStateOf(Routes.Attract) }

            LaunchedEffect(nav) {
                nav.currentBackStackEntryFlow.collect { entry ->
                    currentRoute = entry.destination.route ?: Routes.Attract
                }
            }

            // Limpieza
            DisposableEffect(Unit) {
                onDispose {
                    sfx.release()
                    temi.dispose()
                }
            }

            // 1) Eventos one-off del juego (win/lose/tts/nav)
            LaunchedEffect(Unit) {
                vm.events.collect { e ->
                    when (e) {
                        BallotEvent.PlayWin -> sfx.playWin()
                        BallotEvent.PlayLose -> sfx.playLose()

                        is BallotEvent.Speak -> temi.speakAndWait(e.text)

                        BallotEvent.NavigateToVideo -> {
                            nav.navigate(Routes.Video) { launchSingleTop = true }
                        }
                    }
                }
            }

            // 2) Interacción física con Temi (touch / botón interacción/seguir)
            LaunchedEffect(Unit) {
                temi.interactionEvents.collect {
                    if (currentRoute == Routes.Attract) {
                        markInteraction()
                        temi.stopPatrol()
                        nav.navigate(Routes.Ballot) { launchSingleTop = true }
                    }
                }
            }

            // 3) Timer de inactividad: si estás en tarjetón y pasan 15s sin tocar, vuelve a atracción.
            LaunchedEffect(Unit) {
                while (true) {
                    delay(500)
                    val elapsed = SystemClock.elapsedRealtime() - lastInteractionMs

                    // Solo aplica cuando el usuario está “esperando” en el tarjetón.
                    if (currentRoute == Routes.Ballot && elapsed >= 15_000L) {
                        vm.staffReset() // limpia popup/estado
                        // Volvemos sin duplicar pantallas: preferimos popBackStack.
                        val popped = nav.popBackStack(Routes.Attract, inclusive = false)
                        if (!popped) {
                            nav.navigate(Routes.Attract) { launchSingleTop = true }
                        }
                    }
                }
            }

            AppNavGraph(
                navController = nav,
                attract = {
                    // Al entrar a atracción: arrancamos patrulla. Al salir: la paramos.
                    DisposableEffect(Unit) {
                        val route = temi.buildPatrolRoute(prefix = "ubicacion", maxLocations = 3)
                        temi.startPatrol(route) // si route está vacío, no se mueve (requisito #7)
                        onDispose { temi.stopPatrol() }
                    }

                    AttractScreen(
                        videoUriProvider = { videoProvider.attractLoopVideoUri() },
                        onTapToPlay = {
                            markInteraction()
                            temi.stopPatrol()
                            nav.navigate(Routes.Ballot) { launchSingleTop = true }
                        },
                        onEveryThreeLoops = {
                            // Requisito #14: cada 3 loops -> parar movimiento + TTS.
                            temi.stopPatrol()
                            temi.speakAndWait(
                                "¡Hola! Toca la pantalla para jugar. O presiona el botón seguir."
                            )

                            // Si nadie interactuó (seguimos en atracción), retomamos patrulla.
                            if (currentRoute == Routes.Attract) {
                                val route = temi.buildPatrolRoute(prefix = "ubicacion", maxLocations = 3)
                                temi.startPatrol(route)
                            }
                        }
                    )
                },
                ballot = {
                    val state = vm.state.collectAsState().value as BallotUiState.Ready
                    BallotScreen(
                        state = state,
                        onNumberPressed = {
                            markInteraction()
                            vm.onNumberPressed(it)
                        },
                        onDismissLoseDialog = {
                            markInteraction()
                            vm.dismissLoseDialog()
                        },
                        onStaffReset = {
                            markInteraction()
                            vm.staffReset()
                        }
                    )
                },
                video = {
                    VideoScreen(
                        videoUriProvider = { videoProvider.candidateVideoUri() },
                        onVideoEnded = {
                            // Sin pantalla final: vuelve al tarjetón.
                            markInteraction() // reinicia el conteo de 15s al terminar el video
                            nav.popBackStack(Routes.Ballot, inclusive = false)
                        }
                    )
                }
            )
        }
    }
}
