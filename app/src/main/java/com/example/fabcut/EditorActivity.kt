package com.example.fabcut

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.card.MaterialCardView
import android.content.Intent
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import com.yalantis.ucrop.UCrop
import android.widget.VideoView

class EditorActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var videoPreview: VideoView
    private lateinit var txtOverlay: TextView

    private lateinit var deleteLayout: LinearLayout
    private lateinit var colorScroll: HorizontalScrollView
    private lateinit var colorContainer: LinearLayout

    private lateinit var filterRecyclerView: RecyclerView

    private lateinit var btnFilter: MaterialCardView
    private lateinit var btnText: MaterialCardView
    private lateinit var btnSticker: MaterialCardView
    private lateinit var btnCrop: MaterialCardView
    private lateinit var btnAdjust: MaterialCardView
    private lateinit var textToolbar: LinearLayout

    private lateinit var btnColor: ImageView
    private lateinit var btnBold: ImageView
    private lateinit var btnItalic: ImageView
    private lateinit var btnUnderline: ImageView

    private var originalBitmap: Bitmap? = null

    private var dX = 0f
    private var dY = 0f
    private var lastClickTime = 0L

    private lateinit var scaleDetector: ScaleGestureDetector
    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {

                val resultUri = UCrop.getOutput(result.data!!)

                if (resultUri != null) {
                    imagePreview.setImageURI(resultUri)
                }
            }
        }

    private val colors = arrayOf(
        Color.WHITE,
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.CYAN,
        Color.MAGENTA,
        Color.parseColor("#FF9800"),
        Color.parseColor("#9C27B0")
    )

    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        imagePreview = findViewById(R.id.imagePreview)
        videoPreview = findViewById(R.id.videoPreview)
        txtOverlay = findViewById(R.id.txtOverlay)

        deleteLayout = findViewById(R.id.deleteLayout)
        colorScroll = findViewById(R.id.colorScroll)
        colorContainer = findViewById(R.id.colorContainer)

        filterRecyclerView = findViewById(R.id.filterRecyclerView)

        btnFilter = findViewById(R.id.btnFilter)
        btnText = findViewById(R.id.btnText)
        btnSticker = findViewById(R.id.btnSticker)
        btnCrop = findViewById(R.id.btnCrop)
        btnAdjust = findViewById(R.id.btnAdjust)
        textToolbar = findViewById(R.id.textToolbar)

        btnColor = findViewById(R.id.btnColor)
        btnBold = findViewById(R.id.btnBold)
        btnItalic = findViewById(R.id.btnItalic)
        btnUnderline = findViewById(R.id.btnUnderline)

        filterRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        filterRecyclerView.visibility = View.GONE
        scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

                override fun onScale(detector: ScaleGestureDetector): Boolean {

                    var size =
                        txtOverlay.textSize /
                                resources.displayMetrics.scaledDensity

                    size *= detector.scaleFactor
                    size = size.coerceIn(18f, 100f)

                    txtOverlay.textSize = size
                    return true
                }
            }
        )

        val mediaUri = intent.getStringExtra("MEDIA_URI")

        val isVideo = intent.getBooleanExtra("IS_VIDEO", false)

        if (mediaUri != null) {

            if (isVideo) {

                imagePreview.visibility = View.GONE
                videoPreview.visibility = View.VISIBLE

                videoPreview.setVideoURI(Uri.parse(mediaUri))

                videoPreview.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    videoPreview.start()
                }

            } else {

                videoPreview.visibility = View.GONE
                imagePreview.visibility = View.VISIBLE

                Glide.with(this)
                    .asBitmap()
                    .load(Uri.parse(mediaUri))
                    .into(object : CustomTarget<Bitmap>() {

                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {

                            originalBitmap = resource
                            imagePreview.setImageBitmap(resource)

                            // Keep all your existing filter code here exactly as it is.
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
        btnFilter.setOnClickListener {

            filterRecyclerView.visibility =
                if (filterRecyclerView.visibility == View.VISIBLE)
                    View.GONE
                else
                    View.VISIBLE
        }

        btnText.setOnClickListener {

            filterRecyclerView.visibility = View.GONE

            val editText = EditText(this)
            editText.hint = "Type something..."

            AlertDialog.Builder(this)
                .setTitle("Add Text")
                .setView(editText)
                .setPositiveButton("Add") { _, _ ->

                    val text = editText.text.toString().trim()

                    if (text.isNotEmpty()) {

                        txtOverlay.text = text
                        txtOverlay.visibility = View.VISIBLE

                        textToolbar.visibility = View.VISIBLE

                        txtOverlay.post {

                            txtOverlay.x =
                                imagePreview.x +
                                        imagePreview.width / 2f -
                                        txtOverlay.width / 2f

                            txtOverlay.y =
                                imagePreview.y +
                                        imagePreview.height / 2f -
                                        txtOverlay.height / 2f
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnColor.setOnClickListener {

            colorScroll.visibility =
                if (colorScroll.visibility == View.VISIBLE)
                    View.GONE
                else
                    View.VISIBLE
        }

        colorContainer.removeAllViews()

        for (color in colors) {

            val colorView = View(this)

            val params = LinearLayout.LayoutParams(100,100)
            params.setMargins(16,16,16,16)

            colorView.layoutParams = params
            colorView.setBackgroundColor(color)

            colorView.setOnClickListener {

                txtOverlay.setTextColor(color)

            }

            colorContainer.addView(colorView)
        }

        var isBold = false

        btnBold.setOnClickListener {

            isBold = !isBold

            if (isBold)
                txtOverlay.setTypeface(null, android.graphics.Typeface.BOLD)
            else
                txtOverlay.setTypeface(null, android.graphics.Typeface.NORMAL)

        }
        var isItalic = false

        btnItalic.setOnClickListener {

            isItalic = !isItalic

            if (isItalic)
                txtOverlay.setTypeface(null, android.graphics.Typeface.ITALIC)
            else
                txtOverlay.setTypeface(null, android.graphics.Typeface.NORMAL)

        }

        var isUnderline = false

        btnUnderline.setOnClickListener {

            isUnderline = !isUnderline

            if (isUnderline) {

                txtOverlay.paintFlags =
                    txtOverlay.paintFlags or
                            android.graphics.Paint.UNDERLINE_TEXT_FLAG

            } else {

                txtOverlay.paintFlags =
                    txtOverlay.paintFlags and
                            android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()

            }
        }


        btnCrop.setOnClickListener {

            if (mediaUri == null) return@setOnClickListener

            val sourceUri = Uri.parse(mediaUri)
            val destinationUri = Uri.fromFile(File(cacheDir, "cropped.jpg"))

            val options = UCrop.Options().apply {
                setFreeStyleCropEnabled(true)
            }

            val intent = UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .getIntent(this)

            cropLauncher.launch(intent)
        }
        txtOverlay.setOnLongClickListener {

            colorScroll.visibility =
                if (colorScroll.visibility == View.VISIBLE)
                    View.GONE
                else
                    View.VISIBLE

            true
        }

        // ==========================
        // DRAG + PINCH TO RESIZE
        // ==========================

        txtOverlay.setOnTouchListener { view, event ->

            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    dX = view.x - event.rawX
                    dY = view.y - event.rawY

                    view.performClick()

                    deleteLayout.visibility = View.VISIBLE

                    deleteLayout.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }

                MotionEvent.ACTION_MOVE -> {

                    view.x = event.rawX + dX
                    view.y = event.rawY + dY

                    val centerX = view.x + view.width / 2
                    val centerY = view.y + view.height / 2

                    val binX = deleteLayout.x + deleteLayout.width / 2
                    val binY = deleteLayout.y + deleteLayout.height / 2

                    val distance = sqrt(
                        (centerX - binX).pow(2) +
                                (centerY - binY).pow(2)
                    )

                    if (distance < 180f) {

                        deleteLayout.scaleX = 1.3f
                        deleteLayout.scaleY = 1.3f

                    } else {

                        deleteLayout.scaleX = 1f
                        deleteLayout.scaleY = 1f
                    }
                }
                MotionEvent.ACTION_UP -> {

                    val centerX = view.x + view.width / 2
                    val centerY = view.y + view.height / 2

                    val binX = deleteLayout.x + deleteLayout.width / 2
                    val binY = deleteLayout.y + deleteLayout.height / 2

                    val distance = sqrt(
                        (centerX - binX).pow(2) +
                                (centerY - binY).pow(2)
                    )

                    if (distance < 180f) {

                        txtOverlay.text = ""
                        txtOverlay.visibility = View.GONE
                    }

                    deleteLayout.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            deleteLayout.visibility = View.GONE
                        }
                        .start()
                }
            }

            true
        }

        // ==========================
        // DOUBLE TAP TO EDIT
        // ==========================

        txtOverlay.setOnClickListener {

            val currentTime = System.currentTimeMillis()

            if (currentTime - lastClickTime < 300) {

                val editText = EditText(this)
                editText.setText(txtOverlay.text)
                editText.setSelection(editText.text.length)

                AlertDialog.Builder(this)
                    .setTitle("Edit Text")
                    .setView(editText)
                    .setPositiveButton("Save") { _, _ ->

                        val newText = editText.text.toString().trim()

                        if (newText.isNotEmpty()) {
                            txtOverlay.text = newText
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            lastClickTime = currentTime
        }

        btnSticker.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
        }

        btnAdjust.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
        }
        // ==========================
        // DEFAULT TEXT STYLE
        // ==========================

        txtOverlay.apply {
            textSize = 34f
            setTextColor(Color.WHITE)
            setShadowLayer(
                10f,
                3f,
                3f,
                Color.BLACK
            )
            visibility = View.GONE
        }

    } // End of onCreate()
} // End of EditorActivity