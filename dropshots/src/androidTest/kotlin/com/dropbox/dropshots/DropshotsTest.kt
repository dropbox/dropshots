package com.dropbox.dropshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
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
  private lateinit var imageDirectory: File

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
    imageDirectory =
      File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        "screenshots/test-${testName.methodName}",
      )
    fakeValidator.validator = CountValidator(0)
    activityScenarioRule.scenario.onActivity { activity ->
      activity.setContentView(
        LinearLayout(activity).apply {
          orientation = LinearLayout.VERTICAL

          listOf(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN).forEach {
            addView(
              View(activity).apply {
                setBackgroundColor(it)
                layoutParams = LinearLayout.LayoutParams(
                  ViewGroup.MarginLayoutParams.MATCH_PARENT,
                  ViewGroup.MarginLayoutParams.WRAP_CONTENT,
                  1f
                )
              }
            )
          }
        }
      )
    }
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
  fun testMatchesFullScreenshot2() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot("MatchesFullScreenshot")
    }
  }

  @Test
  fun testMatchesFullScreenshot3() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot("MatchesFullScreenshot")
    }
  }

  @Test
  fun testMatchesFullScreenshot4() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot("MatchesFullScreenshot")
    }
  }

  @Test
  fun testMatchesFullScreenshot5() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot("MatchesFullScreenshot")
    }
  }

  @Test
  fun testMatchesFullScreenshot6() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot("MatchesFullScreenshot")
    }
  }

  @Test
  fun testMatchesFullScreenshot7() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot("MatchesFullScreenshot")
    }
  }

  @Test
  fun testMatchesFullScreenshot8() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot("MatchesFullScreenshot")
    }
  }

  @Test
  fun testMatchesFullScreenshot9() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot("MatchesFullScreenshot")
    }
  }

  @Test
  fun testMatchesFullScreenshot10() {
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
      rootScreenshotDirectory = imageDirectory,
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
      rootScreenshotDirectory = imageDirectory,
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

    with(File(imageDirectory, "diff")) {
      assertTrue(exists())
      assertArrayEquals(arrayOf(File(this, "MatchesViewScreenshot.png")), listFiles())
    }
  }

  @Test
  fun testFailsForDifferences() {
    val dropshots = Dropshots(
      resultValidator = CountValidator(0),
      rootScreenshotDirectory = imageDirectory,
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
      rootScreenshotDirectory = imageDirectory,
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
      rootScreenshotDirectory = imageDirectory,
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
      rootScreenshotDirectory = imageDirectory,
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

