plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlinx.serialization)
  alias(libs.plugins.dokka)
  alias(libs.plugins.binaryCompatibilityValidator)
}

// This module is included in two projects:
// - In the root project where it's released as one of our artifacts
// - In build-logic project where we can use it for the runtime and samples.
//
// We only want to publish when it's being built in the root project.
if (rootProject.name == "dropshots-root") {
  apply(plugin = libs.plugins.mavenPublish.get().pluginId)
} else {
  // Move the build directory when included in build-support so as to not poison the real build.
  // If we don't the configuration cache is broken and all tasks are considered not up-to-date.
  layout.buildDirectory = File(rootDir, "build/model")
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(libs.kotlinx.serialization)
}
