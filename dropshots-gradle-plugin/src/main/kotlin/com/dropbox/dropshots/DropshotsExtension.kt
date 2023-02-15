package com.dropbox.dropshots

public open class DropshotsExtension {
  public var referenceDirectory: String = "src/androidTest/screenshots"

  public companion object {
    public const val EXTENSION_NAME: String = "dropshots"
  }
}
