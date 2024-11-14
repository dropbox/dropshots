package com.dropbox.dropshots.model

import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/**
 * Test run configuration passed from the Dropshots Gradle plugin to the
 * runtime via a serialized file written to the test storage location.
 */
@Serializable
public data class TestRunConfig(
  val isRecording: Boolean,
  val deviceName: String,
) {
  public companion object {
    @OptIn(ExperimentalSerializationApi::class)
    public fun read(data: InputStream): TestRunConfig = Json.decodeFromStream(data)
    public fun read(data: String): TestRunConfig = Json.decodeFromString(data)
  }

  public fun write(): String = Json.encodeToString(this)
}

