package com.dropbox.dropshots

import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyToRecursively
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Not worth caching")
public abstract class UpdateScreenshotsTask : DefaultTask() {
  @get:InputDirectory
  public abstract val referenceImageDir: DirectoryProperty

  @get:Optional
  @get:Input
  public abstract val outputBasePath: Property<String>

  @get:OutputDirectory
  public abstract val outputDir: DirectoryProperty

  @OptIn(ExperimentalPathApi::class)
  @TaskAction
  public fun performAction() {
    val from = referenceImageDir.asFile.get().toPath()
    val to = outputDir.asFile.get().toPath().let { output ->
      if (outputBasePath.isPresent) {
        output.resolve(outputBasePath.get())
      } else {
        output
      }
    }
    val logger = Logging.getLogger(UpdateScreenshotsTask::class.java)
    logger.lifecycle("Copying reference images to $to")
    Files.createDirectories(to)
    from.copyToRecursively(to, followLinks = true)
  }
}
