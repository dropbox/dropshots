package com.dropbox.dropshots.sample

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dropbox.dropshots.Dropshots
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

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
}
