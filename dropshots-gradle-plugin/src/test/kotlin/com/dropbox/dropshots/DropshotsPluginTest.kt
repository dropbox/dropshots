package com.dropbox.dropshots

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DropshotsPluginTest {
  private lateinit var gradleRunner: GradleRunner

  @Before
  fun setup() {
    gradleRunner = GradleRunner.create()
      .withPluginClasspath()
  }

  @Test
  fun configurationCache() {
    val fixtureRoot = File("src/test/projects/configuration-cache-compatible")

    gradleRunner
      .withArguments(":module:tasks", "--configuration-cache", "--stacktrace")
      .runFixture(fixtureRoot) { build() }
  }

  @Test
  fun `applies to library plugins applied after plugin`() {
    val fixtureRoot = File("src/test/projects/configuration-cache-compatible")

    val result = gradleRunner
      .withArguments(":module:tasks", "--configuration-cache", "--stacktrace")
      .runFixture(fixtureRoot) { build() }
    assertThat(result.output).contains("updateDebugScreenshots")
    assertThat(result.output).contains("pullDebugScreenshots")
  }

  private fun GradleRunner.runFixture(
    projectRoot: File,
    action: GradleRunner.() -> BuildResult,
  ): BuildResult {
    val settings = File(projectRoot, "settings.gradle")
    if (!settings.exists()) {
      settings.createNewFile()
      settings.deleteOnExit()
    }

    return withProjectDir(projectRoot).action()
  }
}
