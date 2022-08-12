package com.dropbox.dropshots

import com.dropbox.differ.ImageComparator
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Test

class ResultValidatorTest {

  @Test fun `CountValidator passes when less than count`() {
    val subject = CountValidator(20)
    assertTrue(subject(ImageComparator.ComparisonResult(0, 10, 5, 2)))
  }

  @Test fun `CountValidator passes when equal to count`() {
    val subject = CountValidator(20)
    assertTrue(subject(ImageComparator.ComparisonResult(20, 10, 5, 2)))
  }

  @Test fun `CountValidator fails when more than count`() {
    val subject = CountValidator(20)
    assertFalse(subject(ImageComparator.ComparisonResult(21, 10, 5, 2)))
  }

  @Test fun `CountValidator validates input`() {
    CountValidator(0)
    try {
      CountValidator(-1)
      fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      // pass
    }
  }

  @Test fun `ThresholdValidator passes when less than count`() {
    val subject = ThresholdValidator(.1f)
    assertTrue(subject(ImageComparator.ComparisonResult(0, 100, 10, 10)))
    assertTrue(subject(ImageComparator.ComparisonResult(9, 100, 10, 10)))
  }

  @Test fun `ThresholdValidator passes when equal to count`() {
    val subject = ThresholdValidator(.1f)
    assertTrue(subject(ImageComparator.ComparisonResult(10, 100, 10, 10)))
  }

  @Test fun `ThresholdValidator fails when more than count`() {
    val subject = ThresholdValidator(.1f)
    assertFalse(subject(ImageComparator.ComparisonResult(11, 100, 10, 10)))
  }

  @Test fun `ThresholdValidator validates input in range`() {
    ThresholdValidator(0f)
    try {
      ThresholdValidator(-1f)
      fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      // pass
    }
    try {
      ThresholdValidator(1.01f)
      fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      // pass
    }
  }
}
