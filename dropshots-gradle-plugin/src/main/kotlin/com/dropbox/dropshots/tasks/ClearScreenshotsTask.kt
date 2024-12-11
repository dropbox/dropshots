package com.dropbox.dropshots.tasks

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Destroys
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

public abstract class ClearScreenshotsTask : DefaultTask() {

  @get:Input
  public abstract val adbExecutable: Property<String>

  @get:Destroys
  public abstract val screenshotDir: Property<String>

  @get:Inject
  protected abstract val execOperations: ExecOperations

  init {
    description = "Removes the test screenshots from the test device."
    group = "verification"
  }

  @TaskAction
  public fun clearScreenshots() {
    val adb = adbExecutable.get()
    val dir = screenshotDir.get()
    val checkResult = execOperations.exec {
      it.executable = adb
      it.args = listOf("shell", "test", "-d", dir)
      it.isIgnoreExitValue = true
    }

    if (checkResult.exitValue == 0) {
      execOperations.exec {
        it.executable = adb
        it.args = listOf("shell", "rm", "-r", dir)
      }
    }
  }
}
