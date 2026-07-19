package com.example.fabcut

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {

    private lateinit var btnVideo: MaterialCardView
    private lateinit var btnPhoto: MaterialCardView
    private lateinit var btnCollage: MaterialCardView
    private lateinit var btnRecentHeader: LinearLayout

    private lateinit var recentProjectsContainer: LinearLayout
    private lateinit var txtNoProjects: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnVideo = findViewById(R.id.videoCard)
        btnPhoto = findViewById(R.id.photoCard)
        btnCollage = findViewById(R.id.collageCard)
        btnRecentHeader = findViewById(R.id.recentHeader)

        recentProjectsContainer = findViewById(R.id.recentProjectsContainer)
        txtNoProjects = findViewById(R.id.txtNoProjects)

        btnVideo.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("MEDIA_TYPE", "VIDEO")
            startActivity(intent)
        }

        btnPhoto.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("MEDIA_TYPE", "PHOTO")
            startActivity(intent)
        }

        btnCollage.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("MEDIA_TYPE", "COLLAGE")
            startActivity(intent)
        }

        btnRecentHeader.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java).apply {
                putExtra("MEDIA_TYPE", "SAVED_PROJECTS")
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecentProjects()
    }

    private fun loadRecentProjects() {
        val sharedPrefs = getSharedPreferences("FabCut_Prefs", Context.MODE_PRIVATE)
        val savedPaths = sharedPrefs.getStringSet("recent_projects", emptySet()) ?: emptySet()

        recentProjectsContainer.removeAllViews()

        if (savedPaths.isEmpty()) {
            txtNoProjects.visibility = View.VISIBLE
            recentProjectsContainer.visibility = View.GONE
        } else {
            txtNoProjects.visibility = View.GONE
            recentProjectsContainer.visibility = View.VISIBLE

            val pathList = savedPaths.toList().reversed().take(3)

            for (path in pathList) {
                val cardView = MaterialCardView(this).apply {
                    setRadius(dpToPx(16).toFloat())
                    cardElevation = dpToPx(2).toFloat()
                    useCompatPadding = false
                    preventCornerOverlap = true

                    val cardParams = LinearLayout.LayoutParams(dpToPx(110), dpToPx(110)).apply {
                        setMargins(0, 0, dpToPx(12), 0)
                    }
                    layoutParams = cardParams

                    val thumbnailImageView = ImageView(this@HomeActivity).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }

                    val contentUri = Uri.parse(path)
                    Glide.with(this@HomeActivity)
                        .load(if (path.startsWith("content://")) contentUri else java.io.File(path))
                        .transform(CenterCrop(), RoundedCorners(dpToPx(16)))
                        .into(thumbnailImageView)

                    addView(thumbnailImageView)

                    setOnClickListener {
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            val uriData = if (path.startsWith("content://")) contentUri else Uri.fromFile(java.io.File(path))
                            setDataAndType(uriData, if (path.contains(".mp4")) "video/*" else "image/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        try {
                            startActivity(viewIntent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(this@HomeActivity, "Cannot open item file", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                recentProjectsContainer.addView(cardView)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}