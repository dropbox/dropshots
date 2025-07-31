package com.dropbox.dropshots

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity

class TestActivity : FragmentActivity() {
    
    companion object {
        private val DEMO_COLORS = listOf(
            Color.RED,
            Color.GREEN, 
            Color.BLUE,
            Color.CYAN
        )
        private const val COLOR_STRIP_HEIGHT_DP = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createColorStripLayout())
    }
    
    private fun createColorStripLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            DEMO_COLORS.forEach { color ->
                addView(createColorStrip(color))
            }
        }
    }
    
    private fun createColorStrip(color: Int): View {
        return View(this).apply {
            setBackgroundColor(color)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(COLOR_STRIP_HEIGHT_DP)
            ).apply {
                weight = 1f
            }
            
            contentDescription = "Color strip: ${getColorName(color)}"
        }
    }
    
    private fun getColorName(color: Int): String {
        return when (color) {
            Color.RED -> "Red"
            Color.GREEN -> "Green"
            Color.BLUE -> "Blue"
            Color.CYAN -> "Cyan"
            else -> "Unknown color"
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
