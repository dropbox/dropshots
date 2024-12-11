package com.dropbox.dropshots

import android.net.Uri
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.io.PlatformTestStorage
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable

class FileTestStorage(
  val inputDir: File,
  val outputDir: File,
) : PlatformTestStorage {

  override fun openInputFile(pathname: String?): InputStream {
    requireNotNull(pathname)
    return inputDir.resolve(pathname).inputStream()
  }

  override fun openOutputFile(pathname: String?): OutputStream {
    return openOutputFile(pathname, false)
  }

  override fun openOutputFile(pathname: String?, append: Boolean): OutputStream {
    requireNotNull(pathname)
    val outputFile = File(outputDir, pathname)
    val parentFile = outputFile.parentFile
    if (parentFile != null && !parentFile.exists()) {
      if (!parentFile.mkdirs()) {
        throw FileNotFoundException("Failed to create output dir ${parentFile.absolutePath}")
      }
    }
    return FileOutputStream(outputFile, append)
  }

  override fun getInputArg(argName: String?): String? {
    requireNotNull(argName)
    return InstrumentationRegistry.getArguments().getString(argName)
  }

  override fun getInputArgs(): Map<String, String> {
    return buildMap {
      val args = InstrumentationRegistry.getArguments()
      for (k in args.keySet()) {
        args.getString(k)?.let { put(k, it) }
      }
    }
  }

  override fun addOutputProperties(properties: MutableMap<String, Serializable>?) {
    Log.w("FileTestStorage", "Output properties is not supported.")
  }

  override fun getOutputProperties(): Map<String, Serializable> {
    Log.w("FileTestStorage", "Output properties is not supported.")
    return emptyMap()
  }

  override fun getInputFileUri(pathname: String): Uri {
    return Uri.fromFile(inputDir.resolve(pathname))
  }

  override fun getOutputFileUri(pathname: String): Uri {
    return Uri.fromFile(outputDir.resolve(pathname))
  }

  override fun isTestStorageFilePath(pathname: String): Boolean {
    return pathname.startsWith(outputDir.absolutePath)
  }
}
