pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
  includeBuild("..")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
  }
}

includeBuild("..") {
  dependencySubstitution {
    substitute(module("com.dropbox:dropshots")).using(project(":dropshots"))
  }
}
