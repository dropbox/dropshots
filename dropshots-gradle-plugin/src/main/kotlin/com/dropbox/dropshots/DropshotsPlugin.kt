package com.dropbox.dropshots

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.Variant
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.AndroidBasePlugin
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.tasks.AndroidTestTask
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask
import com.android.build.gradle.internal.tasks.ManagedDeviceInstrumentationTestTask
import com.android.build.gradle.internal.tasks.ManagedDeviceTestTask
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.build.gradle.options.BooleanOption
import com.android.build.gradle.options.ProjectOptionService
import com.android.builder.core.ComponentType
import com.dropbox.dropshots.model.TestRunConfig
import java.io.File
import java.util.Locale
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider

private const val recordScreenshotsArg = "dropshots.record"

public class DropshotsPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit = with(project) {
    plugins.withType(AndroidBasePlugin::class.java) { plugin ->

      // Add dropshots dependency
      afterEvaluate {
        it.dependencies.add(
          "androidTestImplementation",
          "com.dropbox.dropshots:dropshots:$VERSION"
        )
      }

      val updateAllTask = tasks.register("updateDropshotsScreenshots") {
        it.description = "Updates screenshots for all variants."
        it.group = JavaBasePlugin.VERIFICATION_GROUP
      }

      val componentsExtension = extensions.getByType(AndroidComponentsExtension::class.java)
      val testedExtension = extensions.getByType(TestedExtension::class.java)

      //check this to have resource based on flavours
      val androidTestSourceSet = testedExtension.sourceSets.findByName("androidTest")
        ?: throw Exception("Failed to find androidTest source set")

      // TODO configure this via extension
      val referenceScreenshotDirectory = layout.projectDirectory.dir("src/androidTest/screenshots")

      androidTestSourceSet.assets {
        srcDirs(referenceScreenshotDirectory)
      }

      @Suppress("UnstableApiUsage")
      componentsExtension.onVariants { variant ->
        if (!variant.debuggable || variant !is HasDeviceTests) {
          return@onVariants
        }

        logger.warn("Found testable variant: ${variant.name}")

        val variantName = variant.name
        val deviceTestComponent = variant.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE] ?: return@onVariants
        val adbProvider = provider { testedExtension.adbExecutable }

        // Create a test connected check task
        addTasksForDeviceProvider(variant, "connected", adbProvider)

        testedExtension.deviceProviders.forEach { deviceProvider ->
          addTasksForDeviceProvider(variant, deviceProvider.name, adbProvider)
        }

        val optionService = ProjectOptionService.RegistrationAction(this).execute().get()
        optionService.projectOptions.get(BooleanOption.ENABLE_ADDITIONAL_ANDROID_TEST_OUTPUT)
      }
    }
  }

  private fun Project.addTasksForDeviceProvider(
    variant: Variant,
    deviceProviderName: String,
    adbProvider: Provider<File>,
  ) {

    tasks
      .named {
        it == "${deviceProviderName}${variant.name.capitalize()}${ComponentType.ANDROID_TEST_SUFFIX}"
      }
      .all { testTask ->
        val testSlug  = testTask.name.capitalize()

        val testDataProvider = when (testTask) {
          is DeviceProviderInstrumentTestTask -> testTask.testData
          is ManagedDeviceInstrumentationTestTask -> testTask.testData
          is ManagedDeviceTestTask -> testTask.testData
          else -> return@all
        }
        val defaultTestOutputDirectory = layout.buildDirectory
          .dir("outputs/androidTest-results/dropshots/${variant.name}/$deviceProviderName/")
        val additionalTestOutputEnabled = when (testTask) {
          is DeviceProviderInstrumentTestTask -> testTask.additionalTestOutputEnabled
          is ManagedDeviceInstrumentationTestTask -> testTask.getAdditionalTestOutputEnabled()
          is ManagedDeviceTestTask -> testTask.getAdditionalTestOutputEnabled()
          else -> provider { false }
        }
        val additionalTestOutputDir = additionalTestOutputEnabled
          .flatMap { enabled ->
            if (!enabled) {
              defaultTestOutputDirectory
            } else {
              when (testTask) {
                is DeviceProviderInstrumentTestTask -> testTask.additionalTestOutputDir
                is ManagedDeviceInstrumentationTestTask -> testTask.getAdditionalTestOutputDir()
                is ManagedDeviceTestTask -> testTask.getAdditionalTestOutputDir()
                else -> defaultTestOutputDirectory
              }
            }
          }
        val referenceImageResultDir = additionalTestOutputDir.map { it.dir("dropshots/reference") }
        val referenceImageSourceDir = layout.projectDirectory.dir("src/${variant.name}/androidTest/screenshots/$deviceProviderName")

        val updateTaskProvider = tasks.register(
          "update${testTask.name.capitalize()}Screenshots",
          UpdateScreenshotsTask::class.java,
        ) {
          if ("connected" == deviceProviderName) {
            it.description = "Updates screenshots for ${variant.name} on connected devices."
          } else {
            it.description = "Updates screenshots for ${variant.name} using provider: ${deviceProviderName.capitalize()}"
          }
          it.group = JavaBasePlugin.VERIFICATION_GROUP
          it.referenceImageDir.set(referenceImageResultDir)
          it.outputDir.set(referenceImageSourceDir)
        }
        updateTaskProvider.dependsOn(testTask.name)

        val isRecordingScreenshots = project.objects.property(Boolean::class.java)
        project.gradle.taskGraph.whenReady { graph ->
          isRecordingScreenshots.set(updateTaskProvider.map { graph.hasTask(it) })
        }

        val writeConfigTaskProvider = tasks.register(
          "write${testTask.name.capitalize()}ScreenshotConfigFile",
          WriteConfigFileTask::class.java,
        ) { task ->
          task.group = JavaBasePlugin.VERIFICATION_GROUP
          task.getIsRecording.set(isRecordingScreenshots)
          task.adbExecutable.set(adbProvider)
          task.remoteDir.set(
            testDataProvider.map { testData ->
              @Suppress("SdCardPath")
              "/sdcard/Android/media/${testData.instrumentationTargetPackageId.get()}/additional_test_output/dropbox"
            },
          )
        }
        testTask.dependsOn(writeConfigTaskProvider)
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

      val recordScreenshotsTask = tasks.register(
        "record${variantSlug}Screenshots",
        PullScreenshotsTask::class.java,
      ) {
        it.description = "Updates the local reference screenshots"

        it.adbExecutable.set(adbExecutablePath)
        it.screenshotDir.set(screenshotDir.map { base -> "$base/reference" })
        it.outputDirectory.set(referenceScreenshotDirectory)
        it.dependsOn(testTaskProvider)
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
        it.finalizedBy(pullScreenshotsTask)
      }
    }
  }
}
