package com.tether.depin.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tether.depin.ui.components.NavTab
import com.tether.depin.ui.components.TetherBottomNavBar
import com.tether.depin.ui.screens.*
import com.tether.depin.wallet.WalletConnectionManager

@Composable
fun TetherNavGraph() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(NavTab.Dashboard) }
    val walletManager = WalletConnectionManager
    val walletState by walletManager.walletState.collectAsState()

    // CRITICAL FIX: Compute start destination ONCE at first composition.
    // Previously this was reactive: `if (walletState.isConnected) Dashboard else onboarding`
    // which caused the NavHost to recompose when isConnected flipped, destroying the
    // onboarding composable (and its key-backup dialog) mid-display.
    val startDestination = remember {
        if (WalletConnectionManager.walletState.value.isConnected) NavTab.Dashboard.route else "onboarding"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (walletState.isConnected) 90.dp else 0.dp)
        ) {
            composable("onboarding") {
                WalletOnboardingScreen(
                    walletManager = walletManager,
                    onConnected = {
                        navController.navigate(NavTab.Dashboard.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable(NavTab.Dashboard.route) {
                DashboardScreen()
            }
            composable(NavTab.Wallet.route) {
                WalletScreen()
            }
            composable(NavTab.Network.route) {
                NetworkScreen()
            }
            composable(NavTab.Settings.route) {
                SettingsScreen(
                    onSignOut = {
                        walletManager.disconnectWallet()
                        navController.navigate("onboarding") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable("invite") {
                InviteScreen(onBack = {
                    navController.navigate(NavTab.Dashboard.route) {
                        popUpTo(NavTab.Dashboard.route) { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }
        }

        // Bottom Navigation — only show when wallet is connected
        if (walletState.isConnected) {
            TetherBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    navController.navigate(tab.route) {
                        popUpTo(NavTab.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onFabClick = {
                    navController.navigate("invite") {
                        popUpTo(NavTab.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}
