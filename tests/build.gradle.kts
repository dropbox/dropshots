plugins {
  alias(libs.plugins.android.library)
  id("com.dropbox.dropshots")
}

dropshots {
  referenceOutputDirectory = "src/androidTest/assets"
}

android {
  namespace = "com.dropbox.dropshots.integrationtests"
  compileSdk = 34

  defaultConfig {
    minSdk = 21

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
}

dependencies {
  implementation(libs.androidx.fragment)

  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.rules)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.uiautomator)
}
