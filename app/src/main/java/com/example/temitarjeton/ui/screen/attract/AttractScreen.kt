package com.example.temitarjeton.ui.screen.attract

import android.net.Uri
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Modo “atracción”:
 * - Reproduce un video en loop (sin controles, full screen)
 * - Cada 3 loops, dispara [onEveryThreeLoops] (ideal para: parar movimiento + TTS)
 * - Si el usuario toca la pantalla, dispara [onTapToPlay]
 */
@Composable
fun AttractScreen(
    videoUriProvider: () -> Uri,
    onTapToPlay: () -> Unit,
    onEveryThreeLoops: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uri = remember { videoUriProvider() }

    var loopCount by remember { mutableIntStateOf(0) }
    var pendingAnnouncement by remember { mutableStateOf(false) }

    val exoPlayer = remember(context, uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
            volume = 1f
        }
    }

    // Listener: manual loop para poder contar repeticiones.
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_ENDED) return

                loopCount += 1

                // Cada 3 loops: paramos antes de reiniciar para anunciar.
                if (loopCount % 3 == 0) {
                    pendingAnnouncement = true
                    exoPlayer.playWhenReady = false
                } else {
                    exoPlayer.seekTo(0)
                    exoPlayer.playWhenReady = true
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Cada 3 loops: mute temporal + callback (parar movimiento + TTS) + reanudar video.
    LaunchedEffect(pendingAnnouncement) {
        if (!pendingAnnouncement) return@LaunchedEffect

        // Evita que el audio del video se mezcle con el TTS.
        val prevVolume = exoPlayer.volume
        exoPlayer.volume = 0f

        runCatching { onEveryThreeLoops() }

        // Si seguimos en esta pantalla (no navegación), reanudamos el loop.
        exoPlayer.volume = prevVolume
        exoPlayer.seekTo(0)
        exoPlayer.playWhenReady = true
        pendingAnnouncement = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onTapToPlay() },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = android.widget.FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            update = { view -> view.player = exoPlayer }
        )

        Text(
            text = "TOCA PARA JUGAR",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}
