package com.perenniasystems.lascito.core.media

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * Único punto de creación de ExoPlayer en la app.
 *
 * Crea un reproductor preparado (sin reproducir) para [uri] y lo libera cuando
 * sale de composición o cambia [uri].
 */
@Composable
fun rememberExoPlayer(uri: Uri): ExoPlayer {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}
