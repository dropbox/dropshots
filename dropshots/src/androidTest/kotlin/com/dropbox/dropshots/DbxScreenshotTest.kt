package com.dropbox.dropshots

import android.Manifest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.GrantPermissionRule
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DbxScreenshotTest {

  @get:Rule
  val permissionRule = GrantPermissionRule
    .grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

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
      DbxScreenshot.snapActivity(it)
        .setName("MatchesActivityScreenshot")
        .assertUnchanged()
    }
  }

  @Test
  fun testMatchesViewScreenshot() {
    activityScenarioRule.scenario.onActivity {
      DbxScreenshot.snap(it.findViewById(android.R.id.content))
        .setName("MatchesViewScreenshot")
        .assertUnchanged()
    }
  }

  @Test
  fun testFailsForDifferences() {
    activityScenarioRule.scenario.onActivity {
      try {
        DbxScreenshot.snap(it.findViewById(android.R.id.content))
          .setName("MatchesViewScreenshotBad")
          .assertUnchanged()
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
        DbxScreenshot.snap(it.findViewById(android.R.id.content))
          .setName("MatchesViewScreenshotBad")
          .assertUnchanged()
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

