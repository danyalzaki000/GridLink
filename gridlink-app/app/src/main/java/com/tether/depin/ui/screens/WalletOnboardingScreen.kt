package com.tether.depin.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tether.depin.ui.theme.*
import com.tether.depin.wallet.WalletConnectionManager

@Composable
fun WalletOnboardingScreen(
    walletManager: WalletConnectionManager,
    onConnected: () -> Unit
) {
    val context = LocalContext.current
    val walletState by walletManager.walletState.collectAsState()
    val newlyCreatedKey by walletManager.newlyCreatedKey.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    // Initialize encrypted prefs
    LaunchedEffect(Unit) {
        walletManager.initialize(context)
    }

    // Auto-navigate ONLY when connected AND no pending key backup
    LaunchedEffect(walletState.isConnected, newlyCreatedKey) {
        if (walletState.isConnected && newlyCreatedKey == null) {
            onConnected()
        }
    }

    // Import dialog state
    var showImportDialog by remember { mutableStateOf(false) }
    var importKeyText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "pulse"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "ring"
    )

    // ===== IMPORT WALLET DIALOG =====
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; importError = null; importKeyText = "" },
            containerColor = SurfaceDark, shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = Primary)
                    Text("Import Wallet", color = White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Paste your Solana Base58 private key below. It will be encrypted and stored securely on this device.",
                        style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                    OutlinedTextField(
                        value = importKeyText,
                        onValueChange = { importKeyText = it; importError = null },
                        label = { Text("Base58 Private Key", color = SlateGray500) },
                        placeholder = { Text("e.g. 5T2z7K...", color = SlateGray700) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary, unfocusedBorderColor = SlateGray700,
                            focusedTextColor = White, unfocusedTextColor = White, cursorColor = Primary
                        ),
                        singleLine = false, minLines = 2, maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (importError != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Red500.copy(alpha = 0.1f)) {
                            Text("⚠️ $importError", modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall, color = Red500)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importKeyText.isBlank()) { importError = "Please enter a private key"; return@Button }
                        walletManager.initialize(context)
                        val result = walletManager.importWallet(importKeyText)
                        if (result.isSuccess) {
                            showImportDialog = false; importKeyText = ""
                            Toast.makeText(context, "✅ Wallet imported successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            importError = result.exceptionOrNull()?.message ?: "Invalid private key"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(50), enabled = importKeyText.isNotBlank()
                ) { Text("IMPORT", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importError = null; importKeyText = "" }) {
                    Text("CANCEL", color = SlateGray400)
                }
            }
        )
    }

    // ===== PERSISTENT "SAVE YOUR KEY" DIALOG =====
    // Driven by ViewModel StateFlow — stays until user explicitly clicks "I HAVE SAVED IT"
    newlyCreatedKey?.let { keyInfo ->
        var copied by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { /* BLOCKED — user must explicitly acknowledge */ },
            containerColor = SurfaceDark, shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🚨", fontSize = 24.sp)
                    Text("Save Your Private Key", color = White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // WARNING BANNER
                    Surface(shape = RoundedCornerShape(12.dp), color = Red500.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Red500.copy(alpha = 0.25f))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("⚠️ THIS IS THE ONLY TIME THIS WILL BE SHOWN",
                                style = MaterialTheme.typography.labelMedium, color = Red500, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("If you lose this key, your USDC is gone forever. GridLink does not store this on any server.",
                                style = MaterialTheme.typography.bodySmall, color = SlateGray400, lineHeight = 18.sp)
                        }
                    }

                    // PUBLIC KEY
                    Text("Your Public Key", style = MaterialTheme.typography.labelSmall, color = SlateGray500, fontWeight = FontWeight.Medium)
                    Surface(shape = RoundedCornerShape(8.dp), color = BackgroundDark) {
                        SelectionContainer {
                            Text(keyInfo.publicKeyBase58, modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall, color = Primary,
                                fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }

                    // PRIVATE KEY
                    Text("Your Private Key (Secret)", style = MaterialTheme.typography.labelSmall, color = Red500, fontWeight = FontWeight.Medium)
                    Surface(shape = RoundedCornerShape(8.dp), color = BackgroundDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Red500.copy(alpha = 0.2f))) {
                        SelectionContainer {
                            Text(keyInfo.privateKeyBase58, modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall, color = White,
                                fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                    }

                    // COPY BUTTON
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(keyInfo.privateKeyBase58))
                            copied = true
                            Toast.makeText(context, "🔑 Private key copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copied) EmeraldGreen.copy(alpha = 0.15f) else SurfaceDark,
                            contentColor = if (copied) EmeraldGreen else Primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp,
                            if (copied) EmeraldGreen.copy(alpha = 0.3f) else Primary.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null, modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (copied) "COPIED TO CLIPBOARD" else "COPY TO CLIPBOARD",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        walletManager.acknowledgeKeyBackup()
                        // Navigation will happen automatically via LaunchedEffect
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I HAVE SAVED IT", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ===== MAIN SCREEN =====
    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.offset(x = (-60).dp, y = (-120).dp).size(200.dp)
            .clip(CircleShape).background(Primary.copy(alpha = 0.06f)).blur(64.dp))
        Box(modifier = Modifier.offset(x = 80.dp, y = 100.dp).size(160.dp)
            .clip(CircleShape).background(Primary.copy(alpha = 0.04f)).blur(48.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Animated logo
            Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Primary.copy(alpha = 0.06f * (1.3f - ringScale).coerceAtLeast(0f)),
                        radius = 80.dp.toPx() * ringScale.coerceIn(0.5f, 1.3f),
                        center = Offset(size.width / 2, size.height / 2))
                    drawCircle(color = Primary.copy(alpha = 0.1f), radius = 50.dp.toPx(),
                        center = Offset(size.width / 2, size.height / 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                    drawCircle(color = Primary.copy(alpha = pulseAlpha), radius = 30.dp.toPx(),
                        center = Offset(size.width / 2, size.height / 2))
                }
                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(40.dp), tint = BackgroundDark)
            }

            Spacer(modifier = Modifier.height(40.dp))
            Text("Welcome to GridLink", style = MaterialTheme.typography.headlineLarge,
                color = White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Create or import a Solana wallet\nto start earning by sharing bandwidth",
                style = MaterialTheme.typography.bodyLarge, color = SlateGray400,
                textAlign = TextAlign.Center, lineHeight = 24.sp)
            Spacer(modifier = Modifier.height(56.dp))

            // IMPORT EXISTING WALLET
            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth().height(58.dp).shadow(elevation = 16.dp,
                    shape = RoundedCornerShape(50), ambientColor = Primary.copy(alpha = 0.3f),
                    spotColor = Primary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("IMPORT EXISTING WALLET", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // CREATE NEW WALLET
            OutlinedButton(
                onClick = {
                    walletManager.initialize(context)
                    walletManager.createNewWallet()
                    // Dialog appears automatically via newlyCreatedKey StateFlow
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(2.dp, Primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("CREATE NEW WALLET", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(shape = RoundedCornerShape(50), color = Primary.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.15f))) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldGreen))
                    Text("Solana Devnet", style = MaterialTheme.typography.labelSmall,
                        color = SlateGray400, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Embedded wallet • Ed25519 • Encrypted storage",
                style = MaterialTheme.typography.bodySmall, color = SlateGray500, fontSize = 12.sp)
        }
    }
}
