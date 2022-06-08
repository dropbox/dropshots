package com.dropbox.dropshots

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

public abstract class PullScreenshotsTask : DefaultTask() {

  @get:Input
  public abstract val adbExecutable: Property<String>

  @get:Input
  public abstract val screenshotDir: Property<String>

  @get:OutputDirectory
  public abstract val outputDirectory: DirectoryProperty

  init {
    description = "Pull screenshots from the test device."
    group = "verification"
    outputs.upToDateWhen { false }
  }

  @TaskAction
  public fun pullScreenshots() {
    val outputDir = outputDirectory.get().asFile
    outputDir.mkdirs()

    val adb = adbExecutable.get()
    val dir = screenshotDir.get()
    val checkResult = project.exec {
      it.executable = adb
      it.args = listOf("shell", "test", "-d", dir)
      it.isIgnoreExitValue = true
    }

    if (checkResult.exitValue == 0) {
      project.exec {
        it.executable = adb
        it.args = listOf("pull", "$dir/.", outputDir.path)
      }
    }
  }
}

