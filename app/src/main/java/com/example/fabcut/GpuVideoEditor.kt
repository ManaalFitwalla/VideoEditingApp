package com.example.fabcut

import android.content.Context
import android.net.Uri
import com.daasuu.gpuv.player.GPUPlayerView
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer


class GpuVideoEditor(
    private val context: Context,
    private val gpuPlayerView: GPUPlayerView
) {

    private val player = SimpleExoPlayer.Builder(context).build()

    init {
        gpuPlayerView.player = player
    }

    fun loadVideo(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
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