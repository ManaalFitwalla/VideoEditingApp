package com.example.fabcut

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbFilter
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class EditorActivity : AppCompatActivity() {

    private var selectedVideoFilter = "Original"
    private lateinit var imgCropCut: ImageView
    private lateinit var txtCropCut: TextView

    private lateinit var imagePreview: ImageView
    private lateinit var videoPreview: PlayerView
    private lateinit var textOverlayContainer: FrameLayout
    private lateinit var bottomToolbar: LinearLayout
    private lateinit var player: ExoPlayer

    private lateinit var deleteLayout: LinearLayout
    private lateinit var colorScroll: View
    private lateinit var colorContainer: LinearLayout

    private lateinit var filterRecyclerView: RecyclerView

    private lateinit var btnFilter: MaterialCardView
    private lateinit var btnText: MaterialCardView
    private lateinit var btnSticker: MaterialCardView
    private lateinit var btnCrop: MaterialCardView
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

    private var originalBitmap: Bitmap? = null
    private lateinit var mediaUri: String

    private var activeTextView: TextView? = null

    // Handler loop to update video playback time smoothly in real-time
    private val timeHandler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            if (::player.isInitialized && player.isPlaying) {
                val currentMs = player.currentPosition
                val totalMs = player.duration.coerceAtLeast(1L)
                timeText.text = "${formatTime(currentMs)} / ${formatTime(totalMs)}"
                timeHandler.postDelayed(this, 100)
            } else if (::player.isInitialized) {
                timeHandler.postDelayed(this, 200)
            }
        }
    }

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

                timeText.text = "${formatTime(trimStart)} / ${formatTime(trimEnd)}"
            } else {
                player.play()
            }
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    imagePreview.setImageURI(resultUri)
                }
            }
        }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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

    @UnstableApi
    private fun previewSelectedFilter() {
        try {
            when (selectedVideoFilter) {
                "Original" -> {
                    player.setVideoEffects(emptyList())
                }

                "Bright" -> {
                    val brightMatrix = floatArrayOf(
                        1.25f, 0f, 0f, 0f,
                        0f, 1.25f, 0f, 0f,
                        0f, 0f, 1.25f, 0f,
                        0f, 0f, 0f, 1f
                    )
                    player.setVideoEffects(listOf(androidx.media3.effect.RgbMatrix { _, _ -> brightMatrix }))
                }

                "Cool" -> {
                    val coolMatrix = floatArrayOf(
                        0.85f, 0f, 0f, 0f,
                        0f, 1.0f, 0f, 0f,
                        0f, 0f, 1.30f, 0f,
                        0f, 0f, 0f, 1f
                    )
                    player.setVideoEffects(listOf(androidx.media3.effect.RgbMatrix { _, _ -> coolMatrix }))
                }

                "Warm" -> {
                    val warmMatrix = floatArrayOf(
                        1.25f, 0f, 0f, 0f,
                        0f, 1.10f, 0f, 0f,
                        0f, 0f, 0.80f, 0f,
                        0f, 0f, 0f, 1f
                    )
                    player.setVideoEffects(listOf(androidx.media3.effect.RgbMatrix { _, _ -> warmMatrix }))
                }

                "Vintage" -> {
                    val vintageMatrix = floatArrayOf(
                        0.90f, 0.10f, 0.10f, 0f,
                        0.10f, 0.80f, 0.10f, 0f,
                        0.10f, 0.10f, 0.60f, 0f,
                        0f, 0f, 0f, 1f
                    )
                    player.setVideoEffects(listOf(androidx.media3.effect.RgbMatrix { _, _ -> vintageMatrix }))
                }

                "B&W" -> {
                    player.setVideoEffects(listOf(RgbFilter.createGrayscaleFilter()))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateThumbnails(videoUri: Uri) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, videoUri)

        val duration = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLong() ?: 0L

        thumbnailList.clear()
        val frameCount = 10

        for (i in 0 until frameCount) {
            val timeUs = (duration * 1000 / frameCount) * i
            val bitmap = retriever.getFrameAtTime(
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

    private fun showGooglePhotosTextDialog(targetTextView: TextView? = null) {
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)

        val rootLayout = RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor("#A6000000"))
            setPadding(48, 48, 48, 48)
        }

        val btnDone = TextView(this).apply {
            text = "Done"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(24, 24, 24, 24)
            val params = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }
            layoutParams = params
        }

        val editTextInput = EditText(this).apply {
            hint = "Type here..."
            setHintTextColor(Color.parseColor("#80FFFFFF"))
            setTextColor(Color.WHITE)
            textSize = 32f
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = null
            gravity = Gravity.CENTER
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

            if (targetTextView != null && targetTextView.text.isNotEmpty()) {
                setText(targetTextView.text)
                setSelection(text.length)
            }

            val params = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            layoutParams = params
        }

        rootLayout.addView(btnDone)
        rootLayout.addView(editTextInput)
        dialog.setContentView(rootLayout)

        btnDone.setOnClickListener {
            val text = editTextInput.text.toString().trim()
            if (text.isNotEmpty()) {
                if (targetTextView != null) {
                    targetTextView.text = text
                } else {
                    addNewTextOverlay(text)
                }
            } else if (targetTextView != null) {
                textOverlayContainer.removeView(targetTextView)
                if (activeTextView == targetTextView) {
                    activeTextView = null
                    textToolbar.visibility = View.GONE
                    colorScroll.visibility = View.GONE
                }
            }
            dialog.dismiss()
        }

        dialog.show()
        editTextInput.requestFocus()
    }

    private fun addNewTextOverlay(text: String) {
        val textView = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 34f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setShadowLayer(10f, 3f, 3f, Color.BLACK)
            setPadding(16, 16, 16, 16)
            elevation = 110f
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        textView.layoutParams = params
        setupTransformTouchListener(textView, isText = true)
        textOverlayContainer.addView(textView)
        textOverlayContainer.bringToFront()

        activeTextView = textView
        textToolbar.visibility = View.VISIBLE
    }

    private fun addNewStickerOverlay(stickerEmoji: String) {
        val stickerView = TextView(this).apply {
            text = stickerEmoji
            textSize = 60f
            elevation = 110f
            setPadding(16, 16, 16, 16)
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        stickerView.layoutParams = params
        setupTransformTouchListener(stickerView, isText = false)
        textOverlayContainer.addView(stickerView)
        textOverlayContainer.bringToFront()
    }

    private fun setupTransformTouchListener(view: View, isText: Boolean) {
        var dX = 0f
        var dY = 0f
        var lastDist = 0f
        var lastAngle = 0f

        view.setOnTouchListener { v, event ->
            if (isText && v is TextView) {
                activeTextView = v
                textToolbar.visibility = View.VISIBLE
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    deleteLayout.visibility = View.VISIBLE
                    deleteLayout.animate().alpha(1f).setDuration(200).start()
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        val x0 = event.getX(0)
                        val y0 = event.getY(0)
                        val x1 = event.getX(1)
                        val y1 = event.getY(1)

                        val dx = x0 - x1
                        val dy = y0 - y1
                        lastDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        lastAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        v.x = event.rawX + dX
                        v.y = event.rawY + dY
                    } else if (event.pointerCount == 2) {
                        val x0 = event.getX(0)
                        val y0 = event.getY(0)
                        val x1 = event.getX(1)
                        val y1 = event.getY(1)

                        val dx = x0 - x1
                        val dy = y0 - y1
                        val newDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        val newAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                        if (lastDist > 0) {
                            val scaleFactor = newDist / lastDist
                            val newScaleX = (v.scaleX * scaleFactor).coerceIn(0.5f, 4.0f)
                            val newScaleY = (v.scaleY * scaleFactor).coerceIn(0.5f, 4.0f)
                            v.scaleX = newScaleX
                            v.scaleY = newScaleY
                        }

                        val angleDelta = newAngle - lastAngle
                        v.rotation = (v.rotation + angleDelta).toFloat()

                        lastDist = newDist
                        lastAngle = newAngle
                    }

                    val centerX = v.x + v.width / 2
                    val centerY = v.y + v.height / 2
                    val binX = deleteLayout.x + deleteLayout.width / 2
                    val binY = deleteLayout.y + deleteLayout.height / 2

                    val distance = sqrt((centerX - binX).pow(2) + (centerY - binY).pow(2))
                    if (distance < 180f) {
                        deleteLayout.scaleX = 1.3f
                        deleteLayout.scaleY = 1.3f
                    } else {
                        deleteLayout.scaleX = 1f
                        deleteLayout.scaleY = 1f
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val centerX = v.x + v.width / 2
                    val centerY = v.y + v.height / 2
                    val binX = deleteLayout.x + deleteLayout.width / 2
                    val binY = deleteLayout.y + deleteLayout.height / 2

                    val distance = sqrt((centerX - binX).pow(2) + (centerY - binY).pow(2))
                    if (distance < 180f) {
                        textOverlayContainer.removeView(v)
                        if (isText && activeTextView == v) {
                            activeTextView = null
                            textToolbar.visibility = View.GONE
                            colorScroll.visibility = View.GONE
                        }
                    }

                    deleteLayout.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction { deleteLayout.visibility = View.GONE }
                        .start()
                }
            }

            true
        }

        if (isText && view is TextView) {
            view.setOnClickListener {
                activeTextView = view
                showGooglePhotosTextDialog(view)
            }
        }
    }

    private fun showStickerPicker() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_sticker_picker, null)
        bottomSheetDialog.setContentView(view)

        val recycler = view.findViewById<RecyclerView>(R.id.stickerRecyclerView)
        recycler.layoutManager = GridLayoutManager(this, 4)

        val stickers = listOf(
            "😂", "❤️", "🔥", "👍", "😎", "😍", "🎉", "✨",
            "💯", "🙌", "💥", "🥳", "👑", "🚀", "⭐️", "💪"
        )

        recycler.adapter = StickerAdapter(stickers) { selectedSticker ->
            addNewStickerOverlay(selectedSticker)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun setupColorSpectrumBar() {
        colorContainer.removeAllViews()

        val spectrumColors = IntArray(36)
        for (i in 0 until 36) {
            spectrumColors[i] = Color.HSVToColor(floatArrayOf(i * 10f, 1f, 1f))
        }

        val allColors = listOf(Color.WHITE, Color.BLACK) + spectrumColors.toList()

        for (color in allColors) {
            val colorCard = MaterialCardView(this).apply {
                val params = LinearLayout.LayoutParams(90, 90).apply {
                    setMargins(12, 12, 12, 12)
                }
                layoutParams = params
                radius = 45f
                setCardBackgroundColor(color)
                strokeColor = Color.parseColor("#44FFFFFF")
                strokeWidth = 3

                setOnClickListener {
                    activeTextView?.setTextColor(color)
                }
            }
            colorContainer.addView(colorCard)
        }
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
        textOverlayContainer = findViewById(R.id.textOverlayContainer)

        textOverlayContainer.elevation = 100f
        textOverlayContainer.bringToFront()

        deleteLayout = findViewById(R.id.deleteLayout)
        colorScroll = findViewById(R.id.colorScroll)
        colorContainer = findViewById(R.id.colorContainer)
        bottomToolbar = findViewById(R.id.bottomToolbar)

        filterRecyclerView = findViewById(R.id.filterRecyclerView)

        btnFilter = findViewById(R.id.btnFilter)
        btnText = findViewById(R.id.btnText)
        btnSticker = findViewById(R.id.btnSticker)
        btnCrop = findViewById(R.id.btnCrop)
        textToolbar = findViewById(R.id.textToolbar)

        btnColor = findViewById(R.id.btnColor)
        btnBold = findViewById(R.id.btnBold)
        btnItalic = findViewById(R.id.btnItalic)
        btnUnderline = findViewById(R.id.btnUnderline)

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
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val offset = recyclerView.computeHorizontalScrollOffset()
                val range = recyclerView.computeHorizontalScrollRange()
                val extent = recyclerView.computeHorizontalScrollExtent()

                val progress = offset.toFloat() / (range - extent).coerceAtLeast(1)
                val position = (progress * videoDuration).toLong()
                player.seekTo(position)
                timeText.text = "${formatTime(position)} / ${formatTime(videoDuration)}"
            }
        })

        player = ExoPlayer.Builder(this).build()
        videoPreview.player = player
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.setVideoEffects(emptyList())

        filterRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        filterRecyclerView.visibility = View.GONE

        setupColorSpectrumBar()

        mediaUri = intent.getStringExtra("MEDIA_URI")!!

        val isVideo = intent.getBooleanExtra("IS_VIDEO", false)
        if (isVideo) {
            txtCropCut.text = "Trim"
            imgCropCut.setImageResource(R.drawable.ic_cut)
            timeText.visibility = View.VISIBLE
        } else {
            txtCropCut.text = "Crop"
            imgCropCut.setImageResource(R.drawable.ic_crop)
            timeText.visibility = View.GONE
        }

        if (isVideo) {
            imagePreview.visibility = View.GONE
            videoPreview.visibility = View.VISIBLE

            val mediaItem = MediaItem.fromUri(Uri.parse(mediaUri))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            timeHandler.post(updateTimeRunnable)

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        videoDuration = player.duration
                        trimStart = 0
                        trimEnd = videoDuration
                        timeText.text = "00:00 / ${formatTime(videoDuration)}"
                        generateThumbnails(Uri.parse(mediaUri))
                    }
                }
            })

            val thumb = getVideoThumbnail(Uri.parse(mediaUri))

            if (thumb != null) {
                val smallThumb = Bitmap.createScaledBitmap(thumb, 150, 150, true)

                val filters = listOf(
                    FilterItem("Original", smallThumb),
                    FilterItem("Bright", ImageFilters.bright(smallThumb)),
                    FilterItem("Cool", ImageFilters.cool(smallThumb)),
                    FilterItem("Warm", ImageFilters.warm(smallThumb)),
                    FilterItem("Vintage", ImageFilters.vintage(smallThumb)),
                    FilterItem("B&W", ImageFilters.blackAndWhite(smallThumb))
                )

                filterRecyclerView.adapter = FilterAdapter(filters) { filter ->
                    selectedVideoFilter = filter.name
                    previewSelectedFilter()
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

                        val thumb = Bitmap.createScaledBitmap(resource, 150, 150, true)

                        val filters = listOf(
                            FilterItem("Original", thumb),
                            FilterItem("Bright", ImageFilters.bright(thumb)),
                            FilterItem("Cool", ImageFilters.cool(thumb)),
                            FilterItem("Warm", ImageFilters.warm(thumb)),
                            FilterItem("Vintage", ImageFilters.vintage(thumb)),
                            FilterItem("B&W", ImageFilters.blackAndWhite(thumb))
                        )

                        filterRecyclerView.adapter = FilterAdapter(filters) { filter ->
                            selectedVideoFilter = filter.name

                            lifecycleScope.launch(Dispatchers.Default) {
                                val resultBitmap = when (filter.name) {
                                    "Original" -> resource
                                    "Bright" -> ImageFilters.bright(resource)
                                    "Cool" -> ImageFilters.cool(resource)
                                    "Warm" -> ImageFilters.warm(resource)
                                    "Vintage" -> ImageFilters.vintage(resource)
                                    "B&W" -> ImageFilters.blackAndWhite(resource)
                                    else -> resource
                                }

                                withContext(Dispatchers.Main) {
                                    imagePreview.setImageBitmap(resultBitmap)
                                }
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }

        btnFilter.setOnClickListener {
            filterRecyclerView.visibility =
                if (filterRecyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btnText.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
            showGooglePhotosTextDialog()
        }

        btnSticker.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
            showStickerPicker()
        }

        btnColor.setOnClickListener {
            colorScroll.visibility =
                if (colorScroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        var isBold = false
        btnBold.setOnClickListener {
            isBold = !isBold
            activeTextView?.setTypeface(null, if (isBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }

        var isItalic = false
        btnItalic.setOnClickListener {
            isItalic = !isItalic
            activeTextView?.setTypeface(null, if (isItalic) android.graphics.Typeface.ITALIC else android.graphics.Typeface.NORMAL)
        }

        var isUnderline = false
        btnUnderline.setOnClickListener {
            activeTextView?.let { tv ->
                isUnderline = !isUnderline
                tv.paintFlags = if (isUnderline) {
                    tv.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                } else {
                    tv.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                }
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
                setToolbarColor(Color.parseColor("#1A1A1A"))
                setToolbarWidgetColor(Color.WHITE)
                setToolbarTitle("Crop Image")
                setActiveControlsWidgetColor(Color.WHITE)
                setDimmedLayerColor(Color.parseColor("#CC000000"))
                setCropFrameColor(Color.WHITE)
                setCropGridColor(Color.parseColor("#55FFFFFF"))
                setLogoColor(Color.TRANSPARENT)
            }

            val intent = UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .getIntent(this)

            cropLauncher.launch(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeHandler.removeCallbacks(updateTimeRunnable)
        player.release()
    }
}