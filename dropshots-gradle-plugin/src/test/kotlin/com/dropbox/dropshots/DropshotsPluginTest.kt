package com.dropbox.dropshots

import com.dropbox.dropshots.rules.TestProjectRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class DropshotsPluginTest {

  @get:Rule val testProject = TestProjectRule("connected-device")

  @Test
  fun configurationCache() {
    // first build
    testProject.execute("tasks", "--configuration-cache")

    // Cached build
    val result = testProject.execute("tasks", "--configuration-cache", "--stacktrace")
    assertThat(result.output).contains("Reusing configuration cache.")
  }

  @Test
  fun `applies to library plugins applied after plugin`() {
    val result = testProject
      .withBuildScript(
        // language=groovy
        """
          plugins {
            id("com.dropbox.dropshots")
            alias(libs.plugins.android.library)
          }

          android {
            namespace = "com.dropbox.dropshots.test.library"
            compileSdk = 35
            defaultConfig.minSdk = 24
          }

          repositories {
            mavenCentral()
            google()
          }
        """.trimIndent()
      )
      .execute("tasks")
    assertThat(result.output).contains("updateConnectedDebugAndroidTestScreenshots")
    assertThat(result.output).contains("pullConnectedDebugAndroidTestScreenshots")
  }
}
