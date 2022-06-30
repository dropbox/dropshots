import com.vanniktech.maven.publish.AndroidLibrary
import com.vanniktech.maven.publish.JavadocJar.Dokka

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.binaryCompatibilityValidator)
}

android {
  namespace = "com.dropbox.dropshots"
  compileSdk = 32

  defaultConfig {
    minSdk = 19
    targetSdk = 32

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
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

  debugImplementation(libs.androidx.fragment)

  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.rules)
  androidTestImplementation(libs.androidx.test.runner)
}

mavenPublishing {
  configure(AndroidLibrary(Dokka("dokkaJavadoc"), false))
}

project.afterEvaluate {
  val connectedAndroidTest = tasks.named("connectedDebugAndroidTest")
  val adbExecutablePath = provider { android.adbExecutable.path }
  val pullScreenshotsTask = tasks.register("pullScreenshots") {
    dependsOn(connectedAndroidTest)

    description = "Pull screenshots from the test device."
    group = "verification"
    outputs.dir(project.layout.buildDirectory.dir("reports/androidTests/dropshots"))
    outputs.upToDateWhen { false }

    doLast {
      val outputDir = outputs.files.getSingleFile()
      outputDir.mkdirs()

      val adb = adbExecutablePath.get()
      val dir = "/storage/emulated/0/Download/screenshots/com.dropbox.dropshots.test"
      val checkResult = project.exec {
        executable = adb
        args = listOf("shell", "test", "-d", dir)
        isIgnoreExitValue = true
      }

      if (checkResult.exitValue == 0) {
        project.exec {
          executable = adb
          args = listOf("pull", "$dir/.", outputDir.path)
        }

        project.exec {
          executable = adb
          args = listOf("shell", "rm", "-r", dir)
        }
      }
    }
  }

  connectedAndroidTest.configure {
    finalizedBy(pullScreenshotsTask)
  }
}
