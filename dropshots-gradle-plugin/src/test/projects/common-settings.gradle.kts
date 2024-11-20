pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

includeBuild("../../../..") {
  dependencySubstitution {
    substitute(module("com.dropbox.dropshots:dropshots")).using(project(":dropshots"))
    substitute(module("com.dropbox.dropshots:model")).using(project(":model"))
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../../../../gradle/libs.versions.toml"))
    }
  }
}
