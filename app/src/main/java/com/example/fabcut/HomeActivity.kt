package com.example.fabcut

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
class HomeActivity : AppCompatActivity() {

    private lateinit var btnVideo: MaterialCardView
    private lateinit var btnPhoto: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        btnVideo = findViewById(R.id.videoCard)
        btnPhoto = findViewById(R.id.photoCard)

        btnVideo.setOnClickListener {

            val intent = Intent(
                this,
                GalleryActivity::class.java
            )

            intent.putExtra(
                "MEDIA_TYPE",
                "VIDEO"
            )

            startActivity(intent)
        }

        btnPhoto.setOnClickListener {

            val intent = Intent(
                this,
                GalleryActivity::class.java
            )

            intent.putExtra(
                "MEDIA_TYPE",
                "PHOTO"
            )

            startActivity(intent)
        }
    }
}