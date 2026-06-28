package com.example.fabcut

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {

    private lateinit var btnVideo: MaterialCardView
    private lateinit var btnPhoto: MaterialCardView
    private lateinit var btnCollage: MaterialCardView // Added for Collage

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
        btnCollage = findViewById(R.id.collageCard) // Bind your new Collage layout view here

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

        // Added click listener for Collage option
        btnCollage.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            intent.putExtra("MEDIA_TYPE", "COLLAGE") // Tells Gallery this is a collage
            startActivity(intent)
        }
    }
}