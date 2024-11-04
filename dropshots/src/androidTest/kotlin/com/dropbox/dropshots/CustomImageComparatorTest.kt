package com.dropbox.dropshots

import android.os.Environment
import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.dropbox.differ.Image
import com.dropbox.differ.ImageComparator
import com.dropbox.differ.ImageComparator.ComparisonResult
import com.dropbox.differ.Mask
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CustomImageComparatorTest {
  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

  private val imageDirectory = File(
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
    "screenshots/custom-image-comparator-test",
  )
  val comparator = FakeImageComparator()

  @get:Rule
  val dropshots = Dropshots(
    rootScreenshotDirectory = imageDirectory,
    filenameFunc = defaultFilenameFunc,
    recordScreenshots = false,
    imageComparator = comparator,
    resultValidator = CountValidator(0),
  )

  @Before
  fun before() {
    activityScenarioRule.scenario.onActivity {
      it.supportFragmentManager.beginTransaction()
        .add(android.R.id.content, ScreenshotTestFragment())
        .commitNow()
    }
  }

  @After
  fun after() {
    imageDirectory.deleteRecursively()
  }

  @Test
  fun imageComparatorIsConfigurable() {
    val calls = mutableListOf<Triple<Image, Image, Mask?>>()
    comparator.compareFunc = { left, right, mask ->
      calls.add(Triple(left, right, mask))
      ComparisonResult(0, 0, 0, 0)
    }

    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(
        it.findViewById<View>(android.R.id.content),
        name = "MatchesViewScreenshot"
      )
    }

    // Just making sure that the custom comparator was called.
    assert(calls.isNotEmpty())
  }
}

class FakeImageComparator(
  var compareFunc: (Image, Image, Mask?) -> ComparisonResult = { _, _, _ ->
    ComparisonResult(0, 0, 0, 0)
  }
) : ImageComparator {
  override fun compare(left: Image, right: Image, diff: Mask?): ComparisonResult =
    compareFunc(left, right, diff)
}
