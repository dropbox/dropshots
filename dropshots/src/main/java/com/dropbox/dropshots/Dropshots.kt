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

public class Dropshots internal constructor(
  private val rootScreenshotDirectory: File,
  private val filenameFunc: (String, String) -> String,
  private val recordScreenshots: Boolean,
  private val imageComparator: ImageComparator,
  private val resultValidator: ResultValidator,
) : TestRule {
  private val context = InstrumentationRegistry.getInstrumentation().context
  private var fqName: String = ""
  private var packageName: String = ""
  private var className: String = ""
  private var testName: String = ""

  private val snapshotName: String get() = testName

  @JvmOverloads
  public constructor(
    /**
     * Function to create a filename from the class name and snapshot name (i.e. the name provided when taking
     * the snapshot). Default behavior will use the calling function name.
     */
    filenameFunc: (String, String) -> String = defaultFilenameFunc,
    /**
     * Indicates whether new reference screenshots should be recorded. Otherwise Dropshots performs
     * validation of test screenshots against reference screenshots.
     */
    recordScreenshots: Boolean = isRecordingScreenshots(defaultRootScreenshotDirectory()),
    /**
     * The `ImageComparator` used to compare test and reference screenshots.
     */
    imageComparator: ImageComparator = SimpleImageComparator(maxDistance = 0.004f),
    /**
     * The `ResultValidator` used to validate the comparison results.
     */
    resultValidator: ResultValidator = CountValidator(0),
  ): this(defaultRootScreenshotDirectory(), filenameFunc, recordScreenshots, imageComparator, resultValidator)

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
   * Compares a screenshot of the view to a reference screenshot from the test application's assets.
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
  ): Unit = assertSnapshot(Screenshot.capture(view).bitmap, name, filePath)

  /**
   * Compares a screenshot of the activity to a reference screenshot from the test application's assets.
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
  ): Unit = assertSnapshot(Screenshot.capture(activity).bitmap, name, filePath)

  /**
   * Compares a screenshot of the visible screen content to a reference screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param filePath where the screenshots should be store in project eg. "views/colors"
   */
  public fun assertSnapshot(
    name: String = snapshotName,
    filePath: String? = null,
  ): Unit = assertSnapshot(Screenshot.capture().bitmap, name, filePath)

  @Suppress("LongMethod")
  public fun assertSnapshot(
    bitmap: Bitmap,
    name: String = snapshotName,
    filePath: String? = null,
  ) {
    val filename = filenameFunc(className, name)

    val reference = try {
      context.assets.open("$filename.png".prependPath(filePath)).use {
        BitmapFactory.decodeStream(it)
      }
    } catch (e: FileNotFoundException) {
      writeReferenceImage(filename, filePath, bitmap)

      if (!recordScreenshots) {
        throw IllegalStateException(
          "Failed to find reference image named /$filename.png at path $filePath . " +
            "If this is a new test, you may need to record screenshots by running " +
            "record<variantSlug>Screenshots gradle task or pulling screenshots " +
            "from a previous run using pull<variantSlug>Screenshots gradle task",
          e
        )
      }

      return
    }

    if (bitmap.width != reference.width || bitmap.height != reference.height) {
      writeReferenceImage(filename, filePath, bitmap)

      if (!recordScreenshots) {
        val outputPath = writeDiffImage(filename, filePath, bitmap, reference, null)
        throw AssertionError(
          "$name: Test image (w=${bitmap.width}, h=${bitmap.height}) differs in size" +
            " from reference image (w=${reference.width}, h=${reference.height}).\n" +
            "Diff written to: $outputPath",
        )
      }
    }

    val mask = Mask(bitmap.width, bitmap.height)
    val result = try {
      imageComparator.compare(BitmapImage(reference), BitmapImage(bitmap), mask)
    } catch (e: IllegalArgumentException) {
      writeReferenceImage(filename, filePath, bitmap)

      if (!recordScreenshots) {
        val outputPath = writeDiffImage(filename, filePath, bitmap, reference, mask)
        throw AssertionError(
          "Failed to compare images: reference{width=${reference.width}, height=${reference.height}} " +
          "<> bitmap{width=${bitmap.width}, height=${bitmap.height}}\n" +
          "Diff written to: $outputPath",
          e,
        )
      }

      return
    }

    // Assert
    if (!resultValidator(result)) {
      writeReferenceImage(filename, filePath, bitmap)

      if (!recordScreenshots) {
        val outputPath = writeDiffImage(filename, filePath, bitmap, reference, mask)
        throw AssertionError(
          "\"$name\" failed to match reference image. ${result.pixelDifferences} pixels differ " +
            "(${(result.pixelDifferences / result.pixelCount.toFloat()) * 100} %)\n" +
            "Output written to: $outputPath"
        )
      }
    }
  }

  /**
   * Writes the given screenshot to the external reference image directory, returning the
   * file path of the file that was written.
   */
  private fun writeReferenceImage(name: String, filePath: String?, screenshot: Bitmap): String {
    val screenshotFolder = File(rootScreenshotDirectory, "reference".appendPath(filePath))
    return writeImage(screenshotFolder, name, screenshot)
  }

  /**
   * Writes the given screenshot to the external reference image directory, returning the
   * file path of the file that was written.
   */
  private fun writeDiffImage(
    name: String,
    filePath: String?,
    screenshot: Bitmap,
    referenceImage: Bitmap,
    mask: Mask?,
  ): String {
    val screenshotFolder = File(rootScreenshotDirectory, "diff".appendPath(filePath))
    val diffImage = generateDiffImage(referenceImage, screenshot, mask)
    return writeImage(screenshotFolder, name, diffImage)
  }

  private fun writeImage(dir: File, name: String, image: Bitmap): String {
    if (!dir.exists() && !dir.mkdirs()) {
      throw IllegalStateException("Unable to create screenshot storage directory.")
    }

    val file = File(dir, "${name.replace(" ", "_")}.png")
    if (file.exists()){
      throw IllegalStateException("Unable to create screenshot, file already exists. Please " +
        "specify name param with something more specific when calling assertSnapshot function")
    }

    file.outputStream().use {
      image.compress(Bitmap.CompressFormat.PNG, 100, it)
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

internal fun defaultRootScreenshotDirectory(): File {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val externalStorageDir = Environment
    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
  return File(externalStorageDir, "screenshots/${context.packageName}")
}

/**
 * Reads the target application's `is_recording_screenshots` boolean resource to determine if
 * Dropshots should record screenshots or validate them.
 */
internal fun isRecordingScreenshots(rootScreenshotDirectory: File): Boolean {
  val markerFile = File(rootScreenshotDirectory, ".isRecordingScreenshots")
  return markerFile.exists()
}

/**
 * Creates a filename from the requested name that'll be 64 characters or less.
 *
 * If the requested name is longer that 64 characters, it'll be shortened by
 * encoding the end of the name, leaving the beginning in tact to hopefully provide
 * somewhat human readable names while still trying to prevent collisions.
 */
internal val defaultFilenameFunc = { _: String, testName: String ->
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
