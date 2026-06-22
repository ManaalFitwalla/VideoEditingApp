package com.example.fabcut

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MediaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_gallery)

        recyclerView = findViewById(R.id.recyclerView)

        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val mediaType =
            intent.getStringExtra("MEDIA_TYPE") ?: "PHOTO"

        val mediaList = loadMedia(mediaType)

        adapter = MediaAdapter(mediaList)

        recyclerView.adapter = adapter
    }

    private fun loadMedia(type: String): MutableList<MediaItem> {

        val mediaList = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )

        val mediaTypeValue =
            if (type == "PHOTO")
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
            else
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"

        val selectionArgs =
            arrayOf(mediaTypeValue.toString())

        val uri = MediaStore.Files.getContentUri("external")

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        )

        cursor?.use {

            val idColumn =
                it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)

            val typeColumn =
                it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (it.moveToNext()) {

                val id = it.getLong(idColumn)
                val mediaType = it.getInt(typeColumn)

                val contentUri: Uri =
                    if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {

                        Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                    } else {

                        Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )
                    }

                mediaList.add(
                    MediaItem(
                        contentUri,
                        mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    )
                )
            }
        }

        return mediaList
    }
}