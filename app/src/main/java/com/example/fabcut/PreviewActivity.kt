package com.example.fabcut

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_preview)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.previewContainer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val imagePreview =
            findViewById<ImageView>(R.id.imagePreview)

        val videoPreview =
            findViewById<VideoView>(R.id.videoPreview)

        val uri =
            Uri.parse(intent.getStringExtra("MEDIA_URI"))

        val isVideo =
            intent.getBooleanExtra("IS_VIDEO", false)

        if (isVideo) {

            imagePreview.visibility = View.GONE

            videoPreview.visibility = View.VISIBLE

            videoPreview.setVideoURI(uri)

            videoPreview.setOnPreparedListener { mp ->

                mp.isLooping = true

                videoPreview.scaleX = 1.2f
                videoPreview.scaleY = 1.2f

                videoPreview.start()
            }

        } else {

            videoPreview.visibility = View.GONE

            imagePreview.visibility = View.VISIBLE

            imagePreview.setImageURI(uri)
        }
    }
}