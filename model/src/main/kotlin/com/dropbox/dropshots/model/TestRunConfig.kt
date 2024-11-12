package com.dropbox.dropshots.model

import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

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
    public fun read(inputStream: InputStream): TestRunConfig {
      return Cbor.decodeFromByteArray<TestRunConfig>(inputStream.readAllBytes())
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  public fun write(outputStream: OutputStream) {
    outputStream.write(Cbor.encodeToByteArray(this))
  }
}

