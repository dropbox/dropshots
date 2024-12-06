import java.util.Properties

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlinx.serialization) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.ktlint) apply false
  alias(libs.plugins.mavenPublish) apply false
}

subprojects {
  val props = Properties()
  project.rootProject.file("../../gradle.properties").inputStream().use(props::load)
  props.stringPropertyNames().forEach { k ->
    project.ext[k] = props[k]
  }
}
