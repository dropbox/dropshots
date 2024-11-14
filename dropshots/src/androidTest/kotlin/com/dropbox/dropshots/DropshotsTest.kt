package com.dropbox.dropshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.view.View
import androidx.core.net.toFile
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.io.PlatformTestStorage
import androidx.test.platform.io.PlatformTestStorageRegistry
import com.dropbox.differ.SimpleImageComparator
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
  private val isRecordingScreenshots = isRecordingScreenshots(defaultRootScreenshotDirectory())
  private lateinit var testStorage: FileTestStorage

  @get:Rule val testName = TestName()
  @get:Rule
  val dropshots = Dropshots(
    filenameFunc = filenameFunc,
    recordScreenshots = isRecordingScreenshots,
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
  fun testWritesReferenceImageForMissingImages() {
    val dropshots = Dropshots(
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      recordScreenshots = true,
      resultValidator = { false },
      imageComparator = SimpleImageComparator(),
    )

    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(it, "MatchesViewScreenshotBad")
    }

    with(File(testStorage.outputDir, "reference")) {
      assertTrue(exists())
      assertArrayEquals(arrayOf(File(this, "MatchesViewScreenshotBad.png")), listFiles())
    }
  }

  @Test
  fun testWritesDiffImageOnFailureWhenRecording() {
    val dropshots = Dropshots(
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      recordScreenshots = false,
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

    with(File(testStorage.inputDir, "diff")) {
      assertTrue(exists())
      assertArrayEquals(arrayOf(File(this, "MatchesViewScreenshot.png")), listFiles())
    }
  }

  @Test
  fun testFailsForDifferences() {
    val dropshots = Dropshots(
      resultValidator = CountValidator(0),
      testStorage = testStorage,
      filenameFunc = filenameFunc,
      recordScreenshots = false,
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
      recordScreenshots = false,
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
      recordScreenshots = false,
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
      recordScreenshots = false,
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
          filePath = "static"
        )
      } catch (e: AssertionError) {
        caughtError = e
      }
    }

    assertNotNull("Mismatched size screenshot test expected to fail, but passed.", caughtError)
  }
}

