package com.example.fabcut

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val imagesGranted =
                permissions[Manifest.permission.READ_MEDIA_IMAGES] == true

            val videosGranted =
                permissions[Manifest.permission.READ_MEDIA_VIDEO] == true

            if (imagesGranted && videosGranted) {
                openHomeActivity()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        checkPermission()
    }

    private fun checkPermission() {

        val imagesPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_IMAGES
        )

        val videosPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_VIDEO
        )

        if (imagesPermission == PackageManager.PERMISSION_GRANTED &&
            videosPermission == PackageManager.PERMISSION_GRANTED
        ) {
            openHomeActivity()
        } else {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        }
    }

    private fun openHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}