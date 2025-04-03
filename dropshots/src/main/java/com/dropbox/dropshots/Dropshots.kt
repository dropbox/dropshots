package com.dropbox.dropshots

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Environment
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
  private val recordScreenshots: Boolean,
  private val imageComparator: ImageComparator,
  private val resultValidator: ResultValidator,
) : TestRule {
  private val context = InstrumentationRegistry.getInstrumentation().context
  private var fqName: String = ""
  private var packageName: String = ""
  private var className: String = ""
  private var testName: String = ""

  @JvmOverloads
  public constructor(
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
  ): this(defaultRootScreenshotDirectory(), recordScreenshots, imageComparator, resultValidator)

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
   * @param name lambda to create a filename from the snapshot name (i.e. the name provided when
   * taking the snapshot), first parameter is the class name and the second is the test name.
   * Defaults to `<class-name>_<func-name>.png` limited to 255 chars, replacing all spaces with underscores.
   * @param filePath where the screenshots should be store in project under [com.dropbox.dropshots.DropshotsExtension.referenceOutputDirectory]
   */
  @Deprecated("Use assertSnapshot(view: View, fileName: (String, String) -> String, filePath: String?) instead")
  public fun assertSnapshot(
    view: View,
    name: String,
    filePath: String? = null,
  ): Unit = assertSnapshot(Screenshot.capture(view).bitmap, { _, _ -> name }, filePath)

  /**
   * Compares a screenshot of the [Activity] to a reference screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param name lambda to create a filename from the snapshot name (i.e. the name provided when
   * taking the snapshot), first parameter is the class name and the second is the test name.
   * Defaults to `<class-name>_<func-name>.png` limited to 255 chars, replacing all spaces with underscores.
   * @param filePath where the screenshots should be store in project under [com.dropbox.dropshots.DropshotsExtension.referenceOutputDirectory]
   */
  @Deprecated("Use assertSnapshot(activity: Activity, fileName: (String, String) -> String, filePath: String?) instead")
  public fun assertSnapshot(
    activity: Activity,
    name: String,
    filePath: String? = null,
  ): Unit = assertSnapshot(Screenshot.capture(activity).bitmap, { _, _ -> name }, filePath)

  /**
   * Compares a screenshot of the visible screen content to a reference screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param name lambda to create a filename from the snapshot name (i.e. the name provided when
   * taking the snapshot), first parameter is the class name and the second is the test name.
   * Defaults to `<class-name>_<func-name>.png` limited to 255 chars, replacing all spaces with underscores.
   * @param filePath where the screenshots should be store in project under [com.dropbox.dropshots.DropshotsExtension.referenceOutputDirectory]
   */
  @Deprecated("Use assertSnapshot(fileName: (String, String) -> String, filePath: String?) instead")
  public fun assertSnapshot(
    name: String,
    filePath: String? = null,
  ): Unit = assertSnapshot(Screenshot.capture().bitmap, { _, _ -> name }, filePath)


  /**
   * Compares a [bitmap] to a reference screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param bitmap the bitmap to assert
   * @param name lambda to create a filename from the snapshot name (i.e. the name provided when
   * taking the snapshot), first parameter is the class name and the second is the test name.
   * Defaults to `<class-name>_<func-name>.png` limited to 255 chars, replacing all spaces with underscores.
   * @param filePath where the screenshots should be store in project under [com.dropbox.dropshots.DropshotsExtension.referenceOutputDirectory]
   */
  @Deprecated("Use assertSnapshot(fileName: (String, String) -> String, filePath: String?) instead")
  @Suppress("LongMethod")
  public fun assertSnapshot(
    bitmap: Bitmap,
    name: String,
    filePath: String? = null,
  ) : Unit = assertSnapshot(bitmap, { _, _ -> name }, filePath)

  /**
   * Compares a screenshot of the view to a reference screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param fileName lambda to create a filename from the snapshot name (i.e. the name provided when
   * taking the snapshot), first parameter is the class name and the second is the test name.
   * Defaults to `<class-name>_<func-name>.png` limited to 255 chars, replacing all spaces with underscores.
   * @param filePath where the screenshots should be store in project under [com.dropbox.dropshots.DropshotsExtension.referenceOutputDirectory]
   */
  public fun assertSnapshot(
    view: View,
    fileName: (String, String) -> String = defaultFilenameFunc,
    filePath: String? = null,
  ): Unit = assertSnapshot(Screenshot.capture(view).bitmap, fileName, filePath)

  /**
   * Compares a screenshot of the [Activity] to a reference screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param fileName lambda to create a filename from the snapshot name (i.e. the name provided when
   * taking the snapshot), first parameter is the class name and the second is the test name.
   * Defaults to `<class-name>_<func-name>.png` limited to 255 chars, replacing all spaces with underscores.
   * @param filePath where the screenshots should be store in project under [com.dropbox.dropshots.DropshotsExtension.referenceOutputDirectory]
   */
  public fun assertSnapshot(
    activity: Activity,
    fileName: (String, String) -> String = defaultFilenameFunc,
    filePath: String? = null,
  ): Unit = assertSnapshot(Screenshot.capture(activity).bitmap, fileName, filePath)

  /**
   * Compares a screenshot of the visible screen content to a reference screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param fileName lambda to create a filename from the snapshot name (i.e. the name provided when
   * taking the snapshot), first parameter is the class name and the second is the test name.
   * Defaults to `<class name>_<func-name>.png` limited to 255 chars, replacing all spaces with underscores.
   * @param filePath where the screenshots should be store in project under [com.dropbox.dropshots.DropshotsExtension.referenceOutputDirectory]
   */
  public fun assertSnapshot(
    fileName: (String, String) -> String = defaultFilenameFunc,
    filePath: String? = null,
  ): Unit = assertSnapshot(Screenshot.capture().bitmap, fileName, filePath)


  /**
   * Compares a [bitmap] to a reference screenshot from the test application's assets.
   *
   * If `BuildConfig.IS_RECORD_SCREENSHOTS` is set to `true`, then the screenshot will simply be written
   * to disk to be pulled to the host machine to update the reference images.
   *
   * @param bitmap the bitmap to assert
   * @param fileName lambda to create a filename from the snapshot name (i.e. the name provided when
   * taking the snapshot), first parameter is the class name and the second is the test name.
   * Defaults to `<class name>_<func-name>.png` limited to 255 chars, replacing all spaces with underscores.
   * @param filePath where the screenshots should be store in project under [com.dropbox.dropshots.DropshotsExtension.referenceOutputDirectory]
   */
  @Suppress("LongMethod")
  public fun assertSnapshot(
    bitmap: Bitmap,
    fileName: (String, String) -> String = defaultFilenameFunc,
    filePath: String? = null,
  ) {
    // Some CI filesystems don't support spaces in artifact names so we also have to replace them with underscores.
    val filename = fileName(className, testName).replace(" ", "_")
    require(filename.length < 255 - "_diff".length){
      "Screenshot file-name exceeds max length: $filename"
    }

    val reference = try {
      context.assets.open("$filename.png".prependPath(filePath)).use {
        BitmapFactory.decodeStream(it)
      }
    } catch (e: FileNotFoundException) {
      writeReferenceImage(filename, filePath, bitmap)

      if (!recordScreenshots) {
        throw IllegalStateException(
          "Failed to find reference image named /$filename.png at path $filePath . " +
            "If this is a new test, you may need to record screenshots by running the record<variantSlug>Screenshots gradle task",
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
          "$filename: Test image (w=${bitmap.width}, h=${bitmap.height}) differs in size" +
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
          "\"$filename\" failed to match reference image. ${result.pixelDifferences} pixels differ " +
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
    return writeImage(screenshotFolder, name, diffImage, "_diff")
  }

  private fun writeImage(dir: File, name: String, image: Bitmap, nameSuffix: String = ""): String {
    if (!dir.exists() && !dir.mkdirs()) {
      throw IllegalStateException("Unable to create screenshot storage directory.")
    }

    val file = File(dir, "${name.replace(" ", "_")}$nameSuffix.png")

    if (file.exists()){
      throw IllegalStateException("Unable to create screenshot, file already exists. Please " +
        "override fileName when calling assertSnapshot and include qualifiers in your file name.")
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
 * Creates a filename from class-name and test-name of where the screenshot was taken.
 */
internal val defaultFilenameFunc = { className: String?, testName: String ->
  if (className.isNullOrEmpty()){
    testName
  } else {
    "${className}_${testName}"
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
