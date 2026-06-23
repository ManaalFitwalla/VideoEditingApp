package com.example.fabcut

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var btnVideo: LinearLayout
    private lateinit var btnPhoto: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        btnVideo = findViewById(R.id.btnVideo)
        btnPhoto = findViewById(R.id.btnPhoto)

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