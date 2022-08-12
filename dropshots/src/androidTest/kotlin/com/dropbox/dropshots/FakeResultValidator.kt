package com.dropbox.dropshots

import com.dropbox.differ.ImageComparator

class FakeResultValidator(
  var validator: ResultValidator = CountValidator(0)
) : ResultValidator {
  override fun invoke(result: ImageComparator.ComparisonResult): Boolean = validator(result)
}
