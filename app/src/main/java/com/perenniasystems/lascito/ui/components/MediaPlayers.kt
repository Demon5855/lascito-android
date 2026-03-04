package com.perenniasystems.lascito.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.perenniasystems.lascito.ui.theme.CardBorder
import com.perenniasystems.lascito.ui.theme.PlayfairDisplay
import com.perenniasystems.lascito.ui.theme.SurfaceDark
import com.perenniasystems.lascito.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

// ----------------------------------------------------------------------------
// CustomAudioPlayer — reproductor dinámico Premium
// ----------------------------------------------------------------------------
@Composable
fun CustomAudioPlayer(url: String) {
    if (url.isBlank()) return

    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(url)))
            prepare()
            playWhenReady = false
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var sliderScrubbingValue by remember { mutableStateOf<Float?>(null) } // Temp state when dragging

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                delay(100)
            }
        }
    }

    // Determine current slider visually
    val actualPosition = sliderScrubbingValue?.toLong() ?: currentPosition
    val displayRange = 0f..(duration.coerceAtLeast(1L).toFloat())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector        = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayCircle,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(48.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Slider(
                value = actualPosition.toFloat(),
                onValueChange = { newValue ->
                    sliderScrubbingValue = newValue
                },
                onValueChangeFinished = {
                    sliderScrubbingValue?.let {
                        exoPlayer.seekTo(it.toLong())
                    }
                    sliderScrubbingValue = null
                },
                valueRange = displayRange,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = CardBorder
                ),
                modifier = Modifier.height(24.dp)
            )
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = formatMillisToMMSS(actualPosition),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color    = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = PlayfairDisplay
                    )
                )
                Text(
                    text  = formatMillisToMMSS(duration),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = TextSecondary,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlayfairDisplay
                    )
                )
            }
        }
    }
}

fun formatMillisToMMSS(millis: Long): String {
    if (millis < 0) return "00:00"
    val totalSeconds = (millis / 1000).toInt()
    val min = totalSeconds / 60
    val sec = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", min, sec)
}

@Composable
fun MediaPreviewPlayer(uri: Uri, isAudioOnly: Boolean = false) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(uri) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                if (isAudioOnly) {
                    // Hide UI that implies video if it's strictly audio
                    controllerAutoShow = true
                    controllerShowTimeoutMs = -1 // Don't hide the controls
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
