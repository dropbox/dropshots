import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlinx.serialization) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.ktlint)
  alias(libs.plugins.mavenPublish)
}

allprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

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

  plugins.withType<KotlinBasePlugin>().configureEach {
    tasks.withType<KotlinCompile>().configureEach {
      compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        apiVersion.set(KotlinVersion.KOTLIN_1_9)
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
      }
    }
  }

  plugins.withType(JavaBasePlugin::class.java).configureEach {
    extensions.configure(JavaPluginExtension::class.java) {
      toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
      }
    }
  }
}

tasks.register("printVersionName") {
  doLast {
    println(project.property("VERSION_NAME"))
  }
}
