package com.example.fabcut

import android.widget.Toast
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Player
import android.content.Intent
import android.media.MediaMetadataRetriever
import androidx.media3.effect.RgbFilter
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
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import com.yalantis.ucrop.UCrop
import androidx.media3.ui.PlayerView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import android.widget.FrameLayout
class EditorActivity : AppCompatActivity() {

    private lateinit var imgCropCut: ImageView
    private lateinit var txtCropCut: TextView

    private lateinit var imagePreview: ImageView
    private lateinit var videoPreview: PlayerView
    private lateinit var bottomToolbar: HorizontalScrollView
    private lateinit var player: ExoPlayer
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
    private lateinit var trimContainer: FrameLayout
    private lateinit var timeText: TextView
    private lateinit var thumbRecycler: RecyclerView

    private lateinit var leftHandle: ImageView
    private lateinit var rightHandle: ImageView

    private lateinit var leftShade: View
    private lateinit var rightShade: View

    private val thumbnailList = ArrayList<Bitmap>()

    private var videoDuration = 0L
    private var trimStart = 0L
    private var trimEnd = 0L

    private var startX = 0f
    private var endX = 0f

    private var originalBitmap: Bitmap? = null

    private lateinit var mediaUri: String

    private var dX = 0f
    private var dY = 0f
    private var lastClickTime = 0L
    private val trimLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {
                trimStart = result.data?.getLongExtra("TRIM_START", 0L) ?: 0L
                trimEnd = result.data?.getLongExtra("TRIM_END", player.duration) ?: player.duration

                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(mediaUri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(trimStart)
                            .setEndPositionMs(trimEnd)
                            .build()
                    )
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                timeText.text = "${trimStart / 1000}s - ${trimEnd / 1000}s"


            }
            else {

                player.play()

            }
        }

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
    @UnstableApi
    private fun applyVideoFilter(filter: RgbFilter) {

        player.setVideoEffects(
            listOf(filter)
        )

        player.seekTo(player.currentPosition)
    }
    private fun getVideoThumbnail(videoUri: Uri): Bitmap? {

        return try {

            val retriever = MediaMetadataRetriever()

            retriever.setDataSource(this, videoUri)

            val bitmap = retriever.getFrameAtTime(
                0,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )

            retriever.release()

            bitmap

        } catch (e: Exception) {

            e.printStackTrace()
            null
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
    private fun generateThumbnails(videoUri: Uri) {

        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(this, videoUri)

        val duration =
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L

        thumbnailList.clear()

        val frameCount = 10

        for (i in 0 until frameCount) {

            val timeUs =
                (duration * 1000 / frameCount) * i

            val bitmap =
                retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )

            bitmap?.let {
                thumbnailList.add(it)
            }
        }

        retriever.release()

        thumbRecycler.adapter = ThumbnailAdapter(thumbnailList)
    }
    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editor)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        imgCropCut = findViewById(R.id.imgCropCut)

        txtCropCut = findViewById(R.id.txtCropCut)

        imagePreview = findViewById(R.id.imagePreview)
        videoPreview = findViewById(R.id.videoPreview)
        txtOverlay = findViewById(R.id.txtOverlay)

        deleteLayout = findViewById(R.id.deleteLayout)
        colorScroll = findViewById(R.id.colorScroll)
        colorContainer = findViewById(R.id.colorContainer)
        bottomToolbar = findViewById(R.id.bottomToolbar)

        filterRecyclerView = findViewById(R.id.filterRecyclerView)

        btnFilter = findViewById(R.id.btnFilter)
        btnText = findViewById(R.id.btnText)
        btnSticker = findViewById(R.id.btnSticker)
        btnCrop = findViewById(R.id.btnCrop)
        btnAdjust = findViewById(R.id.btnAdjust)
        textToolbar = findViewById(R.id.textToolbar)

        btnColor = findViewById(R.id.btnColor)
        btnBold = findViewById(R.id.btnBold)
        trimContainer = findViewById(R.id.trimContainer)
        timeText = findViewById(R.id.timeText)
        thumbRecycler = findViewById(R.id.thumbRecycler)

        leftHandle = findViewById(R.id.leftHandle)
        rightHandle = findViewById(R.id.rightHandle)

        leftShade = findViewById(R.id.leftShade)
        rightShade = findViewById(R.id.rightShade)

        thumbRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        thumbRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {

                val offset = recyclerView.computeHorizontalScrollOffset()
                val range = recyclerView.computeHorizontalScrollRange()
                val extent = recyclerView.computeHorizontalScrollExtent()

                val progress =
                    offset.toFloat() / (range - extent).coerceAtLeast(1)

                val position =
                    (progress * videoDuration).toLong()

                player.seekTo(position)
            }
        })
        btnItalic = findViewById(R.id.btnItalic)
        btnUnderline = findViewById(R.id.btnUnderline)
        player = ExoPlayer.Builder(this).build()
        videoPreview.player = player

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

        mediaUri = intent.getStringExtra("MEDIA_URI")!!

        val isVideo = intent.getBooleanExtra("IS_VIDEO", false)
        if (isVideo) {
            txtCropCut.text = "Trim"
            imgCropCut.setImageResource(R.drawable.ic_cut)
        } else {
            txtCropCut.text = "Crop"
            imgCropCut.setImageResource(R.drawable.ic_crop)
        }

        if (isVideo) {
            txtCropCut.text = "Trim"
        } else {
            txtCropCut.text = "Crop"
        }
        run {

            if (isVideo) {

                imagePreview.visibility = View.GONE
                videoPreview.visibility = View.VISIBLE

                val mediaItem = MediaItem.fromUri(Uri.parse(mediaUri))

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                player.addListener(object : Player.Listener {

                    override fun onPlaybackStateChanged(state: Int) {

                        if (state == Player.STATE_READY) {

                            videoDuration = player.duration

                            trimStart = 0
                            trimEnd = videoDuration

                            timeText.text = "0s - ${videoDuration / 1000}s"

                            generateThumbnails(Uri.parse(mediaUri))
                        }
                    }
                })
                val thumb = getVideoThumbnail(Uri.parse(mediaUri))

                if (thumb != null) {

                    val smallThumb = Bitmap.createScaledBitmap(
                        thumb,
                        150,
                        150,
                        true
                    )

                    val filters = listOf(
                        FilterItem("Original", smallThumb),
                        FilterItem("Bright", ImageFilters.bright(smallThumb)),
                        FilterItem("Cool", ImageFilters.cool(smallThumb)),
                        FilterItem("Warm", ImageFilters.warm(smallThumb)),
                        FilterItem("Vintage", ImageFilters.vintage(smallThumb)),
                        FilterItem("B&W", ImageFilters.blackAndWhite(smallThumb))
                    )

                    filterRecyclerView.adapter =
                        FilterAdapter(filters) { filter ->

                            when (filter.name) {

                                "Original" -> {
                                    player.setVideoEffects(emptyList())
                                }

                                "B&W" -> {
                                    Toast.makeText(this, "B&W clicked", Toast.LENGTH_SHORT).show()
                                    applyVideoFilter(RgbFilter.createGrayscaleFilter())
                                }
                            }
                        }
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
                        txtOverlay.bringToFront()

                        textToolbar.visibility = View.VISIBLE

                        txtOverlay.post {

                            val targetView =
                                if (videoPreview.visibility == View.VISIBLE)
                                    videoPreview
                                else
                                    imagePreview

                            txtOverlay.x =
                                targetView.x +
                                        targetView.width / 2f -
                                        txtOverlay.width / 2f

                            txtOverlay.y =
                                targetView.y +
                                        targetView.height / 2f -
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

            if (videoPreview.visibility == View.VISIBLE) {



                val intent = Intent(this, VideoTrimActivity::class.java)
                intent.putExtra("VIDEO_URI", mediaUri)
                trimLauncher.launch(intent)

                return@setOnClickListener
            }

            if (mediaUri.isEmpty()) return@setOnClickListener

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
    override fun onStop() {
        super.onStop()

       // player.release()
    }
    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
} // End of EditorActivity
