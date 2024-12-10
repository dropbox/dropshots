package com.dropbox.dropshots

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

public abstract class DropshotsExtension @Inject constructor(objects: ObjectFactory) {
  /**
   * Whether the Dropshots plugin should automatically apply the
   * Dropshots runtime dependency.
   *
   * You can use this to disable automatic addition of the runtime dependency
   * if you have your own fork, but in most cases this should use the default
   * value of `true`.
   */
  public val applyDependency: Property<Boolean> = objects.property(Boolean::class.java)
    .convention(true)
}
