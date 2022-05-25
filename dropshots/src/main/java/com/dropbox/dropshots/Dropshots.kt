package com.dropbox.dropshots

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import com.dropbox.differ.Mask
import com.dropbox.differ.SimpleImageComparator
import java.io.File
import java.io.FileNotFoundException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

public class Dropshots : TestRule {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private var fqName: String = ""
  private var packageName: String = ""
  private var className: String = ""
  private var testName: String = ""

  private val snapshotName: String get() = "${packageName}_${className}_${testName}"

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        fqName = description.className
        packageName = fqName.substringBeforeLast('.', missingDelimiterValue = "")
        className = fqName.substringAfterLast('.', missingDelimiterValue = "")
        testName = description.methodName

        try {
          base.evaluate()
        } finally {

        }
      }
    }
  }

  /**
   * Compares a screenshot of the view to a references screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * See [Documentation](https://dropbox-kms.atlassian.net/wiki/spaces/MF/pages/521306389/Android+Screenshot+Testing) for more.
   */
  public fun assertSnapshot(
    view: View,
    name: String = snapshotName
  ) = assertSnapshot(Screenshot.capture(view).bitmap, name)

  /**
   * Compares a screenshot of the activity to a references screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * See [Documentation](https://dropbox-kms.atlassian.net/wiki/spaces/MF/pages/521306389/Android+Screenshot+Testing) for more.
   */
  public fun assertSnapshot(
    activity: Activity,
    name: String = snapshotName
  ) = assertSnapshot(Screenshot.capture(activity).bitmap, name)

  @Suppress("LongMethod")
  public fun assertSnapshot(
    bitmap: Bitmap,
    name: String
  ) {
    val filename = name.toFilename()

    val reference = try {
      context.assets.open("$filename.png").use {
        BitmapFactory.decodeStream(it)
      }
    } catch (e: FileNotFoundException) {
      if (isRecordingScreenshots()) {
        writeImage(filename, bitmap)
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
    val mask = Mask(bitmap.width, bitmap.height)
    val result = try {
      differ.compare(BitmapImage(reference), BitmapImage(bitmap), mask)
    } catch (e: IllegalArgumentException) {
      if (isRecordingScreenshots()) {
        writeImage(filename, bitmap)
        return
      } else {
        val diffImage = generateDiffImage(reference, bitmap, mask)
        val outputFilePath = writeImage(filename, diffImage)
        throw IllegalArgumentException(
          "Failed to compare images: reference{width=${reference.width}, height=${reference.height}} " +
            "<> bitmap{width=${bitmap.width}, height=${bitmap.height}}\n" +
            "Output written to: $outputFilePath",
          e
        )
      }
    }

    // Assert
    if (result.pixelDifferences != 0) {
      if (isRecordingScreenshots()) {
        writeImage(filename, bitmap)
      } else {
        val diffImage = generateDiffImage(reference, bitmap, mask)
        val outputFilePath = writeImage(filename, diffImage)
        throw AssertionError(
          "\"$name\" failed to match reference image. ${result.pixelDifferences} pixels differ " +
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
    val externalStoragePath = System.getenv("EXTERNAL_STORAGE")!!
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
      val info = context.packageManager.getApplicationInfo(
        context.packageName, PackageManager.GET_META_DATA
      )
      val packageName = info.packageName
      val buildConfigClassName = "$packageName.BuildConfig"
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
