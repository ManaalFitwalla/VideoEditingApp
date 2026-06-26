package com.example.fabcut

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.view.animation.OvershootInterpolator
import android.view.animation.AccelerateDecelerateInterpolator

class SplashActivity : AppCompatActivity() {

    private lateinit var txtF: TextView
    private lateinit var txtA: TextView
    private lateinit var txtB: TextView
    private lateinit var txtC: TextView
    private lateinit var txtU: TextView
    private lateinit var txtT: TextView

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
        val root = findViewById<android.view.View>(android.R.id.content)

        root.alpha = 0f

        root.animate()
            .alpha(1f)
            .setDuration(600)
            .start()

        txtF = findViewById(R.id.txtF)
        txtA = findViewById(R.id.txtA)
        txtB = findViewById(R.id.txtB)
        txtC = findViewById(R.id.txtC)
        txtU = findViewById(R.id.txtU)
        txtT = findViewById(R.id.txtT)
        startAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            checkPermission()
        }, 2000)
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

    private fun startAnimation() {

        val duration = 1800L

        val animF = ObjectAnimator.ofFloat(txtF, "translationX", -1000f, 0f)
        val animA = ObjectAnimator.ofFloat(txtA, "translationY", -900f, 0f)
        val animB = ObjectAnimator.ofFloat(txtB, "translationY", 900f, 0f)
        val animC = ObjectAnimator.ofFloat(txtC, "translationX", 1000f, 0f)
        val animU = ObjectAnimator.ofFloat(txtU, "translationX", -900f, 0f)
        val animT = ObjectAnimator.ofFloat(txtT, "translationX", 900f, 0f)

        val fade1 = ObjectAnimator.ofFloat(txtF, "alpha", 0f, 1f)
        val fade2 = ObjectAnimator.ofFloat(txtA, "alpha", 0f, 1f)
        val fade3 = ObjectAnimator.ofFloat(txtB, "alpha", 0f, 1f)
        val fade4 = ObjectAnimator.ofFloat(txtC, "alpha", 0f, 1f)
        val fade5 = ObjectAnimator.ofFloat(txtU, "alpha", 0f, 1f)
        val fade6 = ObjectAnimator.ofFloat(txtT, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(
                animF, animA, animB, animC, animU, animT,
                fade1, fade2, fade3, fade4, fade5, fade6
            )
            this.duration = duration
            interpolator = OvershootInterpolator(1.4f)
            start()
        }
    }
    private fun openHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}