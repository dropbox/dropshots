package com.dropbox.dropshots

import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.testing.ConnectedDeviceProvider
import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.MultiLineReceiver
import com.dropbox.dropshots.model.TestRunConfig
import java.io.File
import java.util.concurrent.TimeUnit
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Not worth caching")
public abstract class WriteConfigFileTask : DefaultTask() {

  @get:Input
  public abstract val getIsRecording: Property<Boolean>

  @get:Input
  public abstract val adbExecutable: Property<File>

  @get:Input
  public abstract val remoteDir: Property<String>

  init {
    description = "Writes Dropshots config file to emulator"
    outputs.upToDateWhen { false }
  }

  @TaskAction
  public fun performAction() {
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
        fun executeShellCommand(command: String, receiver: MultiLineReceiver) {
          device.executeShellCommand(command, receiver, DdmPreferences.getTimeOut().toLong(), TimeUnit.MILLISECONDS,)
        }

        val deviceName = device.name
        val config = TestRunConfig(getIsRecording.get(), deviceName)
        val remotePath = remoteDir.get()
        val loggingReceiver = object : MultiLineReceiver() {
          override fun isCancelled(): Boolean = false
          override fun processNewLines(lines: Array<out String>?) {
            lines?.forEach(logger::info)
          }
        }

        logger.info("DeviceConnector '$deviceName': creating parent directories $remotePath")
        executeShellCommand("mkdir -p $remotePath", loggingReceiver)

        logger.info("DeviceConnector '$deviceName': writing config file to $remotePath")
        val configFile = "$remotePath/$configFileName"
        executeShellCommand(
          "echo '${config.write()}' > $configFile",
          loggingReceiver,
        )
      }
    }
  }
}
