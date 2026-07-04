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
import kotlin.math.pow
import kotlin.math.sqrt

class EditorActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
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

    private var originalBitmap: Bitmap? = null

    private var dX = 0f
    private var dY = 0f
    private var lastClickTime = 0L

    private lateinit var scaleDetector: ScaleGestureDetector

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

        filterRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        filterRecyclerView.visibility = View.GONE
        scaleDetector = ScaleGestureDetector(
            this,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

                override fun onScale(detector: ScaleGestureDetector): Boolean {

                    var size =
                        txtOverlay.textSize / resources.displayMetrics.scaledDensity

                    size *= detector.scaleFactor
                    size = size.coerceIn(18f, 100f)

                    txtOverlay.textSize = size
                    return true
                }
            }
        )

        val mediaUri = intent.getStringExtra("MEDIA_URI")

        if (mediaUri != null) {

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

                        val thumb = Bitmap.createScaledBitmap(
                            resource,
                            150,
                            150,
                            true
                        )

                        val filters = listOf(
                            FilterItem("Original", thumb),
                            FilterItem("Bright", ImageFilters.bright(thumb)),
                            FilterItem("Cool", ImageFilters.cool(thumb)),
                            FilterItem("Warm", ImageFilters.warm(thumb)),
                            FilterItem("Vintage", ImageFilters.vintage(thumb)),
                            FilterItem("B&W", ImageFilters.blackAndWhite(thumb))
                        )

                        filterRecyclerView.adapter =
                            FilterAdapter(filters) { filter ->

                                when (filter.name) {

                                    "Original" ->
                                        imagePreview.setImageBitmap(resource)

                                    "Bright" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.bright(resource)
                                        )

                                    "Cool" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.cool(resource)
                                        )

                                    "Warm" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.warm(resource)
                                        )

                                    "Vintage" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.vintage(resource)
                                        )

                                    "B&W" ->
                                        imagePreview.setImageBitmap(
                                            ImageFilters.blackAndWhite(resource)
                                        )
                                }
                            }
                    }

                    override fun onLoadCleared(
                        placeholder: Drawable?
                    ) {
                    }
                })
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
        // ==========================
        // FILTER BUTTON
        // ==========================

        btnFilter.setOnClickListener {

            filterRecyclerView.visibility =
                if (filterRecyclerView.visibility == View.VISIBLE)
                    View.GONE
                else
                    View.VISIBLE
        }

        // ==========================
        // ADD TEXT
        // ==========================

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

        // ==========================
        // DRAG + PINCH TO RESIZE
        // ==========================

        txtOverlay.setOnTouchListener { view, event ->

            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {

                    dX = view.x - event.rawX
                    dY = view.y - event.rawY

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
                        deleteLayout.alpha = 1f

                    } else {

                        deleteLayout.scaleX = 1f
                        deleteLayout.scaleY = 1f
                        deleteLayout.alpha = 0.8f
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

                        txtOverlay.visibility = View.GONE
                        txtOverlay.text = ""
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

        // ==========================
        // COLOR PALETTE
        // ==========================

        colorContainer.removeAllViews()

        for (color in colors) {

            val colorView = View(this)

            val params = LinearLayout.LayoutParams(100, 100)
            params.setMargins(16, 16, 16, 16)

            colorView.layoutParams = params
            colorView.setBackgroundColor(color)

            colorView.setOnClickListener {
                txtOverlay.setTextColor(color)
                colorScroll.visibility = View.GONE
            }

            colorContainer.addView(colorView)
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
        // HIDE FILTER BAR
        // ==========================

        btnSticker.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
        }

        btnCrop.setOnClickListener {
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
    }
}