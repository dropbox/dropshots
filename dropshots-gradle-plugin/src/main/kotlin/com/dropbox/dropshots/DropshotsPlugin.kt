package com.dropbox.dropshots

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.tasks.AndroidTestTask
import com.dropbox.dropshots.DropshotsExtension.Companion.EXTENSION_NAME
import java.nio.file.Files
import java.util.Locale
import org.gradle.api.Plugin
import org.gradle.api.Project

private const val recordScreenshotsArg = "dropshots.record"

public class DropshotsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    setupExtension(project)

    project.pluginManager.withPlugin("com.android.application") {
      val extension = project.extensions.findByType(AppExtension::class.java)
        ?: throw Exception("Failed to find Android Application extension")
      project.configureDropshots(extension)
    }

    project.pluginManager.withPlugin("com.android.library") {
      val extension = project.extensions.findByType(LibraryExtension::class.java)
        ?: throw Exception("Failed to find Android Library extension")
      project.configureDropshots(extension)
    }
  }

  private fun setupExtension(project: Project) {
    project.extensions.create(EXTENSION_NAME, DropshotsExtension::class.java)
  }

  private fun Project.configureDropshots(extension: TestedExtension) {
    val isRecordingScreenshots = hasProperty(recordScreenshotsArg)

    extension.buildTypes.getByName("debug") {
      it.resValue("bool", "is_recording_screenshots", isRecordingScreenshots.toString())
    }

    project.afterEvaluate {
      it.dependencies.add(
        "androidTestImplementation",
        "com.dropbox.dropshots:dropshots:$VERSION"
      )
    }

    val androidTestSourceSet = extension.sourceSets.findByName("androidTest")
      ?: throw Exception("Failed to find androidTest source set")

    val referenceScreenshotDirectory = layout.projectDirectory.dir(
      project.extensions.getByType(DropshotsExtension::class.java).referenceDirectory
    )
    val referenceScreenshotDirectoryPath = referenceScreenshotDirectory.asFile.toPath()
    if (Files.exists(referenceScreenshotDirectoryPath)) {
      Files.createDirectory(referenceScreenshotDirectoryPath)
    }

    androidTestSourceSet.assets {
      srcDirs(referenceScreenshotDirectory)
    }

    val adbExecutablePath = provider { extension.adbExecutable.path }
    extension.testVariants.all { variant ->
      val testTaskProvider = variant.connectedInstrumentTestProvider

      val screenshotDir = provider {
        val appId = if (variant.testedVariant is ApkVariant) {
          variant.testedVariant.applicationId
        } else {
          variant.packageApplicationProvider.get().applicationId
          variant.applicationId
        }
        "/storage/emulated/0/Download/screenshots/$appId"
      }

      val clearScreenshotsTask = tasks.register(
        "clear${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}Screenshots",
        ClearScreenshotsTask::class.java,
      ) {
        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir)
      }

      val pullScreenshotsTask = tasks.register(
        "pull${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}Screenshots",
        PullScreenshotsTask::class.java,
      ) {
        it.onlyIf { !isRecordingScreenshots }
        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir)
        it.outputDirectory.set(testTaskProvider.flatMap { (it as AndroidTestTask).resultsDir })
        it.finalizedBy(clearScreenshotsTask)
      }

      val updateScreenshotsTask = tasks.register(
        "update${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}Screenshots",
        PullScreenshotsTask::class.java,
      ) {
        it.description = "Updates the local reference screenshots"

        it.onlyIf { isRecordingScreenshots }
        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir)
        it.outputDirectory.set(referenceScreenshotDirectory)
        it.dependsOn(testTaskProvider)
        it.finalizedBy(clearScreenshotsTask)
      }

      testTaskProvider.configure {
        it.finalizedBy(pullScreenshotsTask, updateScreenshotsTask)
      }
    }
  }

  private fun Project.getAndroidExtension(): TestedExtension {
    return when {
      plugins.hasPlugin("com.android.application") -> extensions.findByType(AppExtension::class.java)!!
      plugins.hasPlugin("com.android.library") -> extensions.findByType(LibraryExtension::class.java)!!
      else -> throw IllegalArgumentException("Dropshots can only be applied to an Android project.")
    }
  }
}
