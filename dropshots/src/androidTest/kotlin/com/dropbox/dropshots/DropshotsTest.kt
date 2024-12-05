package com.dropbox.dropshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.core.net.toFile
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.io.PlatformTestStorageRegistry
import com.dropbox.differ.SimpleImageComparator
import com.dropbox.dropshots.model.TestRunConfig
import java.io.File
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class DropshotsTest {

  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

  private val fakeValidator = FakeResultValidator()
  private var filenameFunc: (String) -> String = { it }
  private lateinit var testStorage: FileTestStorage

  @get:Rule val testName = TestName()
  @get:Rule
  val dropshots = Dropshots(
    filenameFunc = filenameFunc,
    resultValidator = fakeValidator,
    imageComparator = SimpleImageComparator(
      maxDistance = 0.004f,
      hShift = 1,
      vShift = 1,
    ),
  )

  @Before
  fun setup() {
    val imageDirUri = PlatformTestStorageRegistry.getInstance()
      .getOutputFileUri("dropshots-tests")
    testStorage = FileTestStorage(
      imageDirUri.buildUpon().appendPath("input").build().toFile(),
      imageDirUri.buildUpon().appendPath("output").build().toFile()
    )
    fakeValidator.validator = CountValidator(0)
  }

  @After
  fun after() {
    testStorage.outputDir.takeIf { it.exists() }?.deleteRecursively()
    testStorage.inputDir.takeIf { it.exists() }?.deleteRecursively()
  }

  @Test
  fun testMatchesFullScreenshot() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot("MatchesFullScreenshot")
    }
  }

  @Test
  fun testMatchesActivityScreenshot() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(it, "MatchesActivityScreenshot")
    }
  }

  @Test
  fun testMatchesViewScreenshot() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(
        it.findViewById<View>(android.R.id.content),
        name = "MatchesViewScreenshot"
      )
    }
  }

  @Test
  fun testWritesReferenceImageForMissingImagesWhenRecording() {
    val dropshots = Dropshots(
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      testRunConfig = TestRunConfig(isRecording = true, deviceName = "test"),
      resultValidator = { false },
      imageComparator = SimpleImageComparator(),
    )

    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(it, "not-an-image")
    }

    with(File(testStorage.outputDir, "dropshots/reference")) {
      assertTrue(exists())
      assertArrayEquals(arrayOf("not-an-image.png"), list())
    }
  }

  @Test
  fun testWritesReferenceImageForMissingImagesWhenNotRecording() {
    val dropshots = Dropshots(
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      testRunConfig = TestRunConfig(isRecording = false, deviceName = "test"),
      resultValidator = { false },
      imageComparator = SimpleImageComparator(),
    )

    activityScenarioRule.scenario.onActivity {
      var failed = false
      try {
        dropshots.assertSnapshot(it, "not-an-image")
        failed = true
      } catch (_: IllegalStateException) {
        // expected
      }
      if (failed) {
        fail("Expected snapshot assertion to fail but it passed.")
      }
    }

    with(File(testStorage.outputDir, "dropshots/reference")) {
      assertTrue(exists())
      assertArrayEquals(arrayOf("not-an-image.png"), list())
    }
  }

  @Test
  fun testWritesDiffImageOnFailureWhenRecording() {
    val dropshots = Dropshots(
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      testRunConfig = TestRunConfig(isRecording = true, deviceName = "test"),
      resultValidator = { false },
      imageComparator = SimpleImageComparator(),
    )

    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(it, "MatchesViewScreenshot")
    }

    with(File(testStorage.outputDir, "dropshots/diff")) {
      assertTrue(exists())
      assertArrayEquals(arrayOf("MatchesViewScreenshot.png"), list())
    }
  }

  @Test
  fun testWritesDiffImageOnFailureWhenNotRecording() {
    val dropshots = Dropshots(
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      testRunConfig = TestRunConfig(isRecording = false, deviceName = "test"),
      resultValidator = { false },
      imageComparator = SimpleImageComparator(),
    )

    activityScenarioRule.scenario.onActivity {
      var failed = false
      try {
        dropshots.assertSnapshot(it, "MatchesViewScreenshot")
        failed = true
      } catch (_: AssertionError) {
        // expected
      }
      if (failed) {
        fail("Expected snapshot assertion to fail but it passed.")
      }
    }

    with(File(testStorage.outputDir, "dropshots/diff")) {
      assertTrue(exists())
      assertArrayEquals(arrayOf("MatchesViewScreenshot.png"), list())
    }
  }

  @Test
  fun testFailsForDifferences() {
    val dropshots = Dropshots(
      resultValidator = CountValidator(0),
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      testRunConfig = TestRunConfig(isRecording = false, deviceName = "test"),
      imageComparator = SimpleImageComparator(),
    )

    var caughtError: AssertionError? = null
    activityScenarioRule.scenario.onActivity {
      try {
        dropshots.assertSnapshot(
          view = it.findViewById(android.R.id.content),
          name = "MatchesViewScreenshotBad",
          filePath = "static"
        )
      } catch (e: AssertionError) {
        caughtError = e
      }
    }

    assertNotNull("Expected error when screenshots differ.", caughtError)
  }

  @Test
  fun testPassesWhenValidatorPasses() {
    val dropshots = Dropshots(
      resultValidator = FakeResultValidator { true },
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      testRunConfig = TestRunConfig(isRecording = false, deviceName = "test"),
      imageComparator = SimpleImageComparator(),
    )

    activityScenarioRule.scenario.onActivity {
      val image = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
      with(Canvas(image)) {
        drawColor(Color.BLACK)
      }

      dropshots.assertSnapshot(
        bitmap = image,
        name = "50x50",
        filePath = "static"
      )
    }
  }

  @Test
  fun testFailsWhenValidatorFails() {
    val dropshots = Dropshots(
      resultValidator = FakeResultValidator { false },
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      testRunConfig = TestRunConfig(isRecording = false, deviceName = "test"),
      imageComparator = SimpleImageComparator(),
    )

    var caughtError: AssertionError? = null
    activityScenarioRule.scenario.onActivity {
      val image = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
      with(Canvas(image)) {
        drawColor(Color.BLACK)
      }

      try {
        dropshots.assertSnapshot(
          bitmap = image,
          name = "50x50",
          filePath = "static"
        )
      } catch (e: AssertionError) {
        caughtError = e
      }
    }

    assertNotNull(caughtError)
  }

  @Test
  fun fastFailsForMismatchedSize() {
    val dropshots = Dropshots(
      resultValidator = CountValidator(0),
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      testRunConfig = TestRunConfig(isRecording = false, deviceName = "test"),
      imageComparator = SimpleImageComparator(),
    )

    var caughtError: AssertionError? = null
    activityScenarioRule.scenario.onActivity {
      val image = Bitmap.createBitmap(50, 60, Bitmap.Config.ARGB_8888)
      with(Canvas(image)) {
        drawColor(Color.BLACK)
      }

      try {
        dropshots.assertSnapshot(
          bitmap = image,
          name = "50x50",
          filePath = "static",
        )
      } catch (e: AssertionError) {
        caughtError = e
      }
    }

    assertNotNull("Mismatched size screenshot test expected to fail, but passed.", caughtError)
  }
}

