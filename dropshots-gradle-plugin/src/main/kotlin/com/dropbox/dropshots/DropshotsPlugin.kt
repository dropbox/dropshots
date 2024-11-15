package com.dropbox.dropshots

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.DeviceTest
import com.android.build.api.variant.DeviceTestBuilder
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.Variant
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.AndroidBasePlugin
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask
import com.android.build.gradle.internal.tasks.ManagedDeviceInstrumentationTestTask
import com.android.build.gradle.internal.tasks.ManagedDeviceTestTask
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.android.build.gradle.options.BooleanOption
import com.android.build.gradle.options.ProjectOptionService
import com.android.builder.core.ComponentType
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.internal.configuration.problems.taskPathFrom

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

      @Suppress("UnstableApiUsage")
      componentsExtension.onVariants { variant ->
        if (!variant.debuggable || variant !is HasDeviceTests) {
          return@onVariants
        }

        val deviceTestComponent = variant.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE] ?: return@onVariants
        val adbProvider = provider { testedExtension.adbExecutable }

        // Create a test connected check task
        addTasksForDeviceProvider(variant, deviceTestComponent, "connected", adbProvider)

        testedExtension.deviceProviders.forEach { deviceProvider ->
          addTasksForDeviceProvider(variant, deviceTestComponent, deviceProvider.name, adbProvider)
        }

        val optionService = ProjectOptionService.RegistrationAction(this).execute().get()
        optionService.projectOptions.get(BooleanOption.ENABLE_ADDITIONAL_ANDROID_TEST_OUTPUT)
      }
    }
  }

  private fun Project.addTasksForDeviceProvider(
    variant: Variant,
    deviceTest: DeviceTest,
    deviceProviderName: String,
    adbProvider: Provider<File>,
  ) {
    tasks
      .named {
        it == "${deviceProviderName}${variant.name.capitalize()}${ComponentType.ANDROID_TEST_SUFFIX}"
      }
      .all { testTask ->
        val testDataProvider = when (testTask) {
          is DeviceProviderInstrumentTestTask -> testTask.testData
          is ManagedDeviceInstrumentationTestTask -> testTask.testData
          is ManagedDeviceTestTask -> testTask.testData
          else -> return@all
        }
        val defaultTestOutputDirectory = layout.buildDirectory
          .dir("outputs/androidTest-results/$deviceProviderName/${variant.name}")
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

        val addReferenceAssetsTask = tasks.register(
          "generate${testTask.name.capitalize()}ReferenceScreenshots",
          GenerateReferenceScreenshotsTask::class.java
        ) { task ->
          task.referenceImageDir.set(layout.projectDirectory.dir("src/androidTest/screenshots"))
          task.outputDir.set(layout.buildDirectory.dir("generated/dropshots/reference"))
        }
        logger.warn("Adding generated assets to: ${variant.sources.assets}")
        deviceTest.sources.assets?.addGeneratedSourceDirectory(addReferenceAssetsTask) { task -> task.outputDir }

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
          it.referenceImageDir.set(additionalTestOutputDir)
          it.deviceProviderName.set(deviceProviderName)
          it.outputDir.set(layout.projectDirectory.dir("src/androidTest/screenshots/"))
        }
        tasks.named("updateDropshotsScreenshots").dependsOn(updateTaskProvider)

        val isRecordingScreenshots = project.objects.property(Boolean::class.java)
        project.gradle.taskGraph.whenReady { graph ->
          isRecordingScreenshots.set(updateTaskProvider.map { graph.hasTask(it) })
        }

        // additional test output will be overwritten by the test task, so we need to use our own
        // input directory to send information to the device.
        val remoteDirProvider = testDataProvider.map { testData ->
          @Suppress("SdCardPath")
          "/sdcard/Android/media/${testData.instrumentationTargetPackageId.get()}/dropshots"
        }

        val writeConfigTaskProvider = tasks.register(
          "write${testTask.name.capitalize()}ScreenshotConfigFile",
          WriteConfigFileTask::class.java,
        ) { task ->
          task.group = JavaBasePlugin.VERIFICATION_GROUP
          task.recordingScreenshots.set(isRecordingScreenshots)
          task.deviceProviderName.set(deviceProviderName)
          task.adbExecutable.set(adbProvider)
          task.remoteDir.set(remoteDirProvider)
        }
        testTask.dependsOn(writeConfigTaskProvider)

        val pullScreenshotsTask = tasks.register(
          "pull${testTask.name.capitalize()}Screenshots",
          PullScreenshotsTask::class.java,
        ) { task ->
          task.onlyIf { !additionalTestOutputEnabled.get() }
          task.group = JavaBasePlugin.VERIFICATION_GROUP
          task.adbExecutable.set(adbProvider)
          task.remoteDir.set(remoteDirProvider)
          task.outputDirectory.set(additionalTestOutputDir)
        }
        testTask.finalizedBy(pullScreenshotsTask)
        updateTaskProvider.dependsOn(pullScreenshotsTask)
      }
  }
}
