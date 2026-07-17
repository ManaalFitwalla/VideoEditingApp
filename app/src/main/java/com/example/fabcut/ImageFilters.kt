package com.example.fabcut

import android.graphics.Bitmap
import android.graphics.Color

object ImageFilters {

    fun blackAndWhite(bitmap: Bitmap): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        val output = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until width) {
            for (y in 0 until height) {

                val pixel = bitmap.getPixel(x, y)

                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                val gray = ((red + green + blue) / 3)

                output.setPixel(
                    x,
                    y,
                    Color.rgb(gray, gray, gray)
                )
            }
        }

        return output
    }
    fun bright(bitmap: Bitmap): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        val output = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until width) {

            for (y in 0 until height) {

                val pixel = bitmap.getPixel(x, y)

                var r = Color.red(pixel) + 40
                var g = Color.green(pixel) + 40
                var b = Color.blue(pixel) + 40

                r = r.coerceAtMost(255)
                g = g.coerceAtMost(255)
                b = b.coerceAtMost(255)

                output.setPixel(
                    x,
                    y,
                    Color.rgb(r, g, b)
                )
            }
        }

        return output
    }
    fun warm(bitmap: Bitmap): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        val output = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until width) {
            for (y in 0 until height) {

                val pixel = bitmap.getPixel(x, y)

                var r = Color.red(pixel) + 20
                var g = Color.green(pixel) + 5
                var b = Color.blue(pixel) - 20

                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                output.setPixel(
                    x,
                    y,
                    Color.rgb(r, g, b)
                )
            }
        }

        return output
    }
    fun cool(bitmap: Bitmap): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        val output = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until width) {
            for (y in 0 until height) {

                val pixel = bitmap.getPixel(x, y)

                var r = Color.red(pixel) - 15
                var g = Color.green(pixel)
                var b = Color.blue(pixel) + 25

                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                output.setPixel(
                    x,
                    y,
                    Color.rgb(r, g, b)
                )
            }
        }

        return output
    }
    fun vintage(bitmap: Bitmap): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        val output = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        for (x in 0 until width) {
            for (y in 0 until height) {

                val pixel = bitmap.getPixel(x, y)

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                var newR = (r * 0.9 + 30).toInt()
                var newG = (g * 0.85 + 20).toInt()
                var newB = (b * 0.7).toInt()

                newR = newR.coerceIn(0, 255)
                newG = newG.coerceIn(0, 255)
                newB = newB.coerceIn(0, 255)

                output.setPixel(
                    x,
                    y,
                    Color.rgb(newR, newG, newB)
                )
            }
        }

        return output
    }
}