package com.dropbox.dropshots

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.Variant
import com.android.build.api.variant.impl.capitalizeFirstChar
import kotlin.jvm.java
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

private const val recordScreenshotsArg = "dropshots.record"

public class DropshotsPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val dropshotsExtension: DropshotsExtension =
      project.extensions.create("dropshots", DropshotsExtension::class.java)

    project.pluginManager.withPlugin("com.android.application") {
      project.extensions.getByType(ApplicationExtension::class.java).apply {
        sourceSets.addDropshotsAssetsDir(dropshotsExtension)
      }
      project.configureDropshots(dropshotsExtension)
    }

    project.pluginManager.withPlugin("com.android.library") {
      project.extensions.getByType(LibraryExtension::class.java).apply {
        sourceSets.addDropshotsAssetsDir(dropshotsExtension)
      }
      project.configureDropshots(dropshotsExtension)
    }
  }

  private fun NamedDomainObjectContainer<out AndroidSourceSet>.addDropshotsAssetsDir(
    dropshotsExtension: DropshotsExtension
  ) {
    named("androidTest") {
      it.assets.directories += dropshotsExtension.referenceOutputDirectory.get()
    }
  }

  private fun Project.configureDropshots(
    dropshotsExtension: DropshotsExtension
  ) {
    val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
    val adbExecutablePath = androidComponents.sdkComponents.adb.map { it.asFile.path }

    androidComponents.onVariants {
      if (it is HasAndroidTest) {
        configureVariant(it, adbExecutablePath, dropshotsExtension)
      }
    }

    afterEvaluate {
      dependencies.add(
        "androidTestImplementation",
        "com.dropbox.dropshots:dropshots:$VERSION"
      )
    }
  }

  private fun <V> Project.configureVariant(
    variant: V,
    adbExecutablePath: Provider<String>,
    dropshotsExtension: DropshotsExtension
  ) where V : HasAndroidTest, V : Variant {
    val androidTest = variant.androidTest ?: return

    val androidTestVariantSlug = androidTest.name.capitalizeFirstChar()

    val deviceScreenshotDir =
      androidTest.applicationId.map { "/storage/emulated/0/Download/screenshots/$it" }
    val buildScreenshotDir =
      layout.buildDirectory.dir("test-results/dropshots/$androidTestVariantSlug")
    val testTaskName = "connected${variant.name.capitalizeFirstChar()}AndroidTest"

    val clearScreenshotsTask = tasks.register(
      "clear${androidTestVariantSlug}Screenshots",
      ClearScreenshotsTask::class.java,
    ) {
      it.adbExecutable.set(adbExecutablePath)
      it.screenshotDir.set(deviceScreenshotDir)
    }

    val isRecordingScreenshots = project.objects.property(Boolean::class.java)

    val canRecordScreenshots = dropshotsExtension.recordOnFailure.map {
      project.state.failure != null || it
    }

    val pullScreenshotsTask = tasks.register(
      "pull${androidTestVariantSlug}Screenshots",
      PullScreenshotsTask::class.java,
    ) { task ->
      task.adbExecutable.set(adbExecutablePath)
      task.screenshotDir.set(deviceScreenshotDir)
      task.outputDirectory.set(buildScreenshotDir)
      task.shouldWriteReferences.set(isRecordingScreenshots.map { it && (canRecordScreenshots.get()) })
      task.referenceOutputDirectory.set(dropshotsExtension.referenceOutputDirectory)
    }

    val recordScreenshotsTask = tasks.register(
      "record${androidTestVariantSlug}Screenshots",
    ) { task ->
      task.group = "verification"
      task.description = "Indicates that local reference screenshots should be updated"
      // This task being present on the task graph is used as an indicator that we should update
      // source screenshots. The actual work is performed in pullScreenshotsTask since it is
      // a finalizer task.
      task.dependsOn(testTaskName)
    }

    if (providers.gradleProperty(recordScreenshotsArg).isPresent) {
      project.logger.warn(
        "The 'dropshots.record' property has been deprecated and will " +
          "be removed in a future version."
      )
      isRecordingScreenshots.set(true)
    }
    project.gradle.taskGraph.whenReady { graph ->
      isRecordingScreenshots.set(recordScreenshotsTask.map { graph.hasTask(it) })
    }

    val writeMarkerFileTask = tasks.register(
      "push${androidTestVariantSlug}ScreenshotMarkerFile",
      PushFileTask::class.java,
    ) { task ->
      task.onlyIf { isRecordingScreenshots.get() }
      task.adbExecutable.set(adbExecutablePath)
      task.fileContents.set("\n")
      task.remotePath.set(deviceScreenshotDir.map { "$it/.isRecordingScreenshots" })
      task.dependsOn(clearScreenshotsTask)
    }

    tasks.named { it == testTaskName }.configureEach {
      it.finalizedBy(pullScreenshotsTask)
      it.dependsOn(writeMarkerFileTask)
    }
  }
}
