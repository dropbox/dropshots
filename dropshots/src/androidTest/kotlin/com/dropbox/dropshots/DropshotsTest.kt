package com.dropbox.dropshots

import android.Manifest
import android.view.View
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DropshotsTest {

  @get:Rule
  val permissionRule = GrantPermissionRule
    .grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

  @get:Rule
  val dropshots = Dropshots()

  @Before
  fun setup() {
    activityScenarioRule.scenario.onActivity {
      it.supportFragmentManager.beginTransaction()
        .add(android.R.id.content, ScreenshotTestFragment())
        .commitNow()
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
  fun testFailsForDifferences() {
    activityScenarioRule.scenario.onActivity {
      try {
        dropshots.assertSnapshot(
          it.findViewById<View>(android.R.id.content),
          name = "MatchesViewScreenshotBad")
        fail("Expected error when screenshots differ.")
      } catch (e: AssertionError) {
        // pass
      }
    }
  }

  @Test
  fun writesOutputImageOnFailure() {
    activityScenarioRule.scenario.onActivity {
      try {
        dropshots.assertSnapshot(
          view = it.findViewById(android.R.id.content),
          name = "MatchesViewScreenshotBad"
        )
        fail("Expected error when screenshots differ.")
      } catch (e: AssertionError) {
        assertTrue(e.message!!.contains("Output written to: "))
        val path = e.message!!.lines()[1].removePrefix("Output written to: ")
        val outputFile = File(path)
        assertTrue("File expected to exist at: $path", outputFile.exists())
      }
    }
  }
}

