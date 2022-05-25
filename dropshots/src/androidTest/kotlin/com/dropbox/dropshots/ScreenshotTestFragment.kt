package com.dropbox.dropshots

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment

class ScreenshotTestFragment : Fragment() {
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return LinearLayout(inflater.context).apply {
      orientation = LinearLayout.VERTICAL

      listOf(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN).forEach {
        addView(
          View(inflater.context).apply {
            setBackgroundColor(it)
            layoutParams = LinearLayout.LayoutParams(
              ViewGroup.MarginLayoutParams.MATCH_PARENT,
              ViewGroup.MarginLayoutParams.WRAP_CONTENT,
              1f
            )
          }
        )
      }
    }
  }
}
