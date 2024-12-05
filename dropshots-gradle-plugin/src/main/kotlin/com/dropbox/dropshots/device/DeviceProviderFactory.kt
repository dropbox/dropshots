package com.dropbox.dropshots.device

import com.android.build.gradle.internal.LoggerWrapper
import com.android.build.gradle.internal.testing.ConnectedDeviceProvider
import com.android.builder.testing.api.DeviceProvider
import com.android.ddmlib.DdmPreferences
import com.android.utils.ILogger
import java.io.File
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal

public abstract class DeviceProviderFactory {
  @Suppress("DEPRECATION")
  @get:Internal
  internal var deviceProvider: DeviceProvider? = null

  @Suppress("DEPRECATION")
  public fun getDeviceProvider(
    adbExecutableProvider: Provider<File>,
    logger: ILogger = LoggerWrapper.getLogger(DeviceProviderFactory::class.java),
    environmentSerials: String? = System.getenv("ANDROID_SERIAL"),
  ): DeviceProvider {
    return deviceProvider ?: ConnectedDeviceProvider(
      adbExecutableProvider.get(),
      DdmPreferences.getTimeOut(),
      logger,
      environmentSerials,
    )
  }
}
