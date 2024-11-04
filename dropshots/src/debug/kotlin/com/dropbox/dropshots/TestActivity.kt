package com.dropbox.dropshots

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

public class TestActivity : androidx.fragment.app.FragmentActivity()  {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        listOf(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN).forEach {
          addView(
            View(this@TestActivity).apply {
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
    )
  }
}
