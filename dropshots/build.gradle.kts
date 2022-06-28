import com.android.build.api.dsl.ManagedVirtualDevice
import com.vanniktech.maven.publish.AndroidLibrary
import com.vanniktech.maven.publish.JavadocJar.Dokka

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
    minSdk = 19
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

  implementation(libs.androidx.annotation)
  implementation(libs.androidx.test.runner)
  implementation(libs.androidx.test.rules)

  debugImplementation(libs.androidx.fragment)

  androidTestImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.rules)
  androidTestImplementation(libs.androidx.test.runner)
}

mavenPublishing {
  configure(AndroidLibrary(Dokka("dokkaJavadoc"), false))
}
