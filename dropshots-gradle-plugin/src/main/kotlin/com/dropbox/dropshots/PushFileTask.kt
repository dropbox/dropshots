package com.dropbox.dropshots

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.temp.TemporaryFileProvider
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

public abstract class PushFileTask : DefaultTask() {

  @get:Input
  public abstract val adbExecutable: Property<String>

  /**
   * The file to push to the emulator.
   */
  @get:Input
  public abstract val fileContents: Property<String>

  /**
   * The path to the file or directory on the emulator.
   */
  @get:Input
  public abstract val remotePath: Property<String>

  @get:Inject
  protected abstract val execOperations: ExecOperations

  @get:Inject
  protected abstract val tempFileProvider: TemporaryFileProvider

  init {
    description = "Push files to an emulator or device using adb."
    outputs.upToDateWhen { false }
  }

  @TaskAction
  public fun push() {
    val tempFile = tempFileProvider.createTemporaryFile("adb-file", "", "dropshots")
    tempFile.writeText(fileContents.get())

    val adb = adbExecutable.get()
    val remote = remotePath.get()
    execOperations.exec {
      it.executable = adb
      it.args = listOf("push", tempFile.absolutePath, remote)
    }
  }
}