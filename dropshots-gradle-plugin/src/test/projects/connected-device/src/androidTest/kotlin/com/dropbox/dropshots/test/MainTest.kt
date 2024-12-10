package com.dropbox.dropshots.test

import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.dropshots.Dropshots
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Currently recorded screenshots are done with a pixel_5-31
 */
@RunWith(AndroidJUnit4::class)
class MainTest {

  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

  @get:Rule
  val dropshots = Dropshots()

  @Test
  fun basicActivityView() {
    activityScenarioRule.scenario.onActivity {
      dropshots.assertSnapshot(it)
    }
  }

  @Test
  fun testColors() {
    activityScenarioRule.scenario.onActivity {
      val purpleView = it.findViewById<View>(R.id.purpleView)
      dropshots.assertSnapshot(
        view = purpleView,
        name = "purple",
        filePath = "views/colors"
      )

      val redView = it.findViewById<View>(R.id.redView)
      dropshots.assertSnapshot(
        view = redView,
        name = "red",
        filePath = "views/colors"
      )
    }
  }
}
