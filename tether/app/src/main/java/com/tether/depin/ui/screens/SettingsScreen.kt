package com.tether.depin.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tether.depin.ui.theme.*
import com.tether.depin.ui.viewmodel.SettingsViewModel
import com.tether.depin.wallet.WalletConnectionManager

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(), onSignOut: () -> Unit = {}) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val persistentBackground by viewModel.persistentBackground.collectAsState()
    val wifiOnly by viewModel.wifiOnly.collectAsState()
    val batterySaver by viewModel.batterySaver.collectAsState()
    val dailyLimit by viewModel.dailyDataLimitGb.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    // Read real wallet state from singleton
    val walletState by WalletConnectionManager.walletState.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Private Key Export Dialog — shows REAL key from EncryptedSharedPreferences
    if (showExportDialog) {
        var keyCopied by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showExportDialog = false; keyCopied = false },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Primary)
                    Text("Export Private Key", color = White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Warning banner
                    Surface(shape = RoundedCornerShape(12.dp), color = Red500.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Red500.copy(alpha = 0.25f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⚠️ NEVER share your private key with anyone.",
                                style = MaterialTheme.typography.labelMedium, color = Red500, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Store it in a secure, offline location. Anyone with this key has full access to your funds.",
                                style = MaterialTheme.typography.bodySmall, color = SlateGray400, lineHeight = 18.sp)
                        }
                    }

                    // Public Key
                    Text("Public Key", style = MaterialTheme.typography.labelSmall, color = SlateGray500, fontWeight = FontWeight.Medium)
                    Surface(shape = RoundedCornerShape(8.dp), color = BackgroundDark) {
                        SelectionContainer {
                            Text(walletState.publicKey.ifBlank { "No wallet connected" },
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall, color = Primary,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }

                    // Private Key
                    Text("Private Key (Secret)", style = MaterialTheme.typography.labelSmall, color = Red500, fontWeight = FontWeight.Medium)
                    Surface(shape = RoundedCornerShape(8.dp), color = BackgroundDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Red500.copy(alpha = 0.2f))) {
                        SelectionContainer {
                            Text(walletState.privateKeyBase58.ifBlank { "No private key available" },
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall, color = White,
                                fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                    }

                    // Copy to Clipboard Button
                    if (walletState.privateKeyBase58.isNotBlank()) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(walletState.privateKeyBase58))
                                keyCopied = true
                                Toast.makeText(context, "🔑 Private key copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (keyCopied) EmeraldGreen.copy(alpha = 0.15f) else SurfaceDark,
                                contentColor = if (keyCopied) EmeraldGreen else Primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp,
                                if (keyCopied) EmeraldGreen.copy(alpha = 0.3f) else Primary.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                if (keyCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null, modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (keyCopied) "COPIED TO CLIPBOARD" else "COPY PRIVATE KEY",
                                fontWeight = FontWeight.Bold, fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false; keyCopied = false }) {
                    Text("CLOSE", color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Sign Out?", color = White, fontWeight = FontWeight.Bold) },
            text = { Text("This will disconnect your wallet and stop the GridLink node. Are you sure?",
                style = MaterialTheme.typography.bodyMedium, color = SlateGray400) },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onSignOut() }) {
                    Text("SIGN OUT", color = Red500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("CANCEL", color = SlateGray400)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                    Text("Settings", style = MaterialTheme.typography.titleLarge, color = White, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gridlink.depin/support"))
                    try { context.startActivity(intent) } catch (_: Exception) { }
                }) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = Primary)
                }
            }
            Divider(color = Primary.copy(alpha = 0.1f))
        }

        // General Preferences Section
        item {
            Text("GENERAL PREFERENCES", modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 16.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Primary,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        }

        item { SettingsToggleItem("Persistent Background Service", "Keep GridLink running while app is closed",
            persistentBackground) { viewModel.setPersistentBackground(it) } }
        item { SettingsToggleItem("Share on Wi-Fi Only", "Prevent mobile data usage",
            wifiOnly) { viewModel.setWifiOnly(it) } }
        item { SettingsToggleItem("Battery Saver Override", "Prioritize earning over battery life",
            batterySaver) { viewModel.setBatterySaver(it) } }

        // Usage Limits Section
        item {
            Text("USAGE LIMITS", modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 16.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Primary,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        }

        // Daily Data Limit Slider
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("Daily Data Limit", style = MaterialTheme.typography.titleMedium, color = White, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Maximum bandwidth to rent per day", style = MaterialTheme.typography.bodySmall, color = SlateGray400)
                        }
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)) { append("%.1f ".format(dailyLimit)) }
                            withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Primary)) { append("GB") }
                        })
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Slider(value = dailyLimit, onValueChange = { viewModel.setDailyDataLimitGb(it) },
                        valueRange = 0f..10f, steps = 19,
                        colors = SliderDefaults.colors(thumbColor = White, activeTrackColor = Primary, inactiveTrackColor = SlateGray700))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0 GB", style = MaterialTheme.typography.labelSmall, color = SlateGray400, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("5 GB", style = MaterialTheme.typography.labelSmall, color = SlateGray400, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("10 GB", style = MaterialTheme.typography.labelSmall, color = SlateGray400, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }

        // Notifications Section
        item {
            Text("NOTIFICATIONS", modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 16.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Primary,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        }

        item {
            SettingsToggleItem(
                title = "Earning Alerts",
                subtitle = "Push notifications for bandwidth earnings",
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->
                    viewModel.setNotificationsEnabled(enabled)
                    if (!enabled) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        try { context.startActivity(intent) } catch (_: Exception) { }
                    }
                }
            )
        }

        // Account & Security Section
        item {
            Text("ACCOUNT & SECURITY", modifier = Modifier.padding(start = 20.dp, top = 32.dp, bottom = 16.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp), color = Primary,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        }

        // Private Key Export
        item {
            SettingsNavItem(Icons.Outlined.Lock, "Private Key Export", SlateGray500) { showExportDialog = true }
        }

        // Notification Settings
        item {
            SettingsNavItem(Icons.Outlined.Notifications, "Notification Settings", SlateGray500) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                try { context.startActivity(intent) } catch (_: Exception) { }
            }
        }

        // Support
        item {
            SettingsNavItem(Icons.Outlined.HelpOutline, "Help & Support", SlateGray500) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gridlink.depin/support"))
                try { context.startActivity(intent) } catch (_: Exception) { }
            }
        }

        // Sign Out
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp), color = Color.Transparent,
                onClick = { showSignOutDialog = true }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Red500, modifier = Modifier.size(24.dp))
                    Text("Sign Out", style = MaterialTheme.typography.titleMedium, color = Red500, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SlateGray400)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = Primary,
                    uncheckedThumbColor = White, uncheckedTrackColor = SlateGray700,
                    uncheckedBorderColor = Color.Transparent, checkedBorderColor = Color.Transparent))
        }
    }
}

@Composable
fun SettingsNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, tint: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp), color = Color.Transparent, onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = White, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SlateGray400, modifier = Modifier.size(24.dp))
        }
    }
}
