package com.dropbox.dropshots

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

public abstract class DropshotsExtension @Inject constructor(objects: ObjectFactory) {
  /**
   * Relative to the module's (sub-project) directory, this is where all reference screenshots
   * (and any sub directories specified through [com.dropbox.dropshots.Dropshots.assertSnapshot])
   * will be saved.
   */
  public val referenceOutputDirectory: Property<String> = objects.property(String::class.java)
    .convention("src/androidTest/screenshots")

  /**
   * Whether to record screenshots on test failure. If true and recording is enabled, the
   * screenshots from the test device will be pulled and saved to the [referenceOutputDirectory]
   * even if the test fails.
   */
  public val recordOnFailure: Property<Boolean> = objects.property(Boolean::class.java)
    .convention(true)
}
