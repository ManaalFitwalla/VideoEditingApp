package com.example.fabcut.stickers

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.abs

class StickerCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    val stickers = mutableListOf<StickerEntity>()
    private var selectedSticker: StickerEntity? = null

    private var lastX = 0f
    private var lastY = 0f

    private var scaleDetector =
        ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    selectedSticker?.let {
                        it.scale *= detector.scaleFactor
                        it.scale = it.scale.coerceIn(0.2f, 5f)
                        invalidate()
                    }
                    return true
                }
            })

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (sticker in stickers) {

            canvas.save()

            canvas.translate(sticker.x, sticker.y)

            canvas.rotate(sticker.rotation)

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

            if (sticker.selected) {

                val paint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                }

                canvas.drawRect(
                    -sticker.bitmap.width / 2f,
                    -sticker.bitmap.height / 2f,
                    sticker.bitmap.width / 2f,
                    sticker.bitmap.height / 2f,
                    paint
                )
            }

            canvas.restore()
        }
    }
    fun addSticker(resId: Int) {

        val bitmap = BitmapFactory.decodeResource(resources, resId)

        val size = (120 * resources.displayMetrics.density).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            size,
            size,
            true
        )

        stickers.forEach { it.selected = false }

        stickers.add(
            StickerEntity(
                bitmap = scaledBitmap,
                x = width / 2f,
                y = height / 2f,
                scale = 1f,
                rotation = 0f,
                flipped = false,
                selected = true
            )
        )

        invalidate()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {

        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                selectedSticker = stickers.lastOrNull()

                selectedSticker?.let {
                    it.selected = true
                }

                lastX = event.x
                lastY = event.y

                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {

                selectedSticker?.let {

                    val dx = event.x - lastX
                    val dy = event.y - lastY

                    it.x += dx
                    it.y += dy

                    lastX = event.x
                    lastY = event.y

                    invalidate()
                }
            }
        }

        return true
    }
}