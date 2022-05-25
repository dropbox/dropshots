package com.dropbox.dropshots

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Destroys
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

public abstract class ClearScreenshotsTask : DefaultTask() {

  @get:Input
  public abstract val adbExecutable: Property<String>

  @get:Destroys
  public abstract val screenshotDir: Property<String>

  init {
    description = "Removes the test screenshots from the test device."
    group = "verification"
  }

  @TaskAction
  public fun clearScreenshots() {
    project.exec {
      it.executable = adbExecutable.get()
      it.args = listOf("shell", "rm", "-r", screenshotDir.get())
    }
  }
}
