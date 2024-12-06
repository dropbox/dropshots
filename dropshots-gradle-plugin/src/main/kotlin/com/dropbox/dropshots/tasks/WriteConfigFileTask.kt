package com.dropbox.dropshots.tasks

import com.android.build.gradle.internal.LoggerWrapper
import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.MultiLineReceiver
import com.dropbox.dropshots.configFileName
import com.dropbox.dropshots.device.DeviceProviderFactory
import com.dropbox.dropshots.model.TestRunConfig
import java.io.File
import java.util.concurrent.TimeUnit
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Not worth caching")
public abstract class WriteConfigFileTask : DefaultTask() {

  @get:Input
  public abstract val recordingScreenshots: Property<Boolean>

  @get:Input
  public abstract val deviceProviderName: Property<String>

  @get:Input
  public abstract val adbExecutable: Property<File>

  @get:Input
  public abstract val remoteDir: Property<String>

  @get:Nested
  public abstract val deviceProviderFactory: DeviceProviderFactory

  init {
    description = "Writes Dropshots config file to emulator"
    outputs.upToDateWhen { false }
  }

  @TaskAction
  public fun performAction() {
    val iLogger = LoggerWrapper(logger)
    val deviceProvider = deviceProviderFactory
      .getDeviceProvider(adbExecutable, iLogger)

    deviceProvider.use {
      @Suppress("UnstableApiUsage")
      deviceProvider.devices.forEach { device ->
        fun executeShellCommand(command: String, receiver: MultiLineReceiver) {
          device.executeShellCommand(command, receiver, DdmPreferences.getTimeOut().toLong(), TimeUnit.MILLISECONDS,)
        }

        val deviceName = device.name
        val config = TestRunConfig(recordingScreenshots.get(), deviceProviderName.get())
        val remotePath = remoteDir.get()
        val loggingReceiver = object : MultiLineReceiver() {
          override fun isCancelled(): Boolean = false
          override fun processNewLines(lines: Array<out String>?) {
            lines?.forEach(logger::info)
          }
        }

        logger.info("DeviceConnector '$deviceName': creating directories $remotePath")
        executeShellCommand("mkdir -p $remotePath", loggingReceiver)

        val configFile = "$remotePath/$configFileName"
        logger.warn("DeviceConnector '$deviceName': writing config file to $configFile")
        executeShellCommand(
          "echo '${config.write()}' > $configFile",
          loggingReceiver,
        )
      }
    }
  }
}
