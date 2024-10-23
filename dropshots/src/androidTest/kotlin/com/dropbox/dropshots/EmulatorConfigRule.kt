package com.dropbox.dropshots

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class EmulatorConfigRule : TestRule {
  override fun apply(base: Statement, description: Description): Statement =
    EmulatorConfigStatement(base)

  private class EmulatorConfigStatement(
    private val base: Statement,
  ) : Statement() {
    override fun evaluate() {
      enableDemoMode()
      base.evaluate()
      disableDemoMode()
    }

    private fun enableDemoMode() {
      UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        .apply {
          executeShellCommand("cmd overlay enable com.android.internal.systemui.navbar.gestural")
          executeShellCommand("settings put global sysui_demo_allowed 1")
          executeShellCommand("am broadcast -a com.android.systemui.demo -e command enter")
          executeShellCommand("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1234")
          executeShellCommand("am broadcast -a com.android.systemui.demo -e command battery -e plugged false")
          executeShellCommand("am broadcast -a com.android.systemui.demo -e command battery -e level 100")
          executeShellCommand("am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4")
          executeShellCommand("am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4")
          executeShellCommand("am broadcast -a com.android.systemui.demo -e command notifications -e visible false")
        }
    }

    private fun disableDemoMode() {
      UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        .executeShellCommand("am broadcast -a com.android.systemui.demo -e command exit")
    }
  }
}
