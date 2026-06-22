package com.example.fabcut

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    var isSelected: Boolean = false
)