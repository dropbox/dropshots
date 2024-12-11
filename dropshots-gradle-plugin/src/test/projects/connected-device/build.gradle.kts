plugins {
  id("com.dropbox.dropshots")
  alias(libs.plugins.android.library)
}

android {
  namespace = "com.dropbox.dropshots.test"
  compileSdk = 34
  testOptions.targetSdk = 34
  lint.targetSdk = 34
  defaultConfig.minSdk = 21
}
