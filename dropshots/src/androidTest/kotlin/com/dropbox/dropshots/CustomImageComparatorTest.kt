package com.dropbox.dropshots

import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.dropbox.differ.Image
import com.dropbox.differ.ImageComparator
import com.dropbox.differ.ImageComparator.ComparisonResult
import com.dropbox.differ.Mask
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CustomImageComparatorTest {

  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

  val comparator = FakeImageComparator()

  @get:Rule
  val dropshots = Dropshots(recordScreenshots = false, imageComparator = comparator)

  @Before
  fun setup() {
    activityScenarioRule.scenario.onActivity {
      it.supportFragmentManager.beginTransaction()
        .add(android.R.id.content, ScreenshotTestFragment())
        .commitNow()
    }
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
  override fun compare(left: Image, right: Image, mask: Mask?): ComparisonResult =
    compareFunc(left, right, mask)
}
