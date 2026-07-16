package com.example.fabcut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView

class CropGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintBorder = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val paintLines = Paint().apply {
        color = Color.parseColor("#B0FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val paintCorner = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var cropRect = RectF()
    private val imageBounds = RectF()
    private var currentRatio = 1.0f
    private var isFreeRatio = true

    // Touch handling variables
    private var touchMode = MOVE_NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val touchTolerance = 60f
    private val cornerHandleLength = 40f
    private val cornerHandleWidth = 10f

    companion object {
        private const val MOVE_NONE = 0
        private const val MOVE_TOP_LEFT = 1
        private const val MOVE_TOP_RIGHT = 2
        private const val MOVE_BOTTOM_LEFT = 3
        private const val MOVE_BOTTOM_RIGHT = 4
        private const val MOVE_CENTER = 5
    }

    fun setCropRatio(ratio: Float) {
        currentRatio = ratio
        isFreeRatio = false
        resetCropRectToRatio()
        invalidate()
    }

    fun setFreeRatio() {
        isFreeRatio = true
        invalidate()
    }

    fun fitToImage(imageView: ImageView) {
        val drawable = imageView.drawable ?: return
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0 || viewHeight <= 0) return

        val scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight)
        val actualImageWidth = imageWidth * scale
        val actualImageHeight = imageHeight * scale

        val leftLimit = (viewWidth - actualImageWidth) / 2
        val topLimit = (viewHeight - actualImageHeight) / 2

        imageBounds.set(leftLimit, topLimit, leftLimit + actualImageWidth, topLimit + actualImageHeight)

