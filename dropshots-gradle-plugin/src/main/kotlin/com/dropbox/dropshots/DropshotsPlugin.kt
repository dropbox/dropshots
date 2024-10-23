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

public class DropshotsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
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

  private fun Project.configureDropshots(extension: TestedExtension) {

    project.afterEvaluate {
      it.dependencies.add(
        "androidTestImplementation",
        "com.dropbox.dropshots:dropshots:$VERSION"
      )
    }

    //check this to have resource based on flavours
    val androidTestSourceSet = extension.sourceSets.findByName("androidTest")
      ?: throw Exception("Failed to find androidTest source set")

    // TODO configure this via extension
    val referenceScreenshotDirectory = layout.projectDirectory.dir("src/androidTest/screenshots")

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

      val pullScreenshotsTask = tasks.register(
        "pull${variantSlug}Screenshots",
        PullScreenshotsTask::class.java,
      ) {
        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir.map { base -> "$base/diff" })
        it.outputDirectory.set(testTaskProvider.flatMap { (it as AndroidTestTask).resultsDir })
        it.finalizedBy(clearScreenshotsTask)
      }

      val updateScreenshotsTask = tasks.register(
        "update${variantSlug}Screenshots",
        PullScreenshotsTask::class.java,
      ) {
        it.description = "Updates the local reference screenshots"

        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir.map { base -> "$base/reference" })
        it.outputDirectory.set(referenceScreenshotDirectory)
        it.dependsOn(testTaskProvider)
        it.finalizedBy(clearScreenshotsTask)
      }

      val isUpdatingScreenshots = project.objects.property(Boolean::class.java)
      project.gradle.taskGraph.whenReady { graph ->
        isUpdatingScreenshots.set(updateScreenshotsTask.map { graph.hasTask(it) })
      }

      val writeMarkerFileTask = tasks.register(
        "push${variantSlug}ScreenshotMarkerFile",
        PushFileTask::class.java,
      ) {
        it.onlyIf { isUpdatingScreenshots.get() }
        it.adbExecutable.set(adbExecutablePath)
        it.fileContents.set("\n")
        it.remotePath.set(screenshotDir.map { dir -> "$dir/.isUpdatingScreenshots" })
        it.finalizedBy(clearScreenshotsTask)
      }
      testTaskProvider.dependsOn(writeMarkerFileTask)

      testTaskProvider.configure {
        it.finalizedBy(pullScreenshotsTask)
      }
    }
  }
}
