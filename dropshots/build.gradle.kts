import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import java.io.ByteArrayOutputStream
import java.util.Locale

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.binaryCompatibilityValidator)
}

android {
  namespace = "com.dropbox.dropshots"
  compileSdk = 34
  testOptions.targetSdk = 34
  lint.targetSdk = 34

  defaultConfig {
    minSdk = 21

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
}

kotlin {
  explicitApi()
}

dependencies {
  api(libs.differ)

  implementation(libs.androidx.annotation)
  implementation(libs.androidx.test.runner)
  implementation(libs.androidx.test.rules)
  implementation(projects.model)

  debugImplementation(libs.androidx.fragment)

  testImplementation(platform(libs.kotlin.bom))
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)

  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.rules)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.uiautomator)
}

mavenPublishing {
  configure(AndroidSingleVariantLibrary(
    variant = "release",
    sourcesJar = true,
    publishJavadocJar = true,
  ))
}

val adbExecutablePath = provider { android.adbExecutable.path }
android.testVariants.all {
  val screenshotDir = "/storage/emulated/0/Download/screenshots/com.dropbox.dropshots.test"
  val connectedAndroidTest = connectedInstrumentTestProvider
  val variantSlug = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

  val recordScreenshotsTask = tasks.register("record${variantSlug}Screenshots")
  val isRecordingScreenshots = project.objects.property(Boolean::class.java)
  project.gradle.taskGraph.whenReady {
    isRecordingScreenshots.set(recordScreenshotsTask.map { hasTask(it) })
  }

  val pushMarkerFileTask = tasks.register("push${variantSlug}ScreenshotMarkerFile") {
    description = "Push screenshot marker file to test device."
    group = "verification"
    outputs.upToDateWhen { false }
    onlyIf { isRecordingScreenshots.get() }

    doLast {
      val adb = adbExecutablePath.get()
      project.exec {
        executable = adb
        args = listOf("shell", "mkdir", "-p", screenshotDir)
      }
      project.exec {
        executable = adb
        args = listOf("shell", "touch", "$screenshotDir/.isRecordingScreenshots")
      }
    }
  }

  val setupEmulatorTask = tasks.register("setup${variantSlug}ScreenshotEmulator") {
    description = "Configures the test device for screenshots."
    group = "verification"
    doLast {
      val adb = adbExecutablePath.get()
      fun adbCommand(cmd: String): ExecResult {
        return project.exec {
          executable = adb
          args = cmd.split(" ")
        }
      }

      adbCommand("root")
      adbCommand("wait-for-device")
      adbCommand("shell settings put global sysui_demo_allowed 1")
      adbCommand("shell am broadcast -a com.android.systemui.demo -e command enter")
        .assertNormalExitValue()
      adbCommand("shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1234")
      adbCommand("shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false")
      adbCommand("shell am broadcast -a com.android.systemui.demo -e command battery -e level 100")
      adbCommand("shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4")
      adbCommand("shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4")
      adbCommand("shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false")
    }
  }
  val restoreEmulatorTask = tasks.register("restore${variantSlug}ScreenshotEmulator") {
    description = "Restores the test device from screenshot mode."
    group = "verification"
    doLast {
      project.exec {
        executable = adbExecutablePath.get()
        args = "shell am broadcast -a com.android.systemui.demo -e command exit".split(" ")
      }
    }
  }

  val pullScreenshotsTask = tasks.register("pull${variantSlug}Screenshots") {
    description = "Pull screenshots from the test device."
    group = "verification"
    outputs.dir(project.layout.buildDirectory.dir("reports/androidTests/dropshots"))
    outputs.upToDateWhen { false }

    doLast {
      val outputDir = outputs.files.singleFile
      outputDir.mkdirs()

      val adb = adbExecutablePath.get()
      val checkResult = project.exec {
        executable = adb
        args = listOf("shell", "test", "-d", screenshotDir)
        isIgnoreExitValue = true
      }

      if (checkResult.exitValue == 0) {
        val output = ByteArrayOutputStream()
        val pullResult = project.exec {
          executable = adb
          args = listOf("pull", "$screenshotDir/.", outputDir.path)
          standardOutput = output
          isIgnoreExitValue = true
        }

        if (pullResult.exitValue == 0) {
          val fileCount = """^${screenshotDir.replace(".", "\\.")}/?\./: ([0-9]*) files pulled,.*$""".toRegex()
          val matchResult = fileCount.find(output.toString(Charsets.UTF_8))
          if (matchResult != null && matchResult.groups.size > 1) {
            println("${matchResult.groupValues[1]} screenshots saved at ${outputDir.path}")
          } else {
            println("Unknown result executing adb: $adb pull $screenshotDir/. ${outputDir.path}")
            print(output.toString(Charsets.UTF_8))
          }
        } else {
          println("Failed to pull screenshots.")
        }

        project.exec {
          executable = adb
          args = listOf("shell", "rm", "-r", "/storage/emulated/0/Download/screenshots")
        }
      }
    }
  }

  connectedAndroidTest.configure {
    dependsOn(pushMarkerFileTask)
    finalizedBy(pullScreenshotsTask)
    dependsOn(setupEmulatorTask)
    finalizedBy(restoreEmulatorTask)
  }
}
