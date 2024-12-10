package com.dropbox.dropshots.rules

import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.rules.ExternalResource
import org.junit.rules.TemporaryFolder

class TestProjectRule(
  val name: String,
) : ExternalResource() {

  private val tempFolder = TemporaryFolder()
  lateinit var buildFile: File
    private set
  lateinit var gradleRunner: GradleRunner
    private set

  override fun before() {
    tempFolder.create()
    createProject()
  }

  override fun after() {
    tempFolder.delete()
  }

  private fun createProject() {
    val rootProjectDir = File("..").absolutePath
    val versionsFilePath = File("../gradle/libs.versions.toml").absolutePath

    // Setup project directory
    val projectDir = tempFolder.newFolder().apply { mkdir() }
    projectDir.resolve("gradle.properties").writeText(
      """
        org.gradle.jvmargs=-Xmx1g
        android.useAndroidX=true
      """.trimIndent())

    projectDir.resolve("settings.gradle").writeText(
      // language=groovy
      """
        pluginManagement {
          repositories {
            gradlePluginPortal()
            mavenCentral()
            google()
          }
          includeBuild("$rootProjectDir")
        }

        rootProject.name = "test-project"
        include(":module")

        includeBuild("$rootProjectDir") {
          dependencySubstitution {
            substitute(module("com.dropbox.dropshots:dropshots")).using(project(":dropshots"))
            substitute(module("com.dropbox.dropshots:model")).using(project(":model"))
          }
        }

        dependencyResolutionManagement {
          versionCatalogs {
            libs {
              from(files("$versionsFilePath"))
            }
          }

          repositories {
            mavenCentral()
            google()
          }
        }
      """.trimIndent()
    )

    projectDir.resolve("build.gradle").writeText(
      // language=groovy
      """
        plugins {
          alias(libs.plugins.android.application) apply false
          alias(libs.plugins.android.library) apply false
          alias(libs.plugins.kotlin.jvm) apply false
          alias(libs.plugins.kotlin.android) apply false
          alias(libs.plugins.kotlinx.serialization) apply false
        }
      """.trimIndent()
    )

    val moduleDir = projectDir.resolve("module")
    moduleDir.mkdirs()

    @OptIn(ExperimentalPathApi::class)
    File("src/test/projects/$name").toPath()
      .copyToRecursively(
        moduleDir.toPath(),
        followLinks = true,
        overwrite = true,
      )

    buildFile = projectDir.resolve("module/build.gradle")

    gradleRunner = GradleRunner.create()
      .withProjectDir(projectDir)
  }

  fun execute(vararg tasks: String): BuildResult =
    gradleRunner.withArguments(*tasks).build()

  fun withBuildScript(buildScript: String): TestProjectRule {
    buildFile.writeText(buildScript)
    return this
  }
}
