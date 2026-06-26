package com.example.fabcut

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EditorActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var filterRecyclerView: RecyclerView
    private lateinit var btnFilter: Button

    private lateinit var selectedMediaRecyclerView: RecyclerView

    private var selectedMedia:
            ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_editor)

        selectedMediaRecyclerView =
            findViewById(R.id.selectedMediaRecyclerView)

        imagePreview =
            findViewById(R.id.imagePreview)

        filterRecyclerView =
            findViewById(R.id.filterRecyclerView)

        btnFilter =
            findViewById(R.id.btnFilter)

        selectedMedia =
            intent.getStringArrayListExtra(
                "SELECTED_MEDIA"
            )

        if (
            selectedMedia != null &&
            selectedMedia!!.isNotEmpty()
        ) {

            Glide.with(this)
                .load(
                    Uri.parse(
                        selectedMedia!![0]
                    )
                )
                .into(imagePreview)
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

        selectedMediaRecyclerView.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        selectedMediaRecyclerView.adapter =
            SelectedMediaAdapter(
                selectedMedia ?: arrayListOf()
            ) { uri ->

                Glide.with(this)
                    .load(Uri.parse(uri))
                    .into(imagePreview)
            }

        btnFilter.setOnClickListener {

            filterRecyclerView.visibility =
                View.VISIBLE

            filterRecyclerView.adapter =
                FilterAdapter(filters) {

                    // Filter functionality later
                }
        }
    }
}