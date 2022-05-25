package com.dropbox.dropshots

import android.graphics.Bitmap
import com.dropbox.differ.Color
import com.dropbox.differ.Image

internal class BitmapImage(private val src: Bitmap) : Image {
  override val width: Int get() = src.width
  override val height: Int get() = src.height
  override fun getPixel(x: Int, y: Int): Color {
    try {
      return Color(src.getPixel(x, y))
    } catch (e: IllegalArgumentException) {
      throw IllegalArgumentException("Can't request pixel {x = $x, y = $y} from image {width = $width, height = $height}")
    }
  }
}
