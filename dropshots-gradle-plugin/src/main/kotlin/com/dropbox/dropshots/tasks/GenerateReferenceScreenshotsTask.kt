package com.dropbox.dropshots.tasks

import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

public abstract class GenerateReferenceScreenshotsTask : DefaultTask() {
  @get:InputDirectory
  public abstract val referenceImageDir: DirectoryProperty

  @get:OutputDirectory
  public abstract val outputDir: DirectoryProperty

  @OptIn(ExperimentalPathApi::class)
  @TaskAction
  public fun performAction() {
    val from = referenceImageDir.asFile.get().toPath()
    val to = outputDir.asFile.get().toPath().resolve("dropshots")
    val logger = Logging.getLogger(GenerateReferenceScreenshotsTask::class.java)

    logger.lifecycle("Copying reference images to build directory: $to")
    Files.createDirectories(to)
    from.copyToRecursively(to, followLinks = true, overwrite = true)
  }
}
