import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
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
  }
}

tasks.register("printVersionName") {
  doLast {
    val VERSION_NAME: String by project
    println(VERSION_NAME)
  }
}

