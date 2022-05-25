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

    project.exec {
      it.executable = adbExecutable.get()
      it.args = listOf("pull", "${screenshotDir.get()}/.", outputDir.path)
    }
  }
}

