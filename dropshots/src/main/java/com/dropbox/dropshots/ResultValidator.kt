package com.dropbox.dropshots

import com.dropbox.differ.ImageComparator
import kotlin.math.roundToInt

/**
 * A function used to validate the comparison result.
 */
public typealias ResultValidator = (result: ImageComparator.ComparisonResult) -> Boolean

/**
 * Fails validation if there are more than `count` pixel differences.
 */
@Suppress("FunctionName")
public fun CountValidator(count: Int) : ResultValidator {
  require(count >= 0) { "count must be greater than or equal to 0." }
  return { result ->
    result.pixelDifferences <= count
  }
}

/**
 * Fails validation if more than `threshold` percent of pixels are different.
 */
@Suppress("FunctionName")
public fun ThresholdValidator(threshold: Float) : ResultValidator {
  require(threshold in 0f..1f) { "threshold must be in range 0.0..1.0"}
  return { result ->
    result.pixelDifferences <= (result.pixelCount * threshold).roundToInt()
  }
}


