package com.felixbrucker.sleeptracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.felixbrucker.sleeptracker.ui.screens.SleepTrackerScreen
import com.felixbrucker.sleeptracker.ui.viewmodel.SleepTrackerUiState
import com.felixbrucker.sleeptracker.ui.theme.SleepTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [37])
class SleepTrackerScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sleepTracker_screenshot() {
        composeTestRule.setContent {
            SleepTrackerTheme {
                SleepTrackerScreen(
                    uiState = SleepTrackerUiState(),
                    onStartTracking = {},
                    onStopTracking = {},
                    onUpdateReminder = { _, _, _ -> },
                    onTestReminder = {},
                    onSetAutoSync = {},
                    onSyncSession = {},
                    onSyncAll = {},
                    onDeleteSession = {},
                    onUpdateSession = { _, _, _ -> },
                    onSetFallAsleepDuration = {},
                    onRefreshHealthConnect = {},
                    onClearStatusMessage = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
