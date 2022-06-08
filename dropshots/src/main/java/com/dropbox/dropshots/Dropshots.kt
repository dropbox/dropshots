package com.dropbox.dropshots

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Environment
import android.util.Base64
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

public class Dropshots(
  public val recordScreenshots: Boolean = isRecordingScreenshots()
) : TestRule {
  private val context = InstrumentationRegistry.getInstrumentation().context
  private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
  private var fqName: String = ""
  private var packageName: String = ""
  private var className: String = ""
  private var testName: String = ""

  private val snapshotName: String get() = testName

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
      if (recordScreenshots) {
        writeImage(filename, bitmap)
        return
      } else {
        throw IllegalStateException("Failed to find reference image named $filename.png. " +
          "If this is a new test, you may need to record screenshots by adding `recordScreenshots=true` to your gradle.properties file, or gradlew with `-PrecordScreenshots`.", e)
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
      writeThen(filename, reference, bitmap, mask) {
        IllegalArgumentException(
          "Failed to compare images: reference{width=${reference.width}, height=${reference.height}} " +
            "<> bitmap{width=${bitmap.width}, height=${bitmap.height}}\n" +
            "Output written to: $it",
          e
        )
      }
      return
    }

    // Assert
    if (result.pixelDifferences != 0) {
      writeThen(filename, reference, bitmap, mask) {
        AssertionError("\"$name\" failed to match reference image. ${result.pixelDifferences} pixels differ " +
          "(${(result.pixelDifferences / result.pixelCount.toFloat()) * 100} %)\n" +
          "Output written to: $it")
      }
    }
  }

  /**
   * Writes the test image if recording screenshots, otherwise creates and
   * writes a diff, then throws the resulting throwable.
   */
  private fun writeThen(
    filename: String,
    referenceImage: Bitmap,
    testImage: Bitmap,
    mask: Mask,
    message: (outputFilePath: String) -> Throwable
  ) {
    if (recordScreenshots) {
      writeImage(filename, testImage)
    } else {
      val diffImage = generateDiffImage(referenceImage, testImage, mask)
      val outputFilePath = writeImage(filename, diffImage)
      throw message(outputFilePath)
    }
  }

  /**
   * Writes the given screenshot to the external storage directory.
   */
  private fun writeImage(name: String, screenshot: Bitmap): String {
    val externalStorageDir = Environment
      .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val screenFolder = File(externalStorageDir, "screenshots/${targetContext.packageName}")
    if (!screenFolder.exists() && !screenFolder.mkdirs()) {
      throw IllegalStateException("Unable to create screenshot storage directory.")
    }

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
}

/**
 * Reads the target application's `is_recording_screenshots` boolean resource to determine if
 * Dropshots should record screenshots or validate them.
 */
internal fun isRecordingScreenshots(): Boolean {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val resId = context.resources.getIdentifier(
    "is_recording_screenshots",
    "bool",
    context.packageName
  )
  return if (resId == 0) false else context.resources.getBoolean(resId)
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
