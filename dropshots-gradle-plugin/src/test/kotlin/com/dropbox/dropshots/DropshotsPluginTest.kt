package com.dropbox.dropshots

import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class DropshotsPluginTest {
  private lateinit var gradleRunner: GradleRunner

  @Before
  fun setup() {
    gradleRunner = GradleRunner.create()
      .withPluginClasspath()
  }

  @Test
  @Ignore("Working out the compileOnly testkit classpath.")
  fun configurationCache() {
    val fixtureRoot = File("src/test/projects/configuration-cache-compatible")

    gradleRunner
      .withArguments(":module:tasks", "--configuration-cache", "--stacktrace")
      .runFixture(fixtureRoot) { build() }
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
