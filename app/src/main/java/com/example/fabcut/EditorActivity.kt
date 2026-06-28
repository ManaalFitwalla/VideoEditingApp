package com.example.fabcut

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class EditorActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var collageCanvasContainer: LinearLayout
    private lateinit var filterRecyclerView: RecyclerView

    private lateinit var btnFilter: Button
    private lateinit var btnText: Button
    private lateinit var btnSticker: Button
    private lateinit var btnCrop: Button
    private lateinit var btnLayoutFormat: Button

    private var originalBitmap: Bitmap? = null
    private var selectedMediaList: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        imagePreview = findViewById(R.id.imagePreview)
        collageCanvasContainer = findViewById(R.id.collageCanvasContainer)
        filterRecyclerView = findViewById(R.id.filterRecyclerView)

        btnFilter = findViewById(R.id.btnFilter)
        btnText = findViewById(R.id.btnText)
        btnSticker = findViewById(R.id.btnSticker)
        btnCrop = findViewById(R.id.btnCrop)
        btnLayoutFormat = findViewById(R.id.btnLayoutFormat)

        // Fetch inputs for both modes
        val singleMediaUri = intent.getStringExtra("MEDIA_URI")
        selectedMediaList = intent.getStringArrayListExtra("SELECTED_MEDIA")

        // CONDITION: Decide if we are rendering a single item or a collage grid
        if (selectedMediaList != null && selectedMediaList!!.size > 1) {
            imagePreview.visibility = View.GONE
            collageCanvasContainer.visibility = View.VISIBLE
            btnLayoutFormat.visibility = View.VISIBLE

            generateCollageLayout(LinearLayout.HORIZONTAL)
        } else {
            imagePreview.visibility = View.VISIBLE
            collageCanvasContainer.visibility = View.GONE
            btnLayoutFormat.visibility = View.GONE

            val uriToLoad = singleMediaUri ?: selectedMediaList?.firstOrNull()

            if (uriToLoad != null) {
                Glide.with(this)
                    .asBitmap()
                    .load(Uri.parse(uriToLoad))
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            originalBitmap = resource
                            imagePreview.setImageBitmap(resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }

        // Handle collage formatting clicks
        btnLayoutFormat.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
            generateCollageLayout(LinearLayout.VERTICAL)
        }

        // Setup filter system items
        val filters = listOf(
            FilterItem("Original"), FilterItem("Bright"), FilterItem("Cool"),
            FilterItem("Warm"), FilterItem("Vintage"), FilterItem("B&W")
        )

        filterRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        filterRecyclerView.visibility = View.GONE
        filterRecyclerView.adapter = FilterAdapter(filters) {
            // Filter logic implementation area
        }

        btnFilter.setOnClickListener {
            if (filterRecyclerView.visibility == View.VISIBLE) {
                filterRecyclerView.visibility = View.GONE
            } else {
                filterRecyclerView.visibility = View.VISIBLE
            }
        }

        // Hide menus gracefully
        val directTools = listOf(btnText, btnSticker, btnCrop)
        directTools.forEach { button ->
            button.setOnClickListener { filterRecyclerView.visibility = View.GONE }
        }
    }

    // Dynamic Full-Bleed Grid Generator
    private fun generateCollageLayout(orientation: Int) {
        collageCanvasContainer.removeAllViews()
        collageCanvasContainer.orientation = orientation

        selectedMediaList?.forEach { uriString ->
            val ivItem = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f
                ).apply {
                    if (orientation == LinearLayout.VERTICAL) {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                        height = 0
                    }
                    setMargins(2, 2, 2, 2)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP // Grid segments stretch fully!
            }
            collageCanvasContainer.addView(ivItem)
            Glide.with(this).load(Uri.parse(uriString)).into(ivItem)
        }
    }
}