plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.dokka)
  alias(libs.plugins.binaryCompatibilityValidator)
}

if (rootProject.name == "dropshots-root") {
  apply(plugin = libs.plugins.mavenPublish.get().pluginId)
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(libs.kotlinx.serialization)
}
