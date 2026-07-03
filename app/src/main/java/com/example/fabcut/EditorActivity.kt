package com.example.fabcut

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.card.MaterialCardView
class EditorActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var filterRecyclerView: RecyclerView

    private lateinit var btnFilter: MaterialCardView
    private lateinit var btnText: MaterialCardView

    private lateinit var btnSticker: MaterialCardView
    private lateinit var btnCrop: MaterialCardView
    private lateinit var btnAdjust: MaterialCardView

    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

                        val thumbnail = Bitmap.createScaledBitmap(
                            resource,
                            150,
                            150,
                            true
                        )

                        imagePreview.setImageBitmap(resource)
                        val filters = listOf(
                            FilterItem("Original", thumbnail),
                            FilterItem("Bright", ImageFilters.bright(thumbnail)),
                            FilterItem("Cool", ImageFilters.cool(thumbnail)),
                            FilterItem("Warm", ImageFilters.warm(thumbnail)),
                            FilterItem("Vintage", ImageFilters.vintage(thumbnail)),
                            FilterItem("B&W", ImageFilters.blackAndWhite(thumbnail))
                        )
                        filterRecyclerView.adapter =
                            FilterAdapter(filters) { selectedFilter ->

                                when (selectedFilter.name) {

                                    "Original" ->
                                        imagePreview.setImageBitmap(resource)
                                    "Bright" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.bright(resource)
                                        )
                                    "Warm" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.warm(resource)
                                        )
                                    "Cool" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.cool(resource)
                                        )
                                    "Vintage" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.vintage(resource)
                                        )

                                    "B&W" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.blackAndWhite(resource)
                                        )

                                    else ->
                                        imagePreview.setImageBitmap(resource)
                                }

                            }

                    }

                    override fun onLoadCleared(
                        placeholder: Drawable?
                    ) {
                    }
                })
        }



        filterRecyclerView.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        filterRecyclerView.visibility = View.GONE



        // Toggle filter bar
        btnFilter.setOnClickListener {

            if (filterRecyclerView.visibility == View.VISIBLE) {
                filterRecyclerView.visibility = View.GONE
            } else {
                filterRecyclerView.visibility = View.VISIBLE
            }

        }

        // Hide filter bar when other tools are pressed
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