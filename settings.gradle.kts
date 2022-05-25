pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

rootProject.name = "dropshots-root"

include(":dropshots-gradle-plugin")
include(":dropshots")
