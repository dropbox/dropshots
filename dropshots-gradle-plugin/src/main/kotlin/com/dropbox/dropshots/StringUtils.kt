package com.dropbox.dropshots

import java.util.Locale

internal fun String.capitalize(): String =
  replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
