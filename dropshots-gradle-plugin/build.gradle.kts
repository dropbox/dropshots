import com.android.build.gradle.tasks.SourceJarTask
import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar.Dokka
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-gradle-plugin`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.binaryCompatibilityValidator)
}

buildscript {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

sourceSets {
  main.configure {
    java.srcDir("src/generated/kotlin")
  }
}

mavenPublishing {
  configure(GradlePlugin(Dokka("dokkaJavadoc")))
}

val generateVersionTask = tasks.register("generateVersion") {
  inputs.property("version", project.property("VERSION_NAME") as String)
  outputs.dir(project.layout.projectDirectory.dir("src/generated/kotlin"))

  doLast {
    val output = File(outputs.files.first(), "com/dropbox/dropshots/Version.kt")
    output.parentFile.mkdirs()
    output.writeText("""
      |// Generated by gradle task.
      |package com.dropbox.dropshots
      |public const val VERSION: String = "${inputs.properties["version"]}"
    """.trimMargin())
  }
}

tasks.withType<Jar>().configureEach {
  dependsOn(generateVersionTask)
}

tasks.named("dokkaJavadoc").configure {
  dependsOn(generateVersionTask)
}

tasks.named("compileKotlin").configure {
  dependsOn(generateVersionTask)
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
    apiVersion.set(KotlinVersion.KOTLIN_1_8)
    languageVersion.set(KotlinVersion.KOTLIN_1_8)
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(11)
}

kotlin {
  explicitApi()
}

gradlePlugin {
  plugins {
    plugins.create("dropshots") {
      id = "com.dropbox.dropshots"
      implementationClass = "com.dropbox.dropshots.DropshotsPlugin"
    }
  }
}

// See https://github.com/slackhq/keeper/pull/11#issuecomment-579544375 for context
val releaseMode = hasProperty("dropshots.releaseMode")
dependencies {
  compileOnly(gradleApi())
  implementation(platform(libs.kotlin.bom))
  // Don't impose our version of KGP on consumers

  if (releaseMode) {
    compileOnly(libs.android)
    compileOnly(libs.kotlin.plugin)
  } else {
    implementation(libs.android)
    implementation(libs.kotlin.plugin)
  }

  testImplementation(gradleTestKit())
  testImplementation(platform(libs.kotlin.bom))
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}

tasks.register("printVersionName") {
  doLast {
    println(project.property("VERSION_NAME"))
  }
}

tasks.withType<Test>().configureEach {
  dependsOn(":dropshots:publishMavenPublicationToProjectLocalMavenRepository")
}
