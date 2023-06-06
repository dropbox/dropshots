package com.dropbox.dropshots

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.screenshot.Screenshot
import com.dropbox.differ.ImageComparator
import com.dropbox.differ.Mask
import com.dropbox.differ.SimpleImageComparator
import java.io.File
import java.io.FileNotFoundException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

public class Dropshots(
  /**
   * Function to create a filename from a snapshot name (i.e. the name provided when taking
   * the snapshot).
   */
  private val filenameFunc: (String) -> String = defaultFilenameFunc,
  /**
   * Indicates whether new reference screenshots should be recorded. Otherwise Dropshots performs
   * validation of test screenshots against reference screenshots.
   */
  private val recordScreenshots: Boolean = isRecordingScreenshots(),
  /**
   * The `ImageComparator` used to compare test and reference screenshots.
   */
  private val imageComparator: ImageComparator = SimpleImageComparator(maxDistance = 0.004f),
  /**
   * The `ResultValidator` used to validate the comparison results.
   */
  private val resultValidator: ResultValidator = CountValidator(0)
) : TestRule {
  private val context = InstrumentationRegistry.getInstrumentation().context
  private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
  private var fqName: String = ""
  private var packageName: String = ""
  private var className: String = ""
  private var testName: String = ""

  private val snapshotName: String get() = testName

  override fun apply(base: Statement, description: Description): Statement {
    fqName = description.className
    packageName = fqName.substringBeforeLast('.', missingDelimiterValue = "")
    className = fqName.substringAfterLast('.', missingDelimiterValue = "")
    testName = description.methodName

    return if (Build.VERSION.SDK_INT <= 29) {
      GrantPermissionRule
        .grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .apply(base, description)
    } else {
      base
    }
  }

  /**
   * Compares a screenshot of the view to a references screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param filePath where the screenshots should be store in project eg. "views/colors"
   */
  public fun assertSnapshot(
    view: View,
    name: String = snapshotName,
    filePath: String? = null,
  ) = assertSnapshot(Screenshot.capture(view).bitmap, name, filePath)

  /**
   * Compares a screenshot of the activity to a references screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param filePath where the screenshots should be store in project eg. "views/colors"
   */
  public fun assertSnapshot(
    activity: Activity,
    name: String = snapshotName,
    filePath: String? = null,
  ) = assertSnapshot(Screenshot.capture(activity).bitmap, name, filePath)

  @Suppress("LongMethod")
  public fun assertSnapshot(
    bitmap: Bitmap,
    name: String = snapshotName,
    filePath: String? = null,
  ) {
    val filename = filenameFunc(name)

    val reference = try {
      context.assets.open("$filename.png".prependPath(filePath)).use {
        BitmapFactory.decodeStream(it)
      }
    } catch (e: FileNotFoundException) {
      if (recordScreenshots) {
        writeImage(filename, filePath, bitmap)
        return
      } else {
        throw IllegalStateException(
          "Failed to find reference image named /$filename.png at path $filePath . " +
            "If this is a new test, you may need to record screenshots by adding `dropshots.record=true` to your gradle.properties file, or gradlew with `-Pdropshots.record`.",
          e
        )
      }
    }

    if (bitmap.width != reference.width || bitmap.height != reference.height) {
      if (recordScreenshots) {
        writeImage(filename, filePath, bitmap)
        return
      } else {
        writeThen(filename, filePath, reference, bitmap, null) { outputPath ->
          AssertionError(
            "$name: Test image (w=${bitmap.width}, h=${bitmap.height}) differs in size" +
              " from reference image (w=${reference.width}, h=${reference.height}).\n" +
              "Output written to: $outputPath"
          )
        }
      }
    }

    val mask = Mask(bitmap.width, bitmap.height)
    val result = try {
      imageComparator.compare(BitmapImage(reference), BitmapImage(bitmap), mask)
    } catch (e: IllegalArgumentException) {
      writeThen(filename, filePath, reference, bitmap, mask) {
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
    if (!resultValidator(result)) {
      writeThen(filename, filePath, reference, bitmap, mask) {
        AssertionError(
          "\"$name\" failed to match reference image. ${result.pixelDifferences} pixels differ " +
            "(${(result.pixelDifferences / result.pixelCount.toFloat()) * 100} %)\n" +
            "Output written to: $it"
        )
      }
    }
  }

  /**
   * Writes the test image if recording screenshots, otherwise creates and
   * writes a diff, then throws the resulting throwable.
   */
  private fun writeThen(
    filename: String,
    filePath: String?,
    referenceImage: Bitmap,
    testImage: Bitmap,
    mask: Mask?,
    message: (outputFilePath: String) -> Throwable
  ) {
    if (recordScreenshots) {
      writeImage(filename, filePath, testImage)
    } else {
      val diffImage = generateDiffImage(referenceImage, testImage, mask)
      val outputFilePath = writeImage(filename, filePath, diffImage)
      throw message(outputFilePath)
    }
  }

  /**
   * Writes the given screenshot to the external storage directory.
   */
  private fun writeImage(name: String, filePath: String?, screenshot: Bitmap): String {
    val externalStorageDir = Environment
      .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val screenFolder = File(externalStorageDir, "screenshots/${targetContext.packageName}".appendPath(filePath))
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
  private fun generateDiffImage(
    referenceImage: Bitmap,
    testImage: Bitmap,
    differenceMask: Mask?
  ): Bitmap {
    // Render the failed screenshots to an output image
    val maskWidth = differenceMask?.width ?: 0
    val maskHeight = differenceMask?.height ?: 0
    val output = Bitmap.createBitmap(
      referenceImage.width + testImage.width + maskWidth,
      maxOf(referenceImage.height, testImage.height, maskHeight),
      Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(output)
    canvas.drawBitmap(referenceImage, 0f, 0f, null)
    canvas.drawBitmap(testImage, referenceImage.width.toFloat() + maskWidth, 0f, null)

    // If we have a mask, draw it between the reference image and the test image.
    if (differenceMask != null) {
      canvas.drawBitmap(referenceImage, referenceImage.width.toFloat(), 0f, null)

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
internal val defaultFilenameFunc = { testName: String ->
  if (testName.length < 64) {
    testName
  } else {
    val prefix = testName.substring(0, 32)
    val suffix = String(Base64.encode(testName.substring(32).toByteArray(), Base64.DEFAULT)).trim()
    "${prefix}_$suffix"
  }.let {
    // Some CI filesystems don't support spaces in artifact names so we also have to
    // replace them with underscores.
    it.replace(" ", "_")
  }
}

private fun String.prependPath(path: String?): String =
  if (path == null) {
    this
  } else {
    "$path/$this"
  }

private fun String.appendPath(path: String?): String =
  if (path == null) {
    this
  } else {
    "$this/$path"
  }
