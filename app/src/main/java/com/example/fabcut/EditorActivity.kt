package com.example.fabcut

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
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
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.effect.BitmapOverlay
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
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@UnstableApi
class EditorActivity : AppCompatActivity() {

    private var activeTextView: TextView? = null
    private var isBoldActive = false
    private var isItalicActive = false
    private var isUnderlineActive = false

    private var selectedVideoFilter = "Original"
    private lateinit var imgCropCut: ImageView
    private lateinit var txtCropCut: TextView

    private lateinit var imagePreview: ImageView
    private lateinit var videoPreview: PlayerView
    private lateinit var textOverlayContainer: FrameLayout
    private lateinit var bottomToolbar: LinearLayout
    private lateinit var player: ExoPlayer

    private lateinit var btnBack: ImageView
    private lateinit var btnSave: TextView
    private lateinit var btnCropDone: TextView

    private lateinit var deleteLayout: LinearLayout
    private lateinit var colorScroll: View
    private lateinit var colorContainer: LinearLayout

    private lateinit var filterRecyclerView: RecyclerView
    private lateinit var cropPanel: LinearLayout
    private lateinit var tiltSeekBar: SeekBar
    private lateinit var txtAngleVal: TextView

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
    private lateinit var thumbRecycler: RecyclerView

    private lateinit var leftHandle: ImageView
    private lateinit var rightHandle: ImageView
    private lateinit var leftShade: View
    private lateinit var rightShade: View

    // Ratio chips
    private lateinit var chipFree: Chip
    private lateinit var chip11: Chip
    private lateinit var chip43: Chip
    private lateinit var chip34: Chip
    private lateinit var chip169: Chip
    private lateinit var chip916: Chip

    private val thumbnailList = ArrayList<Bitmap>()
    private var videoDuration = 0L
    private var trimStart = 0L
    private var trimEnd = 0L

    private var originalBitmap: Bitmap? = null
    private lateinit var mediaUri: String

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
            } else {
                player.play()
            }
        }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "FabCut_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FabCut")
            }
        }

        val resolver = contentResolver
        try {
            val imageUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                fos = resolver.openOutputStream(imageUri)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos!!)
                fos.flush()
                Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        } finally {
            fos?.close()
        }
    }

    private fun captureCompositeBitmap(): Bitmap? {
        val container = findViewById<FrameLayout>(R.id.previewContainer) ?: return null
        if (container.width <= 0 || container.height <= 0) return null

        val bitmap = Bitmap.createBitmap(container.width, container.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        container.draw(canvas)
        textOverlayContainer.draw(canvas)
        return bitmap
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

    private fun getMatrixForFilter(filterName: String): FloatArray? {
        return when (filterName) {
            "Bright" -> floatArrayOf(
                1.25f, 0f, 0f, 0f,
                0f, 1.25f, 0f, 0f,
                0f, 0f, 1.25f, 0f,
                0f, 0f, 0f, 1f
            )
            "Cool" -> floatArrayOf(
                0.85f, 0f, 0f, 0f,
                0f, 1.0f, 0f, 0f,
                0f, 0f, 1.30f, 0f,
                0f, 0f, 0f, 1f
            )
            "Warm" -> floatArrayOf(
                1.25f, 0f, 0f, 0f,
                0f, 1.10f, 0f, 0f,
                0f, 0f, 0.80f, 0f,
                0f, 0f, 0f, 1f
            )
            "Vintage" -> floatArrayOf(
                0.90f, 0.10f, 0.10f, 0f,
                0.10f, 0.80f, 0.10f, 0f,
                0.10f, 0.10f, 0.60f, 0f,
                0f, 0f, 0f, 1f
            )
            else -> null
        }
    }

    @UnstableApi
    private fun previewSelectedFilter() {
        try {
            val matrix = getMatrixForFilter(selectedVideoFilter)
            if (matrix != null) {
                player.setVideoEffects(listOf(androidx.media3.effect.RgbMatrix { _, _ -> matrix }))
            } else if (selectedVideoFilter == "B&W") {
                player.setVideoEffects(listOf(RgbFilter.createGrayscaleFilter()))
            } else {
                player.setVideoEffects(emptyList())
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

            textCursorDrawable = null

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
            val textVal = editTextInput.text.toString().trim()
            if (textVal.isNotEmpty()) {
                if (targetTextView != null) {
                    targetTextView.text = textVal
                } else {
                    val textView = TextView(this@EditorActivity).apply {
                        text = textVal
                        setTextColor(Color.WHITE)
                        textSize = 28f
                        setPadding(20, 20, 20, 20)
                        gravity = Gravity.CENTER
                        alpha = 1.0f
                    }

                    val oParams = RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        addRule(RelativeLayout.CENTER_IN_PARENT)
                    }

                    textView.layoutParams = oParams
                    setupTransformTouchListener(textView, isText = true)
                    textOverlayContainer.addView(textView)

                    this@EditorActivity.activeTextView = textView
                    textToolbar.visibility = View.VISIBLE
                }
            } else if (targetTextView != null) {
                textOverlayContainer.removeView(targetTextView)
                if (this@EditorActivity.activeTextView == targetTextView) {
                    this@EditorActivity.activeTextView = null
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
            alpha = 1.0f
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

    private fun addNewStickerOverlay(emojiText: String) {
        val stickerView = TextView(this).apply {
            text = emojiText
            textSize = 64f
            elevation = 110f
            setPadding(12, 12, 12, 12)
            alpha = 1.0f
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
        var startDist = 0f
        var startAngle = 0f
        var baseScaleX = 1f
        var baseScaleY = 1f
        var baseRotation = 0f

        view.setOnTouchListener { v, event ->
            v.alpha = 1.0f

            if (isText && v is TextView) {
                activeTextView = v
                textToolbar.visibility = View.VISIBLE

                val currentTypeface = v.typeface
                isBoldActive = currentTypeface != null && currentTypeface.isBold
                isItalicActive = currentTypeface != null && currentTypeface.isItalic
                isUnderlineActive = (v.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG) != 0
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    deleteLayout.visibility = View.VISIBLE
                    deleteLayout.animate().alpha(1f).setDuration(150).start()
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        val dx = event.getX(0) - event.getX(1)
                        val dy = event.getY(0) - event.getY(1)
                        startDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        startAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        baseScaleX = v.scaleX
                        baseScaleY = v.scaleY
                        baseRotation = v.rotation
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount == 1) {
                        v.x = event.rawX + dX
                        v.y = event.rawY + dY
                    } else if (event.pointerCount == 2 && startDist > 10f) {
                        val dx = event.getX(0) - event.getX(1)
                        val dy = event.getY(0) - event.getY(1)
                        val newDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                        val newAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                        val factor = newDist / startDist
                        val clampedScale = (baseScaleX * factor).coerceIn(0.4f, 5.0f)
                        v.scaleX = clampedScale
                        v.scaleY = clampedScale

                        val angleDiff = newAngle - startAngle
                        v.rotation = baseRotation + angleDiff
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
                        .setDuration(150)
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
        recycler.layoutManager = GridLayoutManager(this, 5)

        val emojis = listOf(
            "😂", "❤️", "🔥", "👍", "😎", "😍", "🎉", "✨", "💯", "🙌",
            "💥", "🥳", "👑", "🚀", "⭐️", "💪", "🤩", "😈", "🤡", "⚡️",
            "👀", "💖", "🍿", "🏆", "🌟", "🎯", "📸", "🎁", "🎨", "🎭"
        )

        recycler.adapter = StickerAdapter(emojis) { selectedEmoji ->
            val stickerView = TextView(this@EditorActivity).apply {
                text = selectedEmoji
                textSize = 48f
                gravity = Gravity.CENTER
                setPadding(20, 20, 20, 20)
                alpha = 1.0f
            }

            val sParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }

            stickerView.layoutParams = sParams
            setupTransformTouchListener(stickerView, isText = false)
            textOverlayContainer.addView(stickerView)

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
                    this@EditorActivity.activeTextView?.setTextColor(color)
                }
            }
            colorContainer.addView(colorCard)
        }
    }

    private fun setupNativeCropPanel() {
        val cropGridView = findViewById<com.example.fabcut.CropGridView>(R.id.cropGridView)
        val ratioChips = listOf(chipFree, chip11, chip43, chip34, chip169, chip916)

        fun selectChip(selectedChip: Chip) {
            ratioChips.forEach { chip ->
                val isTarget = (chip == selectedChip)
                chip.isSelected = isTarget
                chip.isActivated = isTarget
                chip.isChecked = isTarget
            }
        }

        tiltSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val angle = progress - 45
                txtAngleVal.text = "$angle°"

                val targetView: View =
                    if (videoPreview.visibility == View.VISIBLE) videoPreview else imagePreview
                targetView.rotation = angle.toFloat()

                val rad = Math.toRadians(abs(angle).toDouble())
                val zoomFactor = (cos(rad) + sin(rad)).toFloat()
                targetView.scaleX = zoomFactor
                targetView.scaleY = zoomFactor
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        txtAngleVal.setOnClickListener {
            tiltSeekBar.progress = 45
            val targetView: View =
                if (videoPreview.visibility == View.VISIBLE) videoPreview else imagePreview
            targetView.rotation = 0f
            targetView.scaleX = 1f
            targetView.scaleY = 1f
            txtAngleVal.text = "0°"
        }

        chipFree.setOnClickListener {
            selectChip(chipFree)
            cropGridView.setFreeRatio()
            cropGridView.fitToImage(imagePreview)
        }
        chip11.setOnClickListener {
            selectChip(chip11)
            cropGridView.setCropRatio(1.0f)
            cropGridView.fitToImage(imagePreview)
        }
        chip43.setOnClickListener {
            selectChip(chip43)
            cropGridView.setCropRatio(4.0f / 3.0f)
            cropGridView.fitToImage(imagePreview)
        }
        chip34.setOnClickListener {
            selectChip(chip34)
            cropGridView.setCropRatio(3.0f / 4.0f)
            cropGridView.fitToImage(imagePreview)
        }
        chip169.setOnClickListener {
            selectChip(chip169)
            cropGridView.setCropRatio(16.0f / 9.0f)
            cropGridView.fitToImage(imagePreview)
        }
        chip916.setOnClickListener {
            selectChip(chip916)
            cropGridView.setCropRatio(9.0f / 16.0f)
            cropGridView.fitToImage(imagePreview)
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

        btnBack = findViewById(R.id.btnBack)
        btnSave = findViewById(R.id.btnSave)
        btnCropDone = findViewById(R.id.btnCropDone)

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
        cropPanel = findViewById(R.id.cropPanel)
        tiltSeekBar = findViewById(R.id.tiltSeekBar)
        txtAngleVal = findViewById(R.id.txtAngleVal)

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
        thumbRecycler = findViewById(R.id.thumbRecycler)

        leftHandle = findViewById(R.id.leftHandle)
        rightHandle = findViewById(R.id.rightHandle)
        leftShade = findViewById(R.id.leftShade)
        rightShade = findViewById(R.id.rightShade)

        chipFree = findViewById(R.id.chipFree)
        chip11 = findViewById(R.id.chip11)
        chip43 = findViewById(R.id.chip43)
        chip34 = findViewById(R.id.chip34)
        chip169 = findViewById(R.id.chip169)
        chip916 = findViewById(R.id.chip916)

        val lilacColor = Color.parseColor("#D0BCFF")
        tiltSeekBar.progressTintList = android.content.res.ColorStateList.valueOf(lilacColor)
        tiltSeekBar.thumbTintList = android.content.res.ColorStateList.valueOf(lilacColor)

        btnSave.background?.colorFilter = PorterDuffColorFilter(lilacColor, PorterDuff.Mode.SRC_IN)

        btnBack.setOnClickListener { finish() }

        btnCropDone.setOnClickListener {
            val isVideoMode = intent.getBooleanExtra("IS_VIDEO", false)
            val grid = findViewById<com.example.fabcut.CropGridView>(R.id.cropGridView)

            if (!isVideoMode) {
                val croppedResultBitmap = grid.getCroppedBitmap(imagePreview)
                if (croppedResultBitmap != null) {
                    imagePreview.setImageBitmap(croppedResultBitmap)
                    originalBitmap = croppedResultBitmap
                }
            }

            cropPanel.visibility = View.GONE
            btnCropDone.visibility = View.GONE
            grid.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
        }

        btnSave.setOnClickListener {
            val isVideoMode = intent.getBooleanExtra("IS_VIDEO", false)
            val sharedPrefs = getSharedPreferences("FabCut_Prefs", android.content.Context.MODE_PRIVATE)

            if (isVideoMode) {
                if (mediaUri.isNotEmpty() && trimEnd > trimStart) {
                    val inputUri = Uri.parse(mediaUri)

                    // Standard temporary directory staging path
                    val tempOutputFile = java.io.File(cacheDir, "FabCut_Export_${System.currentTimeMillis()}.mp4")

                    val mediaItem = MediaItem.Builder()
                        .setUri(inputUri)
                        .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(trimStart)
                            .setEndPositionMs(trimEnd)
                            .build())
                        .build()

                    val effectsList = ArrayList<androidx.media3.common.Effect>()

                    val filterMatrix = getMatrixForFilter(selectedVideoFilter)
                    if (filterMatrix != null) {
                        effectsList.add(androidx.media3.effect.RgbMatrix { _, _ -> filterMatrix })
                    } else if (selectedVideoFilter == "B&W") {
                        effectsList.add(RgbFilter.createGrayscaleFilter())
                    }

                    if (textOverlayContainer.childCount > 0) {
                        val containerWidth = if (textOverlayContainer.width > 0) textOverlayContainer.width else 720
                        val containerHeight = if (textOverlayContainer.height > 0) textOverlayContainer.height else 1280

                        val overlayBitmap = Bitmap.createBitmap(
                            containerWidth,
                            containerHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(overlayBitmap)
                        textOverlayContainer.draw(canvas)

                        val textureOverlay = BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
                        effectsList.add(OverlayEffect(listOf(textureOverlay)))
                    }

                    // Default builder configuration handles fallbacks explicitly across standard Android devices
                    val transformer = androidx.media3.transformer.Transformer.Builder(this).build()

                    // Safe fallback setup: removes complex timing constraints that fail on custom architectures
                    val editedMediaItem = androidx.media3.transformer.EditedMediaItem.Builder(mediaItem)
                        .setEffects(androidx.media3.transformer.Effects(emptyList(), effectsList))
                        .build()

                    android.widget.Toast.makeText(this, "Saving video... please wait.", android.widget.Toast.LENGTH_LONG).show()

                    transformer.addListener(object : androidx.media3.transformer.Transformer.Listener {
                        override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult) {
                            val filename = "FabCut_${System.currentTimeMillis()}.mp4"
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Movies/FabCut")
                                }
                            }

                            try {
                                val videoUri = contentResolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                                if (videoUri != null) {
                                    contentResolver.openOutputStream(videoUri)?.use { outputStream ->
                                        tempOutputFile.inputStream().use { inputStream ->
                                            inputStream.copyTo(outputStream)
                                        }
                                    }

                                    val history = sharedPrefs.getStringSet("recent_projects", emptySet()) ?: emptySet()
                                    val updatedHistory = HashSet<String>(history)
                                    updatedHistory.add(videoUri.toString())
                                    sharedPrefs.edit().putStringSet("recent_projects", updatedHistory).apply()
                                }

                                if (tempOutputFile.exists()) tempOutputFile.delete()

                                runOnUiThread {
                                    android.widget.Toast.makeText(this@EditorActivity, "Video saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                runOnUiThread {
                                    android.widget.Toast.makeText(this@EditorActivity, "Gallery Save Failed.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        override fun onError(composition: androidx.media3.transformer.Composition, exportResult: androidx.media3.transformer.ExportResult, exportException: androidx.media3.transformer.ExportException) {
                            if (tempOutputFile.exists()) tempOutputFile.delete()
                            runOnUiThread {
                                // Direct diagnostic message mapping to reveal the underlying media issue
                                android.widget.Toast.makeText(this@EditorActivity, "Error: ${exportException.localizedMessage ?: "Codec Failure"}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    })

                    try {
                        transformer.start(editedMediaItem, tempOutputFile.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (tempOutputFile.exists()) tempOutputFile.delete()
                        android.widget.Toast.makeText(this, "Failed starting export engine.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val bitmap = captureCompositeBitmap()
                if (bitmap != null) {
                    val filename = "FabCut_${System.currentTimeMillis()}.jpg"
                    var fos: java.io.OutputStream? = null

                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/FabCut")
                        }
                    }

                    try {
                        val imageUri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (imageUri != null) {
                            fos = contentResolver.openOutputStream(imageUri)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos!!)
                            fos.flush()

                            val targetPath = imageUri.toString()
                            val history = sharedPrefs.getStringSet("recent_projects", emptySet()) ?: emptySet()
                            val updatedHistory = HashSet<String>(history)
                            updatedHistory.add(targetPath)
                            sharedPrefs.edit().putStringSet("recent_projects", updatedHistory).apply()

                            android.widget.Toast.makeText(this, "Image saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(this, "Failed to save image", android.widget.Toast.LENGTH_SHORT).show()
                    } finally {
                        fos?.close()
                    }
                }
            }
        }

        thumbRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        thumbRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val offset = recyclerView.computeHorizontalScrollOffset()
                val range = recyclerView.computeHorizontalScrollRange()
                val extent = recyclerView.computeHorizontalScrollExtent()

                val maxScrollable = range - extent
                if (maxScrollable > 0 && videoDuration > 0) {
                    val progress = offset.toFloat() / maxScrollable.toFloat()
                    val position = (progress * videoDuration).toLong()
                    player.seekTo(position)
                }
            }
        })

        player = ExoPlayer.Builder(this).build()
        videoPreview.player = player
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.setVideoEffects(emptyList())

        filterRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        filterRecyclerView.visibility = View.GONE

        setupColorSpectrumBar()
        setupNativeCropPanel()

        var startX = 0f
        leftHandle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX = event.rawX; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val newLeft = (view.x + dx).coerceIn(0f, rightHandle.x - 100f)
                    view.x = newLeft
                    startX = event.rawX
                    leftShade.layoutParams = leftShade.layoutParams.apply { width = newLeft.toInt() }
                    leftShade.requestLayout()
                    val tWidth = trimContainer.width.toFloat()
                    if (tWidth > 0 && videoDuration > 0) {
                        trimStart = ((newLeft / tWidth) * videoDuration).toLong()
                        player.seekTo(trimStart)
                    }
                    true
                }
                else -> false
            }
        }
        rightHandle.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX = event.rawX; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val maxRight = trimContainer.width.toFloat() - view.width
                    val newLeft = (view.x + dx).coerceIn(leftHandle.x + 100f, maxRight)
                    view.x = newLeft
                    startX = event.rawX
                    rightShade.layoutParams = rightShade.layoutParams.apply { width = (trimContainer.width - (newLeft + view.width)).toInt() }
                    rightShade.requestLayout()
                    val tWidth = trimContainer.width.toFloat()
                    if (tWidth > 0 && videoDuration > 0) {
                        trimEnd = (((newLeft + view.width) / tWidth) * videoDuration).toLong()
                        player.seekTo(trimEnd)
                    }
                    true
                }
                else -> false
            }
        }

        mediaUri = intent.getStringExtra("MEDIA_URI") ?: ""

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
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        originalBitmap = resource
                        imagePreview.setImageBitmap(resource)

                        val grid = findViewById<com.example.fabcut.CropGridView>(R.id.cropGridView)
                        grid.post { grid.fitToImage(imagePreview) }

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
                                val baseBitmap = originalBitmap ?: resource
                                val resultBitmap = when (filter.name) {
                                    "Original" -> baseBitmap
                                    "Bright" -> ImageFilters.bright(baseBitmap)
                                    "Cool" -> ImageFilters.cool(baseBitmap)
                                    "Warm" -> ImageFilters.warm(baseBitmap)
                                    "Vintage" -> ImageFilters.vintage(baseBitmap)
                                    "B&W" -> ImageFilters.blackAndWhite(baseBitmap)
                                    else -> baseBitmap
                                }
                                withContext(Dispatchers.Main) { imagePreview.setImageBitmap(resultBitmap) }
                            }
                        }
                    }
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
        }

        btnCrop.setOnClickListener {
            filterRecyclerView.visibility = View.GONE
            textToolbar.visibility = View.GONE
            colorScroll.visibility = View.GONE

            val grid = findViewById<com.example.fabcut.CropGridView>(R.id.cropGridView)
            val ratioScrollContainer = chipFree.parent.parent as View

            if (isVideo) {
                val trimIntent = Intent(this, VideoTrimActivity::class.java).apply {
                    putExtra("VIDEO_URI", mediaUri)
                }
                trimLauncher.launch(trimIntent)
            } else {
                if (cropPanel.visibility == View.VISIBLE) {
                    cropPanel.visibility = View.GONE
                    btnCropDone.visibility = View.GONE
                    grid.visibility = View.GONE
                    btnSave.visibility = View.VISIBLE
                } else {
                    cropPanel.visibility = View.VISIBLE
                    btnCropDone.visibility = View.VISIBLE
                    btnSave.visibility = View.GONE

                    trimContainer.visibility = View.GONE
                    grid.visibility = View.VISIBLE
                    tiltSeekBar.parent?.let { (it as View).visibility = View.VISIBLE }
                    ratioScrollContainer.visibility = View.VISIBLE
                    grid.post { grid.fitToImage(imagePreview) }
                }
            }
        }

        btnText.setOnClickListener {
            cropPanel.visibility = View.GONE
            findViewById<com.example.fabcut.CropGridView>(R.id.cropGridView).visibility = View.GONE
            filterRecyclerView.visibility = View.GONE
            btnCropDone.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
            showGooglePhotosTextDialog()
        }

        btnSticker.setOnClickListener {
            cropPanel.visibility = View.GONE
            findViewById<com.example.fabcut.CropGridView>(R.id.cropGridView).visibility = View.GONE
            filterRecyclerView.visibility = View.GONE
            textToolbar.visibility = View.GONE
            colorScroll.visibility = View.GONE
            btnCropDone.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
            showStickerPicker()
        }

        btnFilter.setOnClickListener {
            cropPanel.visibility = View.GONE
            findViewById<com.example.fabcut.CropGridView>(R.id.cropGridView).visibility = View.GONE
            textToolbar.visibility = View.GONE
            colorScroll.visibility = View.GONE
            btnCropDone.visibility = View.GONE
            btnSave.visibility = View.VISIBLE
            filterRecyclerView.visibility = if (filterRecyclerView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btnColor.setOnClickListener {
            colorScroll.visibility = if (colorScroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btnBold.setOnClickListener {
            this@EditorActivity.activeTextView?.let { tv ->
                this@EditorActivity.isBoldActive = !this@EditorActivity.isBoldActive
                val style = if (this@EditorActivity.isBoldActive && this@EditorActivity.isItalicActive) android.graphics.Typeface.BOLD_ITALIC
                else if (this@EditorActivity.isBoldActive) android.graphics.Typeface.BOLD
                else if (this@EditorActivity.isItalicActive) android.graphics.Typeface.ITALIC
                else android.graphics.Typeface.NORMAL
                tv.setTypeface(null, style)
            }
        }

        btnItalic.setOnClickListener {
            this@EditorActivity.activeTextView?.let { tv ->
                this@EditorActivity.isItalicActive = !this@EditorActivity.isItalicActive
                val style = if (this@EditorActivity.isBoldActive && this@EditorActivity.isItalicActive) android.graphics.Typeface.BOLD_ITALIC
                else if (this@EditorActivity.isBoldActive) android.graphics.Typeface.BOLD
                else if (this@EditorActivity.isItalicActive) android.graphics.Typeface.ITALIC
                else android.graphics.Typeface.NORMAL
                tv.setTypeface(null, style)
            }
        }

        btnUnderline.setOnClickListener {
            this@EditorActivity.activeTextView?.let { tv ->
                this@EditorActivity.isUnderlineActive = !this@EditorActivity.isUnderlineActive
                if (this@EditorActivity.isUnderlineActive) {
                    tv.paintFlags = tv.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                } else {
                    tv.paintFlags = tv.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                }
            }
        }
    }
}