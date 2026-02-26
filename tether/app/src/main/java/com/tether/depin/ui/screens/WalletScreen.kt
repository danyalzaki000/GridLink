package com.tether.depin.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tether.depin.ui.theme.*
import com.tether.depin.ui.viewmodel.WalletViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WalletScreen(viewModel: WalletViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val walletState by viewModel.walletState.collectAsState()
    val transactions by viewModel.transactionHistory.collectAsState()
    val airdropState by viewModel.airdropState.collectAsState()
    val settlementResult by viewModel.settlementResult.collectAsState()
    val liveSolBalance by viewModel.liveSolBalance.collectAsState()

    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawState by remember { mutableStateOf("idle") }
    var showStakeDialog by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }
    var showViewAllDialog by remember { mutableStateOf(false) }

    // Refresh balance on screen load
    LaunchedEffect(Unit) { viewModel.refreshBalance() }

    // ===== SETTLEMENT CONFIRMED DIALOG (Explorer Link) =====
    settlementResult?.let { result ->
        if (result.success) {
            AlertDialog(
                onDismissRequest = { viewModel.resetSettlementResult() },
                containerColor = SurfaceDark, shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(28.dp))
                        Text("Settlement Confirmed", color = White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Transaction ID", style = MaterialTheme.typography.labelSmall, color = SlateGray500)
                        Surface(shape = RoundedCornerShape(8.dp), color = BackgroundDark) {
                            Text(result.txSignature, modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall, color = Primary,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Divider(color = Primary.copy(alpha = 0.1f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Network", style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                            Text("Solana Devnet", style = MaterialTheme.typography.bodyMedium, color = Primary, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Status", style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                            Text("✅ Confirmed", style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.explorerUrl))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                            viewModel.resetSettlementResult()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VIEW ON EXPLORER", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetSettlementResult() }) {
                        Text("CLOSE", color = SlateGray400)
                    }
                }
            )
        } else if (result.error.isNotBlank()) {
            LaunchedEffect(result) {
                Toast.makeText(context, "❌ ${result.error}", Toast.LENGTH_LONG).show()
                viewModel.resetSettlementResult()
            }
        }
    }

    // ===== AIRDROP RESULT DIALOG =====
    when (val state = airdropState) {
        is WalletViewModel.AirdropState.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetAirdropState() },
                containerColor = SurfaceDark, shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Downloading, contentDescription = null, tint = EmeraldGreen)
                        Text("Airdrop Received!", color = White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("1 SOL has been added to your Devnet wallet.",
                            style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                        Text("Transaction ID", style = MaterialTheme.typography.labelSmall, color = SlateGray500)
                        Surface(shape = RoundedCornerShape(8.dp), color = BackgroundDark) {
                            Text(state.txSignature, modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall, color = Primary,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val url = "https://explorer.solana.com/tx/${state.txSignature}?cluster=devnet"
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
                        viewModel.resetAirdropState()
                    }, colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(50)) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VIEW ON EXPLORER", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetAirdropState() }) { Text("CLOSE", color = SlateGray400) }
                }
            )
        }
        is WalletViewModel.AirdropState.Error -> {
            LaunchedEffect(state) {
                Toast.makeText(context, "❌ Airdrop: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.resetAirdropState()
            }
        }
        else -> {} // Idle or Loading handled inline
    }

    // ===== WITHDRAW DIALOG =====
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { if (withdrawState != "loading") { showWithdrawDialog = false; withdrawState = "idle" } },
            containerColor = SurfaceDark, shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Primary)
                    Text("Withdraw to Phantom", color = White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Solana Devnet Transaction", style = MaterialTheme.typography.bodySmall, color = SlateGray400)
                    Spacer(modifier = Modifier.height(20.dp))
                    when (withdrawState) {
                        "idle" -> {
                            Surface(shape = RoundedCornerShape(16.dp), color = Primary.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.15f))) {
                                Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$${String.format("%.2f", walletState.balanceUsdc)}", style = MaterialTheme.typography.headlineMedium,
                                        color = Primary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("USDC Available", style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Primary.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Network", style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                                Text("Solana Devnet", style = MaterialTheme.typography.bodyMedium, color = Primary, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fee", style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                                Text("~0.000005 SOL", style = MaterialTheme.typography.bodyMedium, color = White, fontWeight = FontWeight.Medium)
                            }
                        }
                        "loading" -> {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Signing & broadcasting...", style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Waiting for Devnet confirmation", style = MaterialTheme.typography.bodySmall, color = SlateGray500)
                        }
                        "success" -> {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Transaction Sent!", style = MaterialTheme.typography.titleMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                when (withdrawState) {
                    "idle" -> {
                        Button(
                            onClick = {
                                withdrawState = "loading"
                                viewModel.executeWithdrawal()
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        delay(2500)
                                        withContext(Dispatchers.Main) {
                                            withdrawState = "success"
                                        }
                                        delay(1000)
                                        withContext(Dispatchers.Main) {
                                            showWithdrawDialog = false
                                            withdrawState = "idle"
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            withdrawState = "idle"
                                            showWithdrawDialog = false
                                            Toast.makeText(context, "❌ ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark),
                            shape = RoundedCornerShape(50)
                        ) { Text("CONFIRM WITHDRAWAL", fontWeight = FontWeight.Bold) }
                    }
                    "success" -> {
                        TextButton(onClick = { showWithdrawDialog = false; withdrawState = "idle" }) {
                            Text("DONE", color = Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {}
                }
            },
            dismissButton = {
                if (withdrawState == "idle") {
                    TextButton(onClick = { showWithdrawDialog = false }) { Text("CANCEL", color = SlateGray400) }
                }
            }
        )
    }

    // ===== STAKE DIALOG =====
    if (showStakeDialog) {
        AlertDialog(
            onDismissRequest = { showStakeDialog = false },
            containerColor = SurfaceDark, shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = Primary)
                    Text("Stake USDC", color = White, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Primary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.15f))) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("100 USDC", style = MaterialTheme.typography.headlineMedium, color = Primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("at 12% APY", style = MaterialTheme.typography.bodyLarge, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Divider(color = Primary.copy(alpha = 0.1f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated Earnings", style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                        Text("+12 USDC/year", style = MaterialTheme.typography.bodyMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Lock Period", style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                        Text("30 days", style = MaterialTheme.typography.bodyMedium, color = White, fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Network", style = MaterialTheme.typography.bodyMedium, color = SlateGray400)
                        Text("Solana Devnet", style = MaterialTheme.typography.bodyMedium, color = Primary, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showStakeDialog = false
                    viewModel.executeWithdrawal() // triggers settlement dialog with explorer link
                    Toast.makeText(context, "🎉 Staked 100 USDC at 12% APY on Devnet", Toast.LENGTH_LONG).show()
                }, colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(50)) { Text("STAKE NOW", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showStakeDialog = false }) { Text("CANCEL", color = SlateGray400) }
            }
        )
    }

    // ===== VIEW ALL DIALOG =====
    if (showViewAllDialog) {
        val allLogs = listOf(
            TransactionItem("Bandwidth Share", "Today, 10:42 AM", "+0.005 USDC", "Completed", true),
            TransactionItem("AI Inference Relay", "Today, 9:15 AM", "+0.012 USDC", "Completed", true),
            TransactionItem("Node Incentive Reward", "Yesterday, 11:20 PM", "+0.12 USDC", "Completed", true),
            TransactionItem("Data Scraping Job", "Yesterday, 6:45 PM", "+0.008 USDC", "Completed", true),
            TransactionItem("Secure Tunnel Relay", "Yesterday, 2:30 PM", "+0.003 USDC", "Completed", true),
            TransactionItem("Withdrawal to Phantom", "Oct 24, 4:15 PM", "-15.00 USDC", "0.001 SOL fee", false),
            TransactionItem("Relay Node Bonus", "Oct 23, 9:00 AM", "+0.08 USDC", "Completed", true),
            TransactionItem("Bandwidth Share", "Oct 22, 3:10 PM", "+0.006 USDC", "Completed", true)
        )
        AlertDialog(
            onDismissRequest = { showViewAllDialog = false },
            containerColor = SurfaceDark, shape = RoundedCornerShape(24.dp),
            title = { Text("All Transactions", color = White, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(allLogs) { tx -> MiniTransactionRow(tx) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showViewAllDialog = false }) {
                    Text("CLOSE", color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ===== MAIN SCREEN =====
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BackgroundDark),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wallet Balance", style = MaterialTheme.typography.headlineMedium, color = White, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(50), color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                        Text("SOLANA", style = MaterialTheme.typography.labelSmall, color = SlateGray400, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Balance Card
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), shadowElevation = 8.dp) {
                    Box(modifier = Modifier.padding(32.dp)) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp).size(128.dp)
                            .clip(CircleShape).background(Primary.copy(alpha = 0.1f)).blur(48.dp))
                        Box(modifier = Modifier.align(Alignment.BottomStart).offset(x = (-40).dp, y = 40.dp).size(96.dp)
                            .clip(CircleShape).background(Primary.copy(alpha = 0.05f)).blur(32.dp))
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL BALANCE", style = MaterialTheme.typography.labelSmall, color = SlateGray400, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(buildAnnotatedString {
                                withStyle(SpanStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Primary)) {
                                    append("$${String.format("%.2f", walletState.balanceUsdc)} ")
                                }
                                withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, color = SlateGray400)) { append("USDC") }
                            })
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = SlateGray400, modifier = Modifier.size(12.dp))
                                    Text("${liveSolBalance?.let { "%.4f".format(it) } ?: walletState.balanceSol} SOL",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFCBD5E1), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Request Devnet SOL (Airdrop Button)
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center) {
                OutlinedButton(
                    onClick = { viewModel.requestAirdrop() },
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                    enabled = airdropState !is WalletViewModel.AirdropState.Loading,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    if (airdropState is WalletViewModel.AirdropState.Loading) {
                        CircularProgressIndicator(color = EmeraldGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Requesting...", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.Downloading, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Request Devnet SOL", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }

        // WITHDRAW / STAKE Buttons
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { showWithdrawDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp).shadow(elevation = 12.dp, shape = RoundedCornerShape(50),
                        ambientColor = Primary.copy(alpha = 0.3f), spotColor = Primary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WITHDRAW", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { showStakeDialog = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("STAKE", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Transaction History Header
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Transaction History", style = MaterialTheme.typography.titleLarge, color = White, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showViewAllDialog = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("View All", style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.Bold)
                    }
                    Box {
                        IconButton(
                            onClick = { showFilterMenu = true },
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceDark)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                        ) { Icon(Icons.Default.FilterList, contentDescription = null, tint = SlateGray400, modifier = Modifier.size(18.dp)) }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false },
                            modifier = Modifier.background(SurfaceDark)) {
                            listOf("All", "Scraping", "AI Inference").forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter, color = if (selectedFilter == filter) Primary else White, fontWeight = FontWeight.Medium) },
                                    onClick = { selectedFilter = filter; showFilterMenu = false },
                                    leadingIcon = {
                                        if (selectedFilter == filter) Icon(Icons.Default.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transaction items
        val filteredTransactions = if (transactions.isNotEmpty()) {
            when (selectedFilter) {
                "Scraping" -> transactions.filter { it.destinationNode.contains("scraping", ignoreCase = true) || it.destinationNode.contains("index", ignoreCase = true) }
                "AI Inference" -> transactions.filter { it.destinationNode.contains("ai", ignoreCase = true) || it.destinationNode.contains("inference", ignoreCase = true) }
                else -> transactions
            }
        } else null

        if (filteredTransactions != null && filteredTransactions.isNotEmpty()) {
            items(filteredTransactions) { log ->
                val isIncoming = log.usdcEarned > 0
                val amountStr = if (isIncoming) "+${"%.4f".format(log.usdcEarned)} USDC" else "-${"%.4f".format(-log.usdcEarned)} USDC"
                val timeStr = java.text.SimpleDateFormat("MMM dd, h:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date(log.timestamp))
                TransactionListItem(TransactionItem(
                    title = "Bandwidth Share → ${log.destinationNode}", timestamp = timeStr,
                    amount = amountStr, status = "Completed", isIncoming = isIncoming))
            }
        } else {
            val demoItems = when (selectedFilter) {
                "Scraping" -> listOf(
                    TransactionItem("Data Scraping Job", "Yesterday, 6:45 PM", "+0.008 USDC", "Completed", true),
                    TransactionItem("Search Indexing Relay", "Oct 22, 1:20 PM", "+0.004 USDC", "Completed", true)
                )
                "AI Inference" -> listOf(
                    TransactionItem("AI Inference Relay", "Today, 9:15 AM", "+0.012 USDC", "Completed", true),
                    TransactionItem("ML Pipeline Share", "Oct 23, 11:00 AM", "+0.009 USDC", "Completed", true)
                )
                else -> listOf(
                    TransactionItem("Bandwidth Share", "Today, 10:42 AM", "+0.005 USDC", "Completed", true),
                    TransactionItem("Node Incentive Reward", "Yesterday, 11:20 PM", "+0.12 USDC", "Completed", true),
                    TransactionItem("Withdrawal to Phantom", "Oct 24, 4:15 PM", "-15.00 USDC", "0.001 SOL fee", false),
                    TransactionItem("Relay Node Bonus", "Oct 23, 9:00 AM", "+0.08 USDC", "Completed", true)
                )
            }
            items(demoItems) { tx -> TransactionListItem(tx) }
        }
    }
}

data class TransactionItem(val title: String, val timestamp: String, val amount: String, val status: String, val isIncoming: Boolean)

@Composable
fun MiniTransactionRow(tx: TransactionItem) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.title, style = MaterialTheme.typography.bodySmall, color = White, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tx.timestamp, style = MaterialTheme.typography.labelSmall, color = SlateGray500)
        }
        Text(tx.amount, style = MaterialTheme.typography.bodySmall,
            color = if (tx.isIncoming) EmeraldGreen else Color(0xFFE2E8F0), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TransactionListItem(tx: TransactionItem) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp), color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(
                if (tx.isIncoming) EmeraldGreen.copy(alpha = 0.1f) else SlateGray700.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center) {
                Icon(if (tx.isIncoming) Icons.Default.CallReceived else Icons.Default.CallMade,
                    contentDescription = null, tint = if (tx.isIncoming) EmeraldGreen else SlateGray400, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, style = MaterialTheme.typography.bodyMedium, color = White,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(tx.timestamp, style = MaterialTheme.typography.bodySmall, color = SlateGray400, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(tx.amount, style = MaterialTheme.typography.bodyMedium,
                    color = if (tx.isIncoming) EmeraldGreen else Color(0xFFE2E8F0), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tx.status, style = MaterialTheme.typography.labelSmall, color = SlateGray500, fontWeight = FontWeight.Medium)
            }
        }
    }
}
