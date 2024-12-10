package com.dropbox.dropshots

import com.dropbox.dropshots.rules.TestProjectRule
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test

class ConnectedDeviceTest {

  @get:Rule val testProject = TestProjectRule("connected-device")

  @Test
  fun `executes marker file push when record task is run`() {
    val result = testProject.execute("updateConnectedDebugAndroidTestScreenshots")
    with(result.task(":module:writeConnectedDebugAndroidTestScreenshotConfigFile")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

  @Test
  fun `executes marker file push when test task is run`() {
    val result = testProject.execute("connectedDebugAndroidTest")
    with(result.task(":module:writeConnectedDebugAndroidTestScreenshotConfigFile")) {
      assert(this != null) {
        "Expected :module:writeConnectedDebugAndroidTestScreenshotConfigFile in task list.\n${result.output}"
      }
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

  @Test
  fun `writes correct config while testing`() {
    val result = testProject.execute(
      "connectedDebugAndroidTest",
      "-Pandroid.testInstrumentationRunnerArguments.dropshots.test.testConfig=true",
      "-Pandroid.testInstrumentationRunnerArguments.class=com.dropbox.dropshots.test.TestRunConfig",
    )
    assertThat(result.task(":module:connectedDebugAndroidTest")?.outcome)
      .isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun `writes correct config while recording`() {
    val result = testProject.execute(
      "updateConnectedDebugAndroidTestScreenshots",
      "-Pandroid.testInstrumentationRunnerArguments.dropshots.test.testConfig=true",
      "-Pandroid.testInstrumentationRunnerArguments.dropshots.test.expectRecording=true",
      "-Pandroid.testInstrumentationRunnerArguments.class=com.dropbox.dropshots.test.TestRunConfig",
    )
    assertThat(result.task(":module:connectedDebugAndroidTest")?.outcome)
      .isEqualTo(TaskOutcome.SUCCESS)
  }
}
