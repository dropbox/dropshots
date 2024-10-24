package com.dropbox.dropshots

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DropshotsPluginTest {

  private val agpVersion = "8.7.1"
  @get:Rule val tmpFolder = TemporaryFolder()

  private lateinit var projectDir: File
  private lateinit var settingsFile: File
  private lateinit var buildFile: File
  private lateinit var gradleRunner: GradleRunner

  @Before
  fun setup() {
    // Setup project directory
    projectDir = tmpFolder.newFolder().apply { mkdir() }
    File(projectDir, "gradle.properties").writeText("android.useAndroidX=true")
    // language=groovy
    settingsFile = File(projectDir, "settings.gradle").apply {
      writeText(
        """
          pluginManagement {
            repositories {
              gradlePluginPortal()
              mavenCentral()
              google()
            }
          }
        """.trimIndent()
      )
    }
    // language=groovy
    buildFile = File(projectDir, "build.gradle").apply {
      writeText(
        """
          plugins {
            id("com.android.library") version "$agpVersion"
            id("com.dropbox.dropshots")
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
    }

    gradleRunner = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDir)
  }

  @Test
  fun configurationCache() {
    gradleRunner
      .withArguments("tasks", "--configuration-cache", "--stacktrace")
      .build()
  }

  @Test
  fun `applies to library plugins applied after plugin`() {
    val result = gradleRunner
      .withBuildScript(
        // language = groovy
        """
          plugins {
            id("com.dropbox.dropshots")
            id("com.android.library") version "$agpVersion"
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
      .withArguments("tasks", "--configuration-cache", "--stacktrace")
      .build()
    assertThat(result.output).contains("recordDebugAndroidTestScreenshots")
    assertThat(result.output).contains("pullDebugAndroidTestScreenshots")
  }

  @Test
  fun `executes marker file push only when record task is run`() {
    val result = gradleRunner
      .withArguments("recordDebugAndroidTestScreenshots", "--stacktrace")
      .build()
    with(result.task(":pushDebugAndroidTestScreenshotMarkerFile")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

  @Test
  fun `skips marker file push only when record task is run`() {
    val result = gradleRunner
      .withArguments("connectedDebugAndroidTest", "--stacktrace")
      .build()
    with(result.task(":pushDebugAndroidTestScreenshotMarkerFile")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.SKIPPED)
    }
  }

  private fun GradleRunner.withBuildScript(buildScript: String): GradleRunner {
    buildFile.writeText(buildScript)
    return this
  }
}
