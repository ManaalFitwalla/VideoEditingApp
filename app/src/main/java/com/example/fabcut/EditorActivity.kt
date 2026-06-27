package com.example.fabcut

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

class EditorActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var collageCanvasContainer: LinearLayout
    private lateinit var filterRecyclerView: RecyclerView
    private lateinit var collageFormatsRecyclerView: RecyclerView
    private lateinit var selectedMediaRecyclerView: RecyclerView

    private lateinit var btnFilter: Button
    private lateinit var btnLayoutFormat: Button

    private var selectedMedia: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NOTE: We do NOT call enableEdgeToEdge() here. This forces the
        // layout to stay safely below the system status bar and above the nav bar.
        setContentView(R.layout.activity_editor)

        selectedMediaRecyclerView = findViewById(R.id.selectedMediaRecyclerView)
        imagePreview = findViewById(R.id.imagePreview)
        collageCanvasContainer = findViewById(R.id.collageCanvasContainer)
        filterRecyclerView = findViewById(R.id.filterRecyclerView)
        collageFormatsRecyclerView = findViewById(R.id.collageFormatsRecyclerView)
        btnFilter = findViewById(R.id.btnFilter)
        btnLayoutFormat = findViewById(R.id.btnLayoutFormat)

        selectedMedia = intent.getStringArrayListExtra("SELECTED_MEDIA")

        // CHECK: If more than 1 image is selected, activate automatic collage generator mode
        if (selectedMedia != null && selectedMedia!!.size > 1) {
            imagePreview.visibility = View.GONE
            collageCanvasContainer.visibility = View.VISIBLE
            btnLayoutFormat.visibility = View.VISIBLE

            // Build the default grid style automatically
            generateCollageLayout(LinearLayout.HORIZONTAL)
        } else if (!selectedMedia.isNullOrEmpty()) {
            imagePreview.visibility = View.VISIBLE
            collageCanvasContainer.visibility = View.GONE
            btnLayoutFormat.visibility = View.GONE

            Glide.with(this)
                .load(Uri.parse(selectedMedia!![0]))
                .into(imagePreview)
        }

        // Layout Formats Action Trigger
        btnLayoutFormat.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
            collageFormatsRecyclerView.visibility = View.VISIBLE

            // Toggle formatting orientations dynamically
            generateCollageLayout(LinearLayout.VERTICAL)
        }

        // Filters configuration setup
        val filters = listOf(FilterItem("Original"), FilterItem("Bright"), FilterItem("Cool"), FilterItem("Warm"))
        filterRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        selectedMediaRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        selectedMediaRecyclerView.adapter = SelectedMediaAdapter(selectedMedia ?: arrayListOf()) { uri ->
            if (imagePreview.visibility == View.VISIBLE) {
                Glide.with(this).load(Uri.parse(uri)).into(imagePreview)
            }
        }

        btnFilter.setOnClickListener {
            collageFormatsRecyclerView.visibility = View.GONE
            filterRecyclerView.visibility = View.VISIBLE
            filterRecyclerView.adapter = FilterAdapter(filters) {}
        }
    }

    // Dynamic View Grid Generator function
    private fun generateCollageLayout(orientation: Int) {
        collageCanvasContainer.removeAllViews()
        collageCanvasContainer.orientation = orientation

        selectedMedia?.forEach { uriString ->
            val ivItem = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f
                ).apply {
                    if (orientation == LinearLayout.VERTICAL) {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                        height = 0
                    }
                    setMargins(4, 4, 4, 4)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            collageCanvasContainer.addView(ivItem)
            Glide.with(this).load(Uri.parse(uriString)).into(ivItem)
        }
    }
}