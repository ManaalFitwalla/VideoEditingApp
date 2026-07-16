package com.example.fabcut.stickers

import android.graphics.Bitmap

data class StickerEntity(
    val bitmap: Bitmap,
    var x: Float,
    var y: Float,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var flipped: Boolean = false,
    var selected: Boolean = false
)