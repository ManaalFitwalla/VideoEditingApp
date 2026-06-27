package com.example.fabcut

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class EditorActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var filterRecyclerView: RecyclerView

    private lateinit var btnFilter: Button
    private lateinit var btnText: Button
    private lateinit var btnSticker: Button
    private lateinit var btnCrop: Button
    private lateinit var btnAdjust: Button

    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        imagePreview = findViewById(R.id.imagePreview)
        filterRecyclerView = findViewById(R.id.filterRecyclerView)

        btnFilter = findViewById(R.id.btnFilter)
        btnText = findViewById(R.id.btnText)
        btnSticker = findViewById(R.id.btnSticker)
        btnCrop = findViewById(R.id.btnCrop)
        btnAdjust = findViewById(R.id.btnAdjust)

        val mediaUri = intent.getStringExtra("MEDIA_URI")

        if (mediaUri != null) {

            Glide.with(this)
                .asBitmap()
                .load(Uri.parse(mediaUri))
                .into(object : CustomTarget<Bitmap>() {

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        originalBitmap = resource
                        imagePreview.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(
                        placeholder: Drawable?
                    ) {
                    }
                })
        }

        val filters = listOf(
            FilterItem("Original"),
            FilterItem("Bright"),
            FilterItem("Cool"),
            FilterItem("Warm"),
            FilterItem("Vintage"),
            FilterItem("B&W")
        )

        filterRecyclerView.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        filterRecyclerView.visibility = View.GONE

        filterRecyclerView.adapter =
            FilterAdapter(filters) {
                // Add filter logic later
            }

        btnFilter.setOnClickListener {
            filterRecyclerView.visibility = View.VISIBLE
        }

        btnText.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
        }

        btnSticker.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
        }

        btnCrop.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
        }

        btnAdjust.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
        }
    }
}