package com.dropbox.dropshots.tasks

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

  @get:Input
  public abstract val deviceProviderName: Property<String>

  @get:Optional
  @get:Input
  public abstract val outputBasePath: Property<String>

  @get:OutputDirectory
  public abstract val outputDir: DirectoryProperty

  @OptIn(ExperimentalPathApi::class)
  @TaskAction
  public fun performAction() {
    val from = referenceImageDir.asFile.get().toPath()
    val to = outputDir.asFile.get().toPath()
    val logger = Logging.getLogger(UpdateScreenshotsTask::class.java)

    Files.list(from).forEach { devicePath ->
      val deviceName = deviceProviderName.get()
      logger.lifecycle("Copying reference images for $deviceName")

      val referenceImagePath = devicePath.resolve("dropshots/reference")
      val outputPath = to.resolve(deviceName)
      Files.createDirectories(outputPath)
      referenceImagePath.copyToRecursively(
        outputPath,
        followLinks = true,
        overwrite = true,
      )
    }
  }
}
