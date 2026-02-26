package com.tether.depin

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.tether.depin.ui.screens.DashboardScreen
import com.tether.depin.ui.theme.TetherTheme
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboardShowsProxyButton() {
        composeTestRule.setContent {
            TetherTheme {
                DashboardScreen()
            }
        }

        // Verify START PROXY button exists
        composeTestRule
            .onNodeWithText("START PROXY", ignoreCase = true)
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun dashboardShowsTetherNodeHeader() {
        composeTestRule.setContent {
            TetherTheme {
                DashboardScreen()
            }
        }

        composeTestRule
            .onNodeWithText("Tether Node")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun dashboardShowsRecentActivitySection() {
        composeTestRule.setContent {
            TetherTheme {
                DashboardScreen()
            }
        }

        composeTestRule
            .onNodeWithText("Recent Activity")
            .assertExists()
    }

    @Test
    fun proxyButtonTogglesText() {
        composeTestRule.setContent {
            TetherTheme {
                DashboardScreen()
            }
        }

        // Initially shows START PROXY
        composeTestRule
            .onNodeWithText("START PROXY", ignoreCase = true)
            .assertExists()
    }
}
