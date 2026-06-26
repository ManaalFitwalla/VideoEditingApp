package com.example.fabcut

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MediaAdapter
    private lateinit var txtSelectedCount: TextView
    private lateinit var btnProceed: Button

    private lateinit var mediaList: MutableList<MediaItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_gallery)

        txtSelectedCount =
            findViewById(R.id.txtSelectedCount)

        btnProceed =
            findViewById(R.id.btnProceed)

        recyclerView =
            findViewById(R.id.recyclerView)

        recyclerView.layoutManager =
            GridLayoutManager(this, 3)

        recyclerView.addItemDecoration(
            ItemSpacingDecoration(8)
        )

        btnProceed.visibility = View.GONE

        val mediaType =
            intent.getStringExtra("MEDIA_TYPE") ?: "PHOTO"

        val maxLimit =
            if (mediaType == "VIDEO")
                10
            else
                20

        txtSelectedCount.text =
            "0 / $maxLimit Selected"

        mediaList =
            loadMedia(mediaType)

        adapter = MediaAdapter(

            mediaList,

            { mediaItem ->

                val previewIntent =
                    Intent(
                        this,
                        PreviewActivity::class.java
                    )

                previewIntent.putExtra(
                    "MEDIA_URI",
                    mediaItem.uri.toString()
                )

                previewIntent.putExtra(
                    "IS_VIDEO",
                    mediaItem.isVideo
                )

                startActivity(previewIntent)
            },

            { count ->

                txtSelectedCount.text =
                    "$count / $maxLimit Selected"

                btnProceed.visibility =
                    if (count > 0)
                        View.VISIBLE
                    else
                        View.GONE
            }
        )

        recyclerView.adapter =
            adapter

        btnProceed.setOnClickListener {

            val selectedUris =
                ArrayList<String>()

            mediaList.forEach {

                if (it.isSelected) {

                    selectedUris.add(
                        it.uri.toString()
                    )
                }
            }

            if (selectedUris.isNotEmpty()) {

                val editorIntent =
                    Intent(
                        this,
                        EditorActivity::class.java
                    )

                editorIntent.putStringArrayListExtra(
                    "SELECTED_MEDIA",
                    selectedUris
                )

                startActivity(editorIntent)
            }
        }
    }

    private fun loadMedia(
        type: String
    ): MutableList<MediaItem> {

        val mediaList =
            mutableListOf<MediaItem>()

        val projection =
            arrayOf(
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
            arrayOf(
                mediaTypeValue.toString()
            )

        val uri =
            MediaStore.Files.getContentUri(
                "external"
            )

        val cursor =
            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            )

        cursor?.use {

            val idColumn =
                it.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns._ID
                )

            val typeColumn =
                it.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.MEDIA_TYPE
                )

            while (it.moveToNext()) {

                val id =
                    it.getLong(idColumn)

                val mediaType =
                    it.getInt(typeColumn)

                val contentUri: Uri =

                    if (
                        mediaType ==
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    ) {

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
                        mediaType ==
                                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    )
                )
            }
        }

        return mediaList
    }
}