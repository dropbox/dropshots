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

  @get:Rule val tmpFolder = TemporaryFolder()

  private lateinit var buildFile: File
  private lateinit var gradleRunner: GradleRunner

  @Before
  fun setup() {
    val localMavenRepo = File("../build/localMaven").absolutePath
    val versionsFilePath = File("../gradle/libs.versions.toml").absolutePath

    // Setup project directory
    val projectDir = tmpFolder.newFolder().apply { mkdir() }
    projectDir.resolve("gradle.properties").writeText("android.useAndroidX=true")

    projectDir.resolve("settings.gradle").writeText(
      // language=groovy
      """
        pluginManagement {
          repositories {
            gradlePluginPortal()
            mavenCentral()
            google()
          }
        }

        dependencyResolutionManagement {
          versionCatalogs {
            libs {
              from(files("$versionsFilePath"))
            }
          }

          repositories {
            maven {
              url("$localMavenRepo")
            }
            mavenCentral()
            google()
          }
        }
      """.trimIndent()
    )

    buildFile = projectDir.resolve("build.gradle")
    buildFile.writeText(
      // language=groovy
      """
        plugins {
          alias(libs.plugins.android.library)
          id("com.dropbox.dropshots")
        }

        android {
          namespace = "com.dropbox.dropshots.test.library"
          compileSdk = 35

          defaultConfig.minSdk = 24
        }
      """.trimIndent()
    )

    gradleRunner = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDir)
  }

  @Test
  fun configurationCache() {
    // first build
    gradleRunner.withArguments("tasks", "--configuration-cache").build()

    // Cached build
    val result = gradleRunner
      .withArguments("tasks", "--configuration-cache", "--stacktrace")
      .build()
    assertThat(result.output).contains("Reusing configuration cache.")
  }

  @Test
  fun `applies to library plugins applied after plugin`() {
    val result = gradleRunner
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
      .withArguments("tasks")
      .build()
    assertThat(result.output).contains("recordDebugAndroidTestScreenshots")
    assertThat(result.output).contains("pullDebugAndroidTestScreenshots")
  }

  @Test
  fun `executes marker file push only when record task is run`() {
    val result = gradleRunner
      .withArguments("recordDebugAndroidTestScreenshots")
      .build()
    with(result.task(":pushDebugAndroidTestScreenshotMarkerFile")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
  }

  @Test
  fun `skips marker file push only when record task is run`() {
    val result = gradleRunner
      .withArguments("connectedDebugAndroidTest")
      .build()
    with(result.task(":pushDebugAndroidTestScreenshotMarkerFile")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.SKIPPED)
    }
  }

  @Test
  fun `specifying output directory in extension returns src_test_screenshots`() {
    val result = gradleRunner
      .withBuildScript(
        // language=groovy
        """
          plugins {
            id("com.dropbox.dropshots")
            alias(libs.plugins.android.library)
          }

          dropshots {
              referenceOutputDirectory = "src/test"
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

          dropshots {
              referenceOutputDirectory.set("test/example")
          }

          tasks.register("printOutputDirectory") {
              doLast {
                  println(dropshots.referenceOutputDirectory.get())
              }
          }
        """.trimIndent()
      )
      .withArguments("printOutputDirectory")
      .build()
    assertThat(result.output).contains("test/example")
  }

  @Test
  fun `dropshot record shows a deprecated warning`() {
    val result = gradleRunner
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
      .withArguments("tasks", "-Pdropshots.record")
      .build()
    assertThat(result.output).contains("The 'dropshots.record' property has been deprecated and will " +
      "be removed in a future version.")
  }


  private fun GradleRunner.withBuildScript(buildScript: String): GradleRunner {
    buildFile.writeText(buildScript)
    return this
  }
}
