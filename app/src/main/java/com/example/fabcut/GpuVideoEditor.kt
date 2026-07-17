package com.example.fabcut

import android.content.Context
import android.net.Uri
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@UnstableApi
class GpuVideoEditor(
    private val context: Context,
    private val playerView: PlayerView
) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        playerView.player = player
        player.repeatMode = Player.REPEAT_MODE_ALL
    }

    fun loadVideo(uri: Uri) {
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun applyFilter(effect: Effect) {
        player.setVideoEffects(listOf(effect))
    }

    fun clearFilters() {
        player.setVideoEffects(emptyList())
    }

    fun pause() {
        player.pause()
    }

    fun play() {
        player.play()
    }

    fun release() {
        player.release()
    }
}