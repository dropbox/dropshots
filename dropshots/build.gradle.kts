import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.binaryCompatibilityValidator)
  id("com.dropbox.dropshots")
}

dropshots {
  // Our dropshots tests will use us, so we don't want a maven dependency added.
  applyDependency.set(false)
}

android {
  namespace = "com.dropbox.dropshots"
  compileSdk = 34
  testOptions.targetSdk = 34
  lint.targetSdk = 34

  defaultConfig {
    minSdk = 21

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
}

kotlin {
  explicitApi()
}

dependencies {
  api(libs.differ)
  api(projects.model)

  implementation(libs.androidx.annotation)
  implementation(libs.androidx.test.runner)
  implementation(libs.androidx.test.rules)

  debugImplementation(libs.androidx.fragment)

  testImplementation(platform(libs.kotlin.bom))
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)

  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.rules)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.uiautomator)
}

mavenPublishing {
  configure(AndroidSingleVariantLibrary(
    variant = "release",
    sourcesJar = true,
    publishJavadocJar = true,
  ))
}
