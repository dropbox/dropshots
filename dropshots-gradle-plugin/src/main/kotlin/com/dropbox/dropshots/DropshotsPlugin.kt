package com.dropbox.dropshots

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.internal.tasks.AndroidTestTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized

public class DropshotsPlugin : Plugin<Project> {
  public companion object {
    public const val RECORD_PROPERTY_NAME: String = "recordScreenshots"
  }

  override fun apply(target: Project): Unit = target.run {
    val isRecordingScreenshots = hasProperty(RECORD_PROPERTY_NAME)
    val referenceScreenshotDirectory = project.layout.projectDirectory.dir("screenshots")

    val androidExtension = getAndroidExtension()
    androidExtension.buildTypes.getByName("debug") {
      it.resValue("bool", "is_recording_screenshots", isRecordingScreenshots.toString())
    }

    project.afterEvaluate {
      it.dependencies.add(
        "androidTestImplementation",
        "com.dropbox.dropshots:dropshots:$VERSION"
      )
    }

    val androidTestSourceSet = androidExtension.sourceSets.findByName("androidTest")
    requireNotNull(androidTestSourceSet) {
      "Failed to find androidTest source set."
    }
    androidTestSourceSet.assets {
      srcDirs(referenceScreenshotDirectory)
    }

    val adbExecutablePath = provider { androidExtension.adbExecutable.path }
    androidExtension.testVariants.all { variant ->
      val testTaskProvider = variant.connectedInstrumentTestProvider
      val screenshotDir = provider { "/storage/emulated/0/screenshots/${variant.testedVariant.applicationId}" }

      val clearScreenshotsTask = tasks.register(
        "clear${variant.name.capitalized()}Screenshots",
        ClearScreenshotsTask::class.java,
      ) {
        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir)
      }

      val pullScreenshotsTask = tasks.register(
        "pull${variant.name.capitalized()}Screenshots",
        PullScreenshotsTask::class.java,
      ) {
        it.onlyIf { !isRecordingScreenshots }
        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir)
        it.outputDirectory.set(testTaskProvider.flatMap { (it as AndroidTestTask).resultsDir })
        it.finalizedBy(clearScreenshotsTask)
      }

      val updateScreenshotsTask = tasks.register(
        "update${variant.name.capitalized()}Screenshots",
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
