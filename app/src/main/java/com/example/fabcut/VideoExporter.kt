package com.example.fabcut

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

@UnstableApi
object VideoExporter {

    fun trimVideo(
        context: Context,
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File,
        onSuccess: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {

        if (outputFile.exists()) outputFile.delete()

        val mediaItem = MediaItem.fromUri(inputUri)

        val editedItem = EditedMediaItem.Builder(mediaItem).build()

        val transformer = Transformer.Builder(context).build()

        transformer.start(
            editedItem,
            outputFile.absolutePath
        )

        transformer.addListener(object : Transformer.Listener {

            override fun onCompleted(
                composition: androidx.media3.transformer.Composition,
                exportResult: ExportResult
            ) {
                onSuccess(outputFile)
            }

            override fun onError(
                composition: androidx.media3.transformer.Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                onError(exportException)
            }
        })
    }
}