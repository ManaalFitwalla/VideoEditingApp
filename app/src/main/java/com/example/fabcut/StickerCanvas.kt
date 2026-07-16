package com.example.fabcut.stickers

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.example.fabcut.R
import kotlin.math.atan2
import androidx.core.content.ContextCompat

class StickerCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val undoStack = mutableListOf<List<StickerEntity>>()
    private val redoStack = mutableListOf<List<StickerEntity>>()

    val stickers = mutableListOf<StickerEntity>()

    private var selectedSticker: StickerEntity? = null

    private var lastX = 0f
    private var lastY = 0f

    private var previousAngle = 0f
    private var rotating = false

    private val controlSize = 90f

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A90E2")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private fun icon(drawableId: Int): Bitmap {

        val drawable = ContextCompat.getDrawable(context, drawableId)
            ?: throw IllegalArgumentException("Drawable not found")

        val size = controlSize.toInt()

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        return bitmap
    }
    private val deleteIcon = icon(R.drawable.ic_delete_stiicker)
    private val flipIcon = icon(R.drawable.ic_flip)
    private val duplicateIcon = icon(R.drawable.ic_duplicate)



    private val scaleDetector =
        ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    selectedSticker?.let {
                        it.scale *= detector.scaleFactor
                        it.scale = it.scale.coerceIn(0.25f, 5f)
                        invalidate()
                    }
                    return true
                }
            })


    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)

        stickers.forEach { sticker ->

            canvas.save()

            // Move to sticker position
            canvas.translate(sticker.x, sticker.y)

            // Rotate everything together
            canvas.rotate(sticker.rotation)

            // -------------------------
            // Draw ONLY the sticker
            // -------------------------
            canvas.save()

            canvas.scale(
                if (sticker.flipped) -sticker.scale else sticker.scale,
                sticker.scale
            )

            canvas.drawBitmap(
                sticker.bitmap,
                -sticker.bitmap.width / 2f,
                -sticker.bitmap.height / 2f,
                null
            )

            canvas.restore()

            // -------------------------
            // Draw border & controls
            // -------------------------
            if (sticker.selected) {

                val left = -sticker.bitmap.width / 2f
                val top = -sticker.bitmap.height / 2f
                val right = sticker.bitmap.width / 2f
                val bottom = sticker.bitmap.height / 2f

                // Border
                canvas.drawRoundRect(
                    RectF(left, top, right, bottom),
                    20f,
                    20f,
                    borderPaint
                )

                // Flip icon (Top Left)
                canvas.drawBitmap(
                    flipIcon,
                    left - controlSize / 2f,
                    top - controlSize / 2f,
                    null
                )

                // Delete icon (Top Right)
                canvas.drawBitmap(
                    deleteIcon,
                    right - controlSize / 2f,
                    top - controlSize / 2f,
                    null
                )

                // Duplicate icon (Bottom Center)
                canvas.drawBitmap(
                    duplicateIcon,
                    -controlSize / 2f,
                    bottom - controlSize / 2f,
                    null
                )
            }

            canvas.restore()
        }
    }
    fun addSticker(resId: Int) {

        val bitmap = BitmapFactory.decodeResource(resources, resId) ?: return

        val size = (120 * resources.displayMetrics.density).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            size,
            size,
            true
        )

        // Deselect all previous stickers
        stickers.forEach {
            it.selected = false
        }

        val centerX = if (width > 0) width / 2f else resources.displayMetrics.widthPixels / 2f
        val centerY = if (height > 0) height / 2f else resources.displayMetrics.heightPixels / 2f

        val sticker = StickerEntity(
            bitmap = scaledBitmap,
            x = centerX,
            y = centerY,
            scale = 1f,
            rotation = 0f,
            flipped = false,
            selected = true
        )

        stickers.add(sticker)
        selectedSticker = sticker

        invalidate()
    }

    private fun calculateAngle(event: MotionEvent): Float {


        if (event.pointerCount < 2) return 0f

        val dx = event.getX(1) - event.getX(0)
        val dy = event.getY(1) - event.getY(0)

        return Math.toDegrees(
            atan2(dy.toDouble(), dx.toDouble())
        ).toFloat()
    }

    private fun hitSticker(x: Float, y: Float): StickerEntity? {


        for (i in stickers.indices.reversed()) {

            val sticker = stickers[i]

            val halfW = sticker.bitmap.width * sticker.scale / 2f
            val halfH = sticker.bitmap.height * sticker.scale / 2f

            if (
                x >= sticker.x - halfW &&
                x <= sticker.x + halfW &&
                y >= sticker.y - halfH &&
                y <= sticker.y + halfH
            ) {
                return sticker
            }
        }

        return null
    }

    private fun bringToFront(sticker: StickerEntity) {


        stickers.remove(sticker)
        stickers.add(sticker)
    }

    private fun deleteSticker(sticker: StickerEntity) {
        saveState()

        stickers.remove(sticker)

        if (selectedSticker == sticker)
            selectedSticker = null

        invalidate()
    }

    private fun flipSticker(sticker: StickerEntity) {
        saveState()

        sticker.flipped = !sticker.flipped

        invalidate()
    }

    private fun duplicateSticker(sticker: StickerEntity) {
        saveState()

        stickers.forEach {
            it.selected = false
        }

        val copy = sticker.copy(
            bitmap = sticker.bitmap,
            x = sticker.x + 60f,
            y = sticker.y + 60f,
            selected = true
        )

        stickers.add(copy)
        selectedSticker = copy

        invalidate()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {

        saveState()

        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                // First check control buttons of selected sticker
                selectedSticker?.let { sticker ->

                    val halfW = sticker.bitmap.width * sticker.scale / 2f
                    val halfH = sticker.bitmap.height * sticker.scale / 2f

                    val touchPadding = 30f

                    val deleteRect = RectF(
                        sticker.x + halfW - controlSize / 2f - touchPadding,
                        sticker.y - halfH - controlSize / 2f - touchPadding,
                        sticker.x + halfW + controlSize / 2f + touchPadding,
                        sticker.y - halfH + controlSize / 2f + touchPadding
                    )

                    val flipRect = RectF(
                        sticker.x - halfW - controlSize / 2f - touchPadding,
                        sticker.y - halfH - controlSize / 2f - touchPadding,
                        sticker.x - halfW + controlSize / 2f + touchPadding,
                        sticker.y - halfH + controlSize / 2f + touchPadding
                    )

                    val duplicateRect = RectF(
                        sticker.x - controlSize / 2,
                        sticker.y + halfH,
                        sticker.x + controlSize / 2,
                        sticker.y + halfH + controlSize
                    )


                    if (deleteRect.contains(event.x, event.y)) {
                        deleteSticker(sticker)
                        return true
                    }

                    if (flipRect.contains(event.x, event.y)) {
                        flipSticker(sticker)
                        return true
                    }

                    if (duplicateRect.contains(event.x, event.y)) {
                        duplicateSticker(sticker)
                        return true
                    }
                }

                // Select sticker
                val sticker = hitSticker(event.x, event.y)

                stickers.forEach {
                    it.selected = false
                }

                if (sticker != null) {

                    selectedSticker = sticker
                    sticker.selected = true
                    bringToFront(sticker)

                    lastX = event.x
                    lastY = event.y

                } else {

                    selectedSticker = null
                }

                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {

                selectedSticker?.let {

                    if (rotating && event.pointerCount >= 2) {

                        val angle = calculateAngle(event)
                        val delta = angle - previousAngle

                        it.rotation += delta
                        previousAngle = angle

                    } else {

                        val dx = event.x - lastX
                        val dy = event.y - lastY

                        it.x += dx
                        it.y += dy

                        lastX = event.x
                        lastY = event.y
                    }

                    invalidate()
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {

                if (event.pointerCount == 2) {
                    previousAngle = calculateAngle(event)
                    rotating = true
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                rotating = false
            }

            MotionEvent.ACTION_UP -> {
                performClick()
            }
        }

        return true
    }

    private fun saveState() {

        val copy = stickers.map {
            it.copy(
                bitmap = it.bitmap
            )
        }

        undoStack.add(copy)
        redoStack.clear()

        if (undoStack.size > 30) {
            undoStack.removeAt(0)
        }
    }
    fun undo() {

        if (undoStack.isEmpty()) return

        val current = stickers.map {
            it.copy(bitmap = it.bitmap)
        }

        redoStack.add(current)

        val previous = undoStack.removeAt(undoStack.lastIndex)

        stickers.clear()
        stickers.addAll(previous.map {
            it.copy(bitmap = it.bitmap)
        })

        selectedSticker = stickers.lastOrNull {
            it.selected
        }

        invalidate()
    }
    fun redo() {

        if (redoStack.isEmpty()) return

        val current = stickers.map {
            it.copy(bitmap = it.bitmap)
        }

        undoStack.add(current)

        val next = redoStack.removeAt(redoStack.lastIndex)

        stickers.clear()
        stickers.addAll(next.map {
            it.copy(bitmap = it.bitmap)
        })

        selectedSticker = stickers.lastOrNull {
            it.selected
        }

        invalidate()
    }
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}