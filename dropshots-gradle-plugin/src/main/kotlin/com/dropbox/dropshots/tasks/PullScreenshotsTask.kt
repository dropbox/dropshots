package com.dropbox.dropshots.tasks

import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.testing.ConnectedDeviceProvider
import com.android.ddmlib.DdmPreferences
import java.io.File
import kotlin.io.path.absolutePathString
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

public abstract class PullScreenshotsTask : DefaultTask() {

  @get:Input
  public abstract val adbExecutable: Property<File>

  @get:Input
  public abstract val remoteDir: Property<String>

  @get:OutputDirectory
  public abstract val outputDirectory: DirectoryProperty

  init {
    description = "Pull screenshots from the test device."
    group = "verification"
    outputs.upToDateWhen { false }
  }

  @TaskAction
  public fun pullScreenshots() {
    val iLogger = LoggerWrapper(logger)
    val deviceProvider = ConnectedDeviceProvider(
      adbExecutable.get(),
      DdmPreferences.getTimeOut(),
      iLogger,
      System.getenv("ANDROID_SERIAL"),
    )

    deviceProvider.use {
      @Suppress("UnstableApiUsage")
      deviceProvider.devices.forEach { device ->
        val remotePath = remoteDir.get()
        val localPath = outputDirectory.dir("${device.name}/dropshots").get().asFile.toPath()

        // TODO Does this really only do a single file? If so we'll have to `ls` to get the files
        device.pullFile("$remotePath/.", localPath.absolutePathString())
      }
    }
  }
}

