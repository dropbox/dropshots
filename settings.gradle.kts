pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "dropshots-root"

include(":dropshots-gradle-plugin")
include(":dropshots")
include(":model")
