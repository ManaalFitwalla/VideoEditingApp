package com.example.fabcut

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        val btnVideo = findViewById<Button>(R.id.btnVideo)
        val btnPhoto = findViewById<Button>(R.id.btnPhoto)

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
    }
}