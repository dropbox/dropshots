package com.dropbox.dropshots.test

import android.annotation.SuppressLint
import android.net.Uri
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dropbox.dropshots.Dropshots
import com.dropbox.dropshots.configFileName
import com.dropbox.dropshots.model.TestRunConfig
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Currently recorded screenshots are done with a pixel_5-31
 */
@RunWith(AndroidJUnit4::class)
class TestRunConfigTest {

  @get:Rule
  val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

  @get:Rule
  val dropshots = Dropshots()

  private fun shouldExecuteTests() = InstrumentationRegistry.getArguments()
    .getBoolean("dropshots.test.testConfig", false)

  private fun readConfig(): TestRunConfig {
    val targetApplicationId = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    @SuppressLint("SdCardPath")
    val testDataFileUri = Uri.fromFile(File("/sdcard/Android/media/${targetApplicationId}/dropshots/$configFileName"))
    return InstrumentationRegistry.getInstrumentation().context
      .contentResolver
      .openInputStream(testDataFileUri)
      .use { inputStream ->
        requireNotNull(inputStream)
        TestRunConfig.read(inputStream)
      }
  }

  @Test
  fun testConfigContainsConnectedDeviceName() {
    assumeTrue(shouldExecuteTests())

    val config = readConfig()
    assert(config.deviceName == "connected") {
      "Expected config.deviceName to be 'connected', but got '${config.deviceName}'"
    }
  }

  @Test
  fun testConfigContainsIsRecording() {
    assumeTrue(shouldExecuteTests())

    val config = readConfig()
    val expectRecording = InstrumentationRegistry.getArguments()
      .getBoolean("dropshots.test.expectRecording", false)
    assert(config.isRecording == expectRecording) {
      "Expected config.isRecording to be '$expectRecording', but got '${config.isRecording}'"
    }
  }
}
