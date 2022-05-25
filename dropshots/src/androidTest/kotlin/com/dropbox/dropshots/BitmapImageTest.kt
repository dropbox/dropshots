package com.dropbox.dropshots

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import com.dropbox.differ.Color as DColor
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class BitmapImageTest {

  private lateinit var bitmap: Bitmap
  private lateinit var image: BitmapImage

  @Before
  fun setup() {
    bitmap = Bitmap
      .createBitmap(10, 10, Bitmap.Config.ARGB_8888)
      .applyCanvas {
        drawColor(Color.WHITE)

        val paint = Paint().apply { color = Color.RED }
        drawRect(0f, 5f, 5f, 0f, paint)

        paint.color = Color.GREEN
        drawRect(5f, 10f, 10f, 5f, paint)

        paint.color = Color.BLACK
        drawRect(5f, 5f, 10f, 0f, paint)
      }
    image = BitmapImage(bitmap)
  }

  @Test
  fun matchesBitmapDimensions() {
    assertEquals(10, image.width)
    assertEquals(10, image.height)
  }

  @Test
  fun getsCorrectColor() {
    assertEquals(DColor(Color.RED), image.getPixel(0, 0))
    assertEquals(DColor(Color.RED), image.getPixel(4, 4))
    assertEquals(DColor(Color.WHITE), image.getPixel(0, 9))
    assertEquals(DColor(Color.GREEN), image.getPixel(9, 9))
    assertEquals(DColor(Color.BLACK), image.getPixel(9, 0))
  }

  @Test
  fun throwsIllegalArgumentExceptionWhenOutOfRange() {
    try {
      image.getPixel(-1, 0)
      fail("Expected IllegalArgumentException when out of range ")
    } catch (e: IllegalArgumentException) {
      // pass
    }
    try {
      image.getPixel(0, -1)
      fail("Expected IllegalArgumentException when out of range ")
    } catch (e: IllegalArgumentException) {
      // pass
    }
    try {
      image.getPixel(10, 0)
      fail("Expected IllegalArgumentException when out of range ")
    } catch (e: IllegalArgumentException) {
      // pass
    }
    try {
      image.getPixel(0, 10)
      fail("Expected IllegalArgumentException when out of range ")
    } catch (e: IllegalArgumentException) {
      // pass
    }
  }
}
