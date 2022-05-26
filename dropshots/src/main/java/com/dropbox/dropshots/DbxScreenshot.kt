package com.dropbox.dropshots

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot
import com.dropbox.differ.Mask
import com.dropbox.differ.SimpleImageComparator
import java.io.File
import java.io.FileNotFoundException

private const val SCREENSHOT_FILE_NAME_SEPARATOR = "___"

object DbxScreenshot {
  fun snapActivity(activity: Activity): DbxScreenshotBuilder = makeScreenshotBuilder(activity)
  fun snap(view: View): DbxScreenshotBuilder = makeScreenshotBuilder(view)
}

fun makeScreenshotBuilder(
  activity: Activity,
  screenshotName: String? = null,
  shouldIncludeAccessibilityInfo: Boolean? = null
) = DbxScreenshotBuilder(
  activity,
  screenshotName,
  shouldIncludeAccessibilityInfo,
  screenshotProvider = { Screenshot.capture(activity) }
)

fun makeScreenshotBuilder(
  view: View,
  screenshotName: String? = null,
  shouldIncludeAccessibilityInfo: Boolean? = null
) = DbxScreenshotBuilder(
  view.context,
  screenshotName,
  shouldIncludeAccessibilityInfo,
  screenshotProvider = { Screenshot.capture(view) }
)

class DbxScreenshotBuilder internal constructor(
  val context: Context,
  private var screenshotName: String? = null,
  private var shouldIncludeAccessibilityInfo: Boolean? = null,
  private val screenshotProvider: () -> ScreenCapture
) {

  fun setName(newName: String): DbxScreenshotBuilder {
    screenshotName = newName
    return this
  }

  fun setIncludeAccessibilityInfo(includeInfo: Boolean): DbxScreenshotBuilder {
    shouldIncludeAccessibilityInfo = includeInfo
    return this
  }

  /**
   * Compares the current screenshot to a references screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   */
  @Suppress("LongMethod")
  fun assertUnchanged() {
    val testImage = screenshotProvider().bitmap
    val filename = screenshotName!!.toFilename()

    val reference = try {
      context.assets.open("$filename.png").use {
        BitmapFactory.decodeStream(it)
      }
    } catch (e: FileNotFoundException) {
      if (isRecordingScreenshots()) {
        writeImage(filename, testImage)
        return
      } else {
        throw e
      }
    }

    val differ = SimpleImageComparator(
      maxDistance = 0.004f,
      hShift = 5,
      vShift = 5,
    )
    val mask = Mask(testImage.width, testImage.height)
    val result = try {
      differ.compare(BitmapImage(reference), BitmapImage(testImage), mask)
    } catch (e: IllegalArgumentException) {
      if (isRecordingScreenshots()) {
        writeImage(filename, testImage)
        return
      } else {
        val diffImage = generateDiffImage(reference, testImage, mask)
        val outputFilePath = writeImage(filename, diffImage)
        throw IllegalArgumentException(
          "Failed to compare images: reference{width=${reference.width}, height=${reference.height}} " +
            "<> bitmap{width=${testImage.width}, height=${testImage.height}}\n" +
            "Output written to: $outputFilePath",
          e
        )
      }
    }

    // Assert
    if (result.pixelDifferences != 0) {
      if (isRecordingScreenshots()) {
        writeImage(filename, testImage)
      } else {
        val diffImage = generateDiffImage(reference, testImage, mask)
        val outputFilePath = writeImage(filename, diffImage)
        throw AssertionError(
          "\"$screenshotName\" failed to match reference image. ${result.pixelDifferences} pixels differ " +
            "(${(result.pixelDifferences / result.pixelCount.toFloat()) * 100} %)\n" +
            "Output written to: $outputFilePath"
        )
      }
    }
  }

  /**
   * Writes the given screenshot to the external storage directory.
   */
  private fun writeImage(name: String, screenshot: Bitmap): String {
    val externalStoragePath = "/storage/emulated/0/Download" // System.getenv("EXTERNAL_STORAGE")!!
    val screenFolder = File("$externalStoragePath/screenshots/${context.packageName}")
    screenFolder.mkdirs()

    // Changes doesn't support spaces in artifact names: https://jira.dropboxer.net/browse/DEVHELP-1350
    val file = File(screenFolder, "${name.replace(" ", "_")}.png")
    file.outputStream().use {
      screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
    return file.absolutePath
  }

  /**
   * Generates a `Bitmap` consisting of the reference image, the test image, and
   * an image that highlights the differences between the two.
   */
  private fun generateDiffImage(referenceImage: Bitmap, testImage: Bitmap, differenceMask: Mask): Bitmap {
    // Render the failed screenshots to an output image
    val output = Bitmap.createBitmap(
      referenceImage.width + testImage.width + differenceMask.width,
      maxOf(referenceImage.height, testImage.height, differenceMask.height),
      Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(output)
    canvas.drawBitmap(referenceImage, 0f, 0f, null)
    canvas.drawBitmap(referenceImage, referenceImage.width.toFloat(), 0f, null)
    canvas.drawBitmap(testImage, referenceImage.width.toFloat() * 2, 0f, null)

    val diffPaint = Paint().apply {
      color = 0x3DFF0000
      strokeWidth = 0f
    }
    val otherPaint = Paint().apply {
      color = 0x3D000000
      strokeWidth = 0f
    }
    (0 until differenceMask.height).forEach { y ->
      (0 until differenceMask.width).forEach { x ->
        val paint = if (differenceMask.getValue(x, y) > 0) diffPaint else otherPaint
        canvas.drawPoint(referenceImage.width + x.toFloat(), y.toFloat(), paint)
      }
    }
    return output
  }

  private fun isRecordingScreenshots(): Boolean {
    try {
      val info = context.packageManager
        .getApplicationInfo(
          context.packageName, PackageManager.GET_META_DATA
        )
      val packageName = info.packageName
      val buildConfigClassName =
        "$packageName.BuildConfig"
      val clazz = Class.forName(buildConfigClassName)
      val field = clazz.getField("IS_RECORD_SCREENSHOTS")
      return field.getBoolean(clazz)
    } catch (e: Exception) {
      when (e) {
        is PackageManager.NameNotFoundException, is ClassNotFoundException,
        is NoSuchFieldException, is IllegalAccessException ->
          Log.d("Dropshots", "Unable to find project path: $e")
        else ->
          throw e
      }
    }
    return false
  }
}

/**
 * Creates a filename from the requested name that'll be 64 characters or less.
 *
 * If the requested name is longer that 64 characters, it'll be shortened by
 * encoding the end of the name, leaving the beginning in tact to hopefully provide
 * somewhat human readable names while still trying to prevent collisions.
 */
internal fun String.toFilename(): String {
  return if (length < 64) {
    this
  } else {
    val prefix = substring(0, 32)
    val suffix = String(Base64.encode(substring(32).toByteArray(), Base64.DEFAULT)).trim()
    "${prefix}_$suffix"
  }.let {
    // Changes doesn't support spaces in artifact names so we also have to
    // replace them with underscores.
    // See https://jira.dropboxer.net/browse/DEVHELP-1350
    it.replace(" ", "_")
  }
}
