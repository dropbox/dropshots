package com.dropbox.dropshots

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.tasks.AndroidTestTask
import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.util.Locale
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

private const val recordScreenshotsArg = "dropshots.record"

public class DropshotsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val dropshotsExtension = project.extensions.create("dropshots", DropshotsExtension::class.java)

    project.pluginManager.withPlugin("com.android.application") {
      val extension = project.extensions.findByType(AppExtension::class.java)
        ?: throw Exception("Failed to find Android Application extension")
      project.configureDropshots(dropshotsExtension, extension)
    }

    project.pluginManager.withPlugin("com.android.library") {
      val extension = project.extensions.findByType(LibraryExtension::class.java)
        ?: throw Exception("Failed to find Android Library extension")
      project.configureDropshots(dropshotsExtension, extension)
    }
  }

  private fun Project.configureDropshots(
    dropshotsExtension: DropshotsExtension,
    extension: TestedExtension
  ) {
    project.afterEvaluate {
      it.dependencies.add(
        "androidTestImplementation",
        "com.dropbox.dropshots:dropshots:$VERSION"
      )
    }

    //check this to have resource based on flavours
    val androidTestSourceSet = extension.sourceSets.findByName("androidTest")
      ?: throw Exception("Failed to find androidTest source set")

    val referenceScreenshotDirectory = layout.projectDirectory.dir(dropshotsExtension.referenceOutputDirectory)

    androidTestSourceSet.assets {
      srcDirs(referenceScreenshotDirectory)
    }

    val adbExecutablePath = provider { extension.adbExecutable.path }
    extension.testVariants.all { variant ->
      val testTaskProvider = variant.connectedInstrumentTestProvider
      val variantSlug = variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

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
        "clear${variantSlug}Screenshots",
        ClearScreenshotsTask::class.java,
      ) {
        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir)
      }

      val pullScreenshotTask = tasks.register(
        "pull${variantSlug}Screenshots",
        PullScreenshotsTask::class.java,
      ) {
        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir)
        it.outputDirectory.set(testTaskProvider.flatMap { (it as AndroidTestTask).resultsDir })
        it.mustRunAfter(testTaskProvider)
        it.finalizedBy(clearScreenshotsTask)
      }

      val recordScreenshotsTask = tasks.register(
        "record${variantSlug}Screenshots",
        Copy::class.java,
      ) {
        it.description = "Updates the local reference screenshots"
        it.from(
          testTaskProvider.flatMap {
            (it as AndroidTestTask).resultsDir.map { base -> "$base/reference" }
          }
        )
        it.into(referenceScreenshotDirectory)
        it.dependsOn(testTaskProvider, pullScreenshotTask)
        it.finalizedBy(clearScreenshotsTask)
      }

      val isRecordingScreenshots = project.objects.property(Boolean::class.java)
      if (hasProperty(recordScreenshotsArg)) {
        project.logger.warn("The 'dropshots.record' property has been deprecated and will " +
          "be removed in a future version.")
        isRecordingScreenshots.set(true)
      }
      project.gradle.taskGraph.whenReady { graph ->
        isRecordingScreenshots.set(recordScreenshotsTask.map { graph.hasTask(it) })
      }

      val writeMarkerFileTask = tasks.register(
        "push${variantSlug}ScreenshotMarkerFile",
        PushFileTask::class.java,
      ) {
        it.onlyIf { isRecordingScreenshots.get() }
        it.adbExecutable.set(adbExecutablePath)
        it.fileContents.set("\n")
        it.remotePath.set(screenshotDir.map { dir -> "$dir/.isRecordingScreenshots" })
        it.finalizedBy(clearScreenshotsTask)
      }
      testTaskProvider.dependsOn(writeMarkerFileTask)

      testTaskProvider.configure {
        it.finalizedBy(pullScreenshotTask)
      }
    }
  }
}
