import com.vanniktech.maven.publish.AndroidLibrary
import com.vanniktech.maven.publish.JavadocJar.Dokka
import com.vanniktech.maven.publish.SonatypeHost.S01

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.binaryCompatibilityValidator)
}

android {
  namespace = "com.dropbox.dropshots"
  compileSdk = 32

  defaultConfig {
    minSdk = 23
    targetSdk = 32

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

kotlin {
  explicitApi()
}

dependencies {
  api(libs.differ)

  implementation(libs.androidx.test.runner)
  implementation("androidx.appcompat:appcompat:1.4.1")
  implementation("com.google.android.material:material:1.4.0")

  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidx.activity)
  androidTestImplementation(libs.androidx.core)
  androidTestImplementation(libs.androidx.fragment)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.rules)
}

mavenPublishing {
  configure(AndroidLibrary(Dokka("dokkaJavadoc"), false))
}