        if (isFreeRatio) {
            cropRect.set(imageBounds)
        } else {
            resetCropRectToRatio()
        }
        invalidate()
    }

    private fun resetCropRectToRatio() {
        if (imageBounds.isEmpty) return
        val targetWidth: Float
        val targetHeight: Float

        if (imageBounds.width() / imageBounds.height() > currentRatio) {
            targetHeight = imageBounds.height()
            targetWidth = targetHeight * currentRatio
        } else {
            targetWidth = imageBounds.width()
            targetHeight = targetWidth / currentRatio
        }

        val left = imageBounds.left + (imageBounds.width() - targetWidth) / 2
        val top = imageBounds.top + (imageBounds.height() - targetHeight) / 2
        cropRect.set(left, top, left + targetWidth, top + targetHeight)
    }

    fun getCroppedBitmap(imageView: ImageView): Bitmap? {
        val drawable = imageView.drawable as? BitmapDrawable ?: return null
        val originalBitmap = drawable.bitmap ?: return null

        if (imageBounds.isEmpty || cropRect.isEmpty) return null

        val relativeLeft = (cropRect.left - imageBounds.left) / imageBounds.width()
        val relativeTop = (cropRect.top - imageBounds.top) / imageBounds.height()
        val relativeWidth = cropRect.width() / imageBounds.width()
        val relativeHeight = cropRect.height() / imageBounds.height()

        var cropX = (relativeLeft * originalBitmap.width).toInt().coerceIn(0, originalBitmap.width - 1)
        var cropY = (relativeTop * originalBitmap.height).toInt().coerceIn(0, originalBitmap.height - 1)
        var cropW = (relativeWidth * originalBitmap.width).toInt().coerceAtLeast(1)
        var cropH = (relativeHeight * originalBitmap.height).toInt().coerceAtLeast(1)

        if (cropX + cropW > originalBitmap.width) {
            cropW = originalBitmap.width - cropX
        }
        if (cropY + cropH > originalBitmap.height) {
            cropH = originalBitmap.height - cropY
        }

        return Bitmap.createBitmap(originalBitmap, cropX, cropY, cropW, cropH)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchMode = getTouchMode(x, y)
                lastTouchX = x
                lastTouchY = y
                return touchMode != MOVE_NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchMode != MOVE_NONE) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY
                    updateCropWindow(dx, dy)
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchMode = MOVE_NONE
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getTouchMode(x: Float, y: Float): Int {
        if (Math.abs(x - cropRect.left) < touchTolerance && Math.abs(y - cropRect.top) < touchTolerance) return MOVE_TOP_LEFT
        if (Math.abs(x - cropRect.right) < touchTolerance && Math.abs(y - cropRect.top) < touchTolerance) return MOVE_TOP_RIGHT
        if (Math.abs(x - cropRect.left) < touchTolerance && Math.abs(y - cropRect.bottom) < touchTolerance) return MOVE_BOTTOM_LEFT
        if (Math.abs(x - cropRect.right) < touchTolerance && Math.abs(y - cropRect.bottom) < touchTolerance) return MOVE_BOTTOM_RIGHT
        if (cropRect.contains(x, y)) return MOVE_CENTER
        return MOVE_NONE
    }

    private fun updateCropWindow(dx: Float, dy: Float) {
        val minSize = 100f
        when (touchMode) {
            MOVE_CENTER -> {
                cropRect.offset(dx, dy)
                if (cropRect.left < imageBounds.left) cropRect.offset(imageBounds.left - cropRect.left, 0f)
                if (cropRect.right > imageBounds.right) cropRect.offset(imageBounds.right - cropRect.right, 0f)
                if (cropRect.top < imageBounds.top) cropRect.offset(0f, imageBounds.top - cropRect.top)
                if (cropRect.bottom > imageBounds.bottom) cropRect.offset(0f, imageBounds.bottom - cropRect.bottom)
            }
            MOVE_TOP_LEFT -> {
                if (isFreeRatio) {
                    cropRect.left = (cropRect.left + dx).coerceIn(imageBounds.left, cropRect.right - minSize)
                    cropRect.top = (cropRect.top + dy).coerceIn(imageBounds.top, cropRect.bottom - minSize)
                } else {
                    val newLeft = (cropRect.left + dx).coerceIn(imageBounds.left, cropRect.right - minSize)
                    val newHeight = (cropRect.right - newLeft) / currentRatio
                    val newTop = cropRect.bottom - newHeight
                    if (newTop >= imageBounds.top) {
                        cropRect.left = newLeft
                        cropRect.top = newTop
                    }
                }
            }
            MOVE_TOP_RIGHT -> {
                if (isFreeRatio) {
                    cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, imageBounds.right)
                    cropRect.top = (cropRect.top + dy).coerceIn(imageBounds.top, cropRect.bottom - minSize)
                } else {
                    val newRight = (cropRect.right + dx).coerceIn(cropRect.left + minSize, imageBounds.right)
                    val newHeight = (newRight - cropRect.left) / currentRatio
                    val newTop = cropRect.bottom - newHeight
                    if (newTop >= imageBounds.top) {
                        cropRect.right = newRight
                        cropRect.top = newTop
                    }
                }
            }
            MOVE_BOTTOM_LEFT -> {
                if (isFreeRatio) {
                    cropRect.left = (cropRect.left + dx).coerceIn(imageBounds.left, cropRect.right - minSize)
                    cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, imageBounds.bottom)
                } else {
                    val newLeft = (cropRect.left + dx).coerceIn(imageBounds.left, cropRect.right - minSize)
                    val newHeight = (cropRect.right - newLeft) / currentRatio
                    val newBottom = cropRect.top + newHeight
                    if (newBottom <= imageBounds.bottom) {
                        cropRect.left = newLeft
                        cropRect.bottom = newBottom
                    }
                }
            }
            MOVE_BOTTOM_RIGHT -> {
                if (isFreeRatio) {
                    cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, imageBounds.right)
                    cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, imageBounds.bottom)
                } else {
                    val newRight = (cropRect.right + dx).coerceIn(cropRect.left + minSize, imageBounds.right)
                    val newHeight = (newRight - cropRect.left) / currentRatio
                    val newBottom = cropRect.top + newHeight
                    if (newBottom <= imageBounds.bottom) {
                        cropRect.right = newRight
                        cropRect.bottom = newBottom
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cropRect.isEmpty) return

        canvas.drawRect(cropRect, paintBorder)

        val thirdHeight = cropRect.height() / 3
        canvas.drawLine(cropRect.left, cropRect.top + thirdHeight, cropRect.right, cropRect.top + thirdHeight, paintLines)
        canvas.drawLine(cropRect.left, cropRect.top + 2 * thirdHeight, cropRect.right, cropRect.top + 2 * thirdHeight, paintLines)

        val thirdWidth = cropRect.width() / 3
        canvas.drawLine(cropRect.left + thirdWidth, cropRect.top, cropRect.left + thirdWidth, cropRect.bottom, paintLines)
        canvas.drawLine(cropRect.left + 2 * thirdWidth, cropRect.top, cropRect.left + 2 * thirdWidth, cropRect.bottom, paintLines)

        drawCornerHandles(canvas)
    }

    private fun drawCornerHandles(canvas: Canvas) {
        // Top Left Corner
        canvas.drawRect(cropRect.left - 3f, cropRect.top - 3f, cropRect.left + cornerHandleLength, cropRect.top + cornerHandleWidth, paintCorner)
        canvas.drawRect(cropRect.left - 3f, cropRect.top - 3f, cropRect.left + cornerHandleWidth, cropRect.top + cornerHandleLength, paintCorner)

        // Top Right Corner
        canvas.drawRect(cropRect.right - cornerHandleLength, cropRect.top - 3f, cropRect.right + 3f, cropRect.top + cornerHandleWidth, paintCorner)
        canvas.drawRect(cropRect.right - cornerHandleWidth, cropRect.top - 3f, cropRect.right + 3f, cropRect.top + cornerHandleLength, paintCorner)

        // Bottom Left Corner
        canvas.drawRect(cropRect.left - 3f, cropRect.bottom - cornerHandleWidth, cropRect.left + cornerHandleLength, cropRect.bottom + 3f, paintCorner)
        canvas.drawRect(cropRect.left - 3f, cropRect.bottom - cornerHandleLength, cropRect.left + cornerHandleWidth, cropRect.bottom + 3f, paintCorner)

        // Bottom Right Corner
        canvas.drawRect(cropRect.right - cornerHandleLength, cropRect.bottom - cornerHandleWidth, cropRect.right + 3f, cropRect.bottom + 3f, paintCorner)
        canvas.drawRect(cropRect.right - cornerHandleWidth, cropRect.bottom - cornerHandleLength, cropRect.right + 3f, cropRect.bottom + 3f, paintCorner)
    }
}