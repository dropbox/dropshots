import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import java.util.Locale

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.binaryCompatibilityValidator)
  id("com.dropbox.dropshots")
}

dropshots {
  // Our dropshots tests will use us, so we don't want a maven dependency added.
  applyDependency.set(false)
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
  api(projects.model)

  implementation(libs.androidx.annotation)
  implementation(libs.androidx.test.runner)
  implementation(libs.androidx.test.rules)

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
  val connectedAndroidTest = connectedInstrumentTestProvider
  val variantSlug = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

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

  connectedAndroidTest.configure {
    dependsOn(setupEmulatorTask)
    finalizedBy(restoreEmulatorTask)
  }
}
