        package com.example.fabcut
        
        import android.app.AlertDialog
        import android.content.Intent
        import android.graphics.Bitmap
        import android.graphics.Color
        import android.graphics.drawable.Drawable
        import android.media.MediaMetadataRetriever
        import android.net.Uri
        import android.os.Bundle
        import android.view.MotionEvent
        import android.view.ScaleGestureDetector
        import android.view.View
        import android.widget.EditText
        import android.widget.FrameLayout
        import android.widget.HorizontalScrollView
        import android.widget.ImageView
        import android.widget.LinearLayout
        import android.widget.SeekBar
        import android.widget.TextView
        import android.widget.Toast
        import androidx.activity.enableEdgeToEdge
        import androidx.activity.result.contract.ActivityResultContracts
        import androidx.appcompat.app.AppCompatActivity
        import androidx.core.view.ViewCompat
        import androidx.core.view.WindowInsetsCompat
        import androidx.media3.common.MediaItem
        import androidx.media3.common.Player
        import androidx.media3.common.util.UnstableApi
        import androidx.media3.effect.RgbFilter
        import androidx.media3.exoplayer.ExoPlayer
        import androidx.media3.ui.PlayerView
        import androidx.recyclerview.widget.LinearLayoutManager
        import androidx.recyclerview.widget.RecyclerView
        import com.bumptech.glide.Glide
        import com.bumptech.glide.request.target.CustomTarget
        import com.bumptech.glide.request.transition.Transition
        import com.google.android.material.card.MaterialCardView
        import com.yalantis.ucrop.UCrop
        import java.io.File
        import kotlin.math.pow
        import kotlin.math.sqrt
        import com.example.fabcut.stickers.StickerCanvas
        import android.graphics.BitmapFactory

        class EditorActivity : AppCompatActivity() {


            private val categories = listOf(

                CategoryItem(
                    "Smile",
                    R.drawable.ic_smile,
                    listOf(
                        StickerItem(R.drawable.smile_1),
                        StickerItem(R.drawable.smile_2),
                        StickerItem(R.drawable.smile_3),
                        StickerItem(R.drawable.laugh),
                        StickerItem(R.drawable.wink),
                        StickerItem(R.drawable.cool),
                        StickerItem(R.drawable.cry_l),
                        StickerItem(R.drawable.blush),
                        StickerItem(R.drawable.sleepy),
                        StickerItem(R.drawable.sur)
                    )
                ),

                CategoryItem(
                    "Love",
                    R.drawable.ic_love,
                    listOf(
                        StickerItem(R.drawable.love_1),
                        StickerItem(R.drawable.love_2),
                        StickerItem(R.drawable.pink_h),
                        StickerItem(R.drawable.blue_h),
                        StickerItem(R.drawable.broken_h),
                        StickerItem(R.drawable.doub)
                    )
                )

            )

            private var isStickerPanelOpen = false
            private var selectedCategory: CategoryItem? = null
            private lateinit var categoryRecyclerView: RecyclerView
            private lateinit var stickerCanvas: StickerCanvas
            private lateinit var stickerRecyclerView: RecyclerView


            private lateinit var btnUndo: ImageView
            private lateinit var btnRedo: ImageView
        
            private lateinit var btnSave: MaterialCardView
        
            private var selectedVideoFilter = "Original"
            private lateinit var imgCropCut: ImageView
            private lateinit var txtCropCut: TextView
        
            private lateinit var brightnessSeekBar: SeekBar
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
                    } else {
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
            private fun applyVideoFilter(effect: androidx.media3.common.Effect) {
                player.setVideoEffects(listOf(effect))
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
        
            // FIXED: Real Video Filters for Media3
            @UnstableApi
            private fun previewSelectedFilter() {
                try {
                    when (selectedVideoFilter) {
                        "Original" -> {
                            player.setVideoEffects(emptyList())
                        }
        
                        "Bright" -> {
                            // 4x4 Matrix (16 floats) - Scale RGB up by 25%
                            val brightMatrix = floatArrayOf(
                                1.25f, 0f, 0f, 0f,
                                0f, 1.25f, 0f, 0f,
                                0f, 0f, 1.25f, 0f,
                                0f, 0f, 0f, 1f
                            )
                            player.setVideoEffects(listOf(androidx.media3.effect.RgbMatrix { _, _ -> brightMatrix }))
                        }
        
                        "Cool" -> {
                            // Boost Blue (1.3), slightly lower Red (0.85)
                            val coolMatrix = floatArrayOf(
                                0.85f, 0f, 0f, 0f,
                                0f, 1.0f, 0f, 0f,
                                0f, 0f, 1.30f, 0f,
                                0f, 0f, 0f, 1f
                            )
                            player.setVideoEffects(listOf(androidx.media3.effect.RgbMatrix { _, _ -> coolMatrix }))
                        }
        
                        "Warm" -> {
                            // Boost Red (1.25) & Green (1.1), reduce Blue (0.8)
                            val warmMatrix = floatArrayOf(
                                1.25f, 0f, 0f, 0f,
                                0f, 1.10f, 0f, 0f,
                                0f, 0f, 0.80f, 0f,
                                0f, 0f, 0f, 1f
                            )
                            player.setVideoEffects(listOf(androidx.media3.effect.RgbMatrix { _, _ -> warmMatrix }))
                        }
        
                        "Vintage" -> {
                            // Sepia / Vintage RGB mix matrix
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
        
            @UnstableApi
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()
                setContentView(R.layout.activity_editor)


                stickerCanvas = findViewById(R.id.stickerCanvas)
    
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

                categoryRecyclerView = findViewById(R.id.categoryRecyclerView)
                stickerRecyclerView = findViewById(R.id.stickerRecyclerView)

                stickerRecyclerView.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

                categoryRecyclerView.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                stickerCanvas = findViewById(R.id.stickerCanvas)

                categoryRecyclerView.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

                stickerRecyclerView.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    
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
                btnUndo = findViewById(R.id.btnUndo)
                btnRedo = findViewById(R.id.btnRedo)
        
                brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        
                videoPreview.findViewById<View>(androidx.media3.ui.R.id.exo_progress)?.visibility = View.GONE
        
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
                    }
                })
        
                btnItalic = findViewById(R.id.btnItalic)
                btnUnderline = findViewById(R.id.btnUnderline)
        
                // Initialize ExoPlayer
                player = ExoPlayer.Builder(this).build()
                videoPreview.player = player
        
                // FIXED: Set player repeat mode so video loops continuously!
                player.repeatMode = Player.REPEAT_MODE_ALL
        
                player.setVideoEffects(emptyList())
        
                videoPreview.setShowFastForwardButton(false)
                videoPreview.setShowRewindButton(false)
                videoPreview.setShowNextButton(false)
                videoPreview.setShowPreviousButton(false)
                videoPreview.setShowShuffleButton(false)
                videoPreview.setShowSubtitleButton(false)
                videoPreview.setShowVrButton(false)
        
                videoPreview.controllerShowTimeoutMs = 1500
                videoPreview.controllerAutoShow = true
        
                filterRecyclerView.layoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    
        
                stickerRecyclerView.visibility = View.GONE
        
                filterRecyclerView.visibility = View.GONE
                scaleDetector = ScaleGestureDetector(
                    this,
                    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        override fun onScale(detector: ScaleGestureDetector): Boolean {
                            var size = txtOverlay.textSize / resources.displayMetrics.scaledDensity
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
        
                                // FIXED: Applies real distinct filter functions for images!
                                filterRecyclerView.adapter = FilterAdapter(filters) { filter ->
                                    selectedVideoFilter = filter.name
                                    when (filter.name) {
                                        "Original" -> imagePreview.setImageBitmap(resource)
                                        "Bright" -> imagePreview.setImageBitmap(ImageFilters.bright(resource))
                                        "Cool" -> imagePreview.setImageBitmap(ImageFilters.cool(resource))
                                        "Warm" -> imagePreview.setImageBitmap(ImageFilters.warm(resource))
                                        "Vintage" -> imagePreview.setImageBitmap(ImageFilters.vintage(resource))
                                        "B&W" -> imagePreview.setImageBitmap(ImageFilters.blackAndWhite(resource))
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
                                        if (videoPreview.visibility == View.VISIBLE) videoPreview else imagePreview
        
                                    txtOverlay.x = targetView.x + targetView.width / 2f - txtOverlay.width / 2f
                                    txtOverlay.y = targetView.y + targetView.height / 2f - txtOverlay.height / 2f
                                }
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
        
                btnColor.setOnClickListener {
                    colorScroll.visibility =
                        if (colorScroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
        
                colorContainer.removeAllViews()
                for (color in colors) {
                    val colorView = View(this)
                    val params = LinearLayout.LayoutParams(100, 100)
                    params.setMargins(16, 16, 16, 16)
                    colorView.layoutParams = params
                    colorView.setBackgroundColor(color)
                    colorView.setOnClickListener { txtOverlay.setTextColor(color) }
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
                            txtOverlay.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                    } else {
                        txtOverlay.paintFlags =
                            txtOverlay.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
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
                        if (colorScroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    true
                }

                btnUndo.setOnClickListener {
                    stickerCanvas.undo()
                }

                btnRedo.setOnClickListener {
                    stickerCanvas.redo()
                }
                txtOverlay.setOnTouchListener { view, event ->
                    scaleDetector.onTouchEvent(event)
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            dX = view.x - event.rawX
                            dY = view.y - event.rawY
                            view.performClick()
                            deleteLayout.visibility = View.VISIBLE
                            deleteLayout.animate().alpha(1f).setDuration(200).start()
                        }
                        MotionEvent.ACTION_MOVE -> {
                            view.x = event.rawX + dX
                            view.y = event.rawY + dY
        
                            val centerX = view.x + view.width / 2
                            val centerY = view.y + view.height / 2
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
                        MotionEvent.ACTION_UP -> {
                            val centerX = view.x + view.width / 2
                            val centerY = view.y + view.height / 2
                            val binX = deleteLayout.x + deleteLayout.width / 2
                            val binY = deleteLayout.y + deleteLayout.height / 2
        
                            val distance = sqrt((centerX - binX).pow(2) + (centerY - binY).pow(2))
                            if (distance < 180f) {
                                txtOverlay.text = ""
                                txtOverlay.visibility = View.GONE
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

                    if (!isStickerPanelOpen) {

                        categoryRecyclerView.visibility = View.VISIBLE
                        stickerRecyclerView.visibility = View.GONE

                        isStickerPanelOpen = true

                    } else {

                        categoryRecyclerView.visibility = View.GONE
                        stickerRecyclerView.visibility = View.GONE

                        selectedCategory = null
                        isStickerPanelOpen = false

                    }
                    categoryRecyclerView.adapter = CategoryAdapter(categories) { category ->

                        if (selectedCategory == category &&
                            stickerRecyclerView.visibility == View.VISIBLE) {

                            stickerRecyclerView.visibility = View.GONE
                            selectedCategory = null

                        } else {

                            selectedCategory = category
                            stickerRecyclerView.visibility = View.VISIBLE

                            stickerRecyclerView.adapter =
                                StickerAdapter(category.stickers) { sticker ->
                                    stickerCanvas.addSticker(sticker.sticker)
                                }
                        }
                    }
                }
                btnAdjust.setOnClickListener {
                    filterRecyclerView.visibility = View.GONE
                    brightnessSeekBar.visibility =
                        if (brightnessSeekBar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
        
                txtOverlay.apply {
                    textSize = 34f
                    setTextColor(Color.WHITE)
                    setShadowLayer(10f, 3f, 3f, Color.BLACK)
                    visibility = View.GONE
                }
            }



            private fun addSticker(resId: Int) {
                stickerCanvas.addSticker(resId)
            }
    
            override fun onDestroy() {
                super.onDestroy()
                player.release()
            }
        }