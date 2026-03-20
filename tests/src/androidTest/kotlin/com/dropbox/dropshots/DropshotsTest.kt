package com.dropbox.dropshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Environment
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
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
  private var filenameFunc: (String, String) -> String = { _, funcName -> funcName }
  private lateinit var imageDirectory: File

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
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val externalStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    imageDirectory = File(externalStorageDir, "screenshots/${context.packageName}")
      File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "screenshots/test-${testName.methodName}",
      )
    fakeValidator.validator = CountValidator(0)
  }

  @After
  fun after() {
    if (imageDirectory.exists()) {
      imageDirectory.deleteRecursively()
    }
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
        it.findViewById(android.R.id.content),
        name = "MatchesViewScreenshot"
      )
    }
  }

  @Test
  fun testWritesReferenceImageForMissingImages() {
    val dropshots = Dropshots(
      filenameFunc = filenameFunc,
      recordScreenshots = true,
      resultValidator = { false },
      imageComparator = SimpleImageComparator(),
    )

    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(it, "MatchesViewScreenshotBad")
    }

    with(File(imageDirectory, "reference")) {
      assertTrue(exists())
      assertArrayEquals(arrayOf(File(this, "MatchesViewScreenshotBad.png")), listFiles())
    }
  }

  @Test
  fun testWritesDiffImageOnFailureWhenRecording() {
    val dropshots = Dropshots(
      filenameFunc = filenameFunc,
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

    with(File(imageDirectory, "diff")) {
      assertTrue(exists())
      assertArrayEquals(arrayOf(File(this, "MatchesViewScreenshot.png")), listFiles())
    }
  }

  @Test
  fun testFailsForDifferences() {
    val dropshots = Dropshots(
      resultValidator = CountValidator(0),
      filenameFunc = filenameFunc,
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

