import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.ktlint)
  alias(libs.plugins.mavenPublish)
}

allprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

  repositories {
    google()
    mavenCentral()
  }

  plugins.withId("com.vanniktech.maven.publish.base") {
    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(SonatypeHost.S01)
      signAllPublications()

      pomFromGradleProperties()
    }

    publishing {
      repositories {
        maven {
          name = "projectLocalMaven"
          url = uri(rootProject.layout.buildDirectory.dir("localMaven"))
        }
      }
    }
  }
}

tasks.register("printVersionName") {
  doLast {
    println(project.property("VERSION_NAME"))
  }
}
