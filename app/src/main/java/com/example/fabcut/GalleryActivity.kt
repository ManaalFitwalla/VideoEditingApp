package com.example.fabcut
import android.provider.MediaStore
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GalleryActivity : AppCompatActivity() {
    val mediaList = ArrayList<MediaItem>()
    private lateinit var mediaAdapter: MediaAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        recyclerView.layoutManager = GridLayoutManager(this, 3)


        mediaAdapter = MediaAdapter(mediaList)
        recyclerView.adapter = mediaAdapter
        loadImages()}
    private fun loadImages() {

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID
        )

        val cursor = contentResolver.query(
            collection,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {

            val idColumn = it.getColumnIndexOrThrow(
                MediaStore.Images.Media._ID
            )

            while (it.moveToNext()) {

                val id = it.getLong(idColumn)

                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                mediaList.add(
                    MediaItem(uri.toString())
                )
            }
        }
        mediaAdapter.notifyDataSetChanged()
    }
}