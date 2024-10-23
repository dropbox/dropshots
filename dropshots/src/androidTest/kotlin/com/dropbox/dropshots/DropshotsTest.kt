package com.dropbox.dropshots

import android.graphics.Color
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.dropbox.differ.SimpleImageComparator
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DropshotsTest {

  @get:Rule
  val emulatorConfigRule = EmulatorConfigRule()

  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

  private val fakeValidator = FakeResultValidator()
  private var filenameFunc: (String) -> String = { it }
  private val isRecordingScreenshots = InstrumentationRegistry.getInstrumentation()
    .targetContext.resources.getBoolean(R.bool.is_recording_screenshots)
  private lateinit var imageDirectory: File

  @get:Rule
  val dropshots = Dropshots(
    filenameFunc = filenameFunc,
    recordScreenshots = false,
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
        "screenshots/test-${System.currentTimeMillis()}",
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
      Files.walkFileTree(imageDirectory.toPath(), object : FileVisitor<Path> {
        override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
          return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
          requireNotNull(file)
          Files.delete(file)
          return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
          throw exc!!
        }

        override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
          requireNotNull(dir)
          Files.delete(dir)
          return FileVisitResult.CONTINUE
        }
      })
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
        it.findViewById<View>(android.R.id.content),
        name = "MatchesViewScreenshot"
      )
    }
  }

  @Test
  fun testWritesReferenceImageOnFailureWhenRecording() {
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
    assumeFalse(isRecordingScreenshots)

    var failed = false
    activityScenarioRule.scenario.onActivity {
      try {
        Log.d("!!! TEST !!!", "Asserting snapshot...")
        dropshots.assertSnapshot(
          view = it.findViewById(android.R.id.content),
          name = "MatchesViewScreenshotBad",
          filePath = "static"
        )
        Log.d("!!! TEST !!!", "Snapshot asserted")
        failed = true
      } catch (e: AssertionError) {
        Log.d("!!! TEST !!!", "Snapshot assertion failed as expected.")
        // pass
      }
    }

    Log.d("!!! TEST !!!", "Validating thrown error")
    if (failed) {
      fail("Expected error when screenshots differ.")
    }
  }

  @Test
  fun testPassesWhenValidatorPasses() {
    assumeFalse(isRecordingScreenshots)

    fakeValidator.validator = { true }
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(
        view = it.findViewById(android.R.id.content),
        name = "MatchesViewScreenshotBad",
        filePath = "static"
      )
    }
  }

  @Test
  fun testFailsWhenValidatorFails() {
    assumeFalse(isRecordingScreenshots)

    fakeValidator.validator = { false }

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

    assertNotNull(caughtError)
  }

  @Test
  fun fastFailsForMismatchedSize() {
    assumeFalse(isRecordingScreenshots)

    var failed = false
    activityScenarioRule.scenario.onActivity {
      try {
        dropshots.assertSnapshot(
          view = it.findViewById(android.R.id.content),
          name = "MatchesViewScreenshotBadSize",
          filePath = "static"
        )
        failed = true
      } catch (e: Throwable) {
        // no op
      }
    }

    assertFalse("Mismatched size screenshot test expected to fail, but passed.", failed)
  }
}

