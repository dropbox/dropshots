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
}
