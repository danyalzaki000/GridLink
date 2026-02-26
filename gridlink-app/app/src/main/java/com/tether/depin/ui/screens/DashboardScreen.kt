package com.tether.depin.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tether.depin.service.TetherNodeService
import com.tether.depin.ui.theme.*
import com.tether.depin.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val nodeStats by viewModel.nodeStats.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()
    val isProxyRunning by viewModel.isProxyRunning.collectAsState()
    val uptimeSeconds by viewModel.uptimeSeconds.collectAsState()
    val context = LocalContext.current

    var showViewAllDialog by remember { mutableStateOf(false) }

    // View All Dialog
    if (showViewAllDialog) {
        val allActivities = listOf(
            ActivityItem("Encrypted Packet Routed", "2 mins ago • 45 MB", "+0.02 USDC", Icons.Outlined.EnhancedEncryption),
            ActivityItem("Connection Verified", "Local grid node", "Success", Icons.Outlined.VerifiedUser),
            ActivityItem("Traffic Relay", "15 mins ago • 120 MB", "+0.05 USDC", Icons.Outlined.SwapHoriz),
            ActivityItem("AI Inference Relay", "30 mins ago • 80 MB", "+0.03 USDC", Icons.Outlined.Psychology),
            ActivityItem("Data Scraping Job", "1 hour ago • 200 MB", "+0.08 USDC", Icons.Outlined.Storage),
            ActivityItem("Bandwidth Share", "2 hours ago • 60 MB", "+0.02 USDC", Icons.Outlined.SwapHoriz),
            ActivityItem("Secure Tunnel Relay", "3 hours ago • 150 MB", "+0.06 USDC", Icons.Outlined.Security),
            ActivityItem("Node Incentive Reward", "Yesterday • Bonus", "+0.12 USDC", Icons.Outlined.CardGiftcard)
        )
        AlertDialog(
            onDismissRequest = { showViewAllDialog = false },
            containerColor = SurfaceDark, shape = RoundedCornerShape(24.dp),
            title = { Text("All Activity", color = White, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(allActivities) { item -> ActivityListItem(item) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showViewAllDialog = false }) {
                    Text("CLOSE", color = Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Pulse animation for NODE ACTIVE badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Glow animation for button
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Calculate progress from live Room DB data
    val totalBytesGB = nodeStats.totalBytesShared.toDouble() / (1024.0 * 1024.0 * 1024.0)
    val totalBytesMB = nodeStats.totalBytesShared.toDouble() / (1024.0 * 1024.0)
    val displayValue = if (totalBytesGB >= 1.0) "%.1f".format(totalBytesGB) else "%.0f".format(totalBytesMB)
    val displayUnit = if (totalBytesGB >= 1.0) "GB" else "MB"
    val displayEarned = nodeStats.totalEarnedUsdc
    val progress = (totalBytesGB / 10.0).coerceIn(0.0, 1.0).toFloat()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "GridLink Node",
                    style = MaterialTheme.typography.headlineMedium,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                // Node Active Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Primary.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp)) {
                            // Ping animation
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = pulseAlpha * 0.75f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Primary)
                            )
                        }
                        Text(
                            if (isProxyRunning) "NODE ACTIVE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Circular Progress Dial
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(256.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.08f))
                            .blur(24.dp)
                    )

                    // SVG-like circular progress
                    Canvas(modifier = Modifier.size(256.dp)) {
                        val strokeWidth = 12.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // Background circle
                        drawCircle(
                            color = SurfaceDark,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )

                        // Progress arc
                        val sweepAngle = 360f * progress
                        drawArc(
                            color = Primary,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(
                                center.x - radius,
                                center.y - radius
                            ),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // Center content
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "DATA SHARED",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGray400,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, color = White)) {
                                    append("$displayValue ")
                                }
                                withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal, color = SlateGray400)) {
                                    append(displayUnit)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(1.dp)
                                .background(SlateGray700)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "TOTAL EARNED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SlateGray400,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)) {
                                    append("$${String.format("%.2f", displayEarned)} ")
                                }
                                withStyle(SpanStyle(fontSize = 14.sp, color = Primary)) {
                                    append("USDC")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // STOP/START PROXY Button
                Button(
                    onClick = {
                        if (isProxyRunning) {
                            context.startService(
                                Intent(context, TetherNodeService::class.java).apply {
                                    action = TetherNodeService.ACTION_STOP
                                }
                            )
                            viewModel.setProxyRunning(false)
                        } else {
                            context.startForegroundService(
                                Intent(context, TetherNodeService::class.java)
                            )
                            viewModel.setProxyRunning(true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = Primary.copy(alpha = glowAlpha),
                            spotColor = Primary.copy(alpha = glowAlpha)
                        ),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = BackgroundDark
                    )
                ) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isProxyRunning) "STOP PROXY" else "START PROXY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Stats Grid
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Uptime Card
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "UPTIME",
                    value = if (isProxyRunning) viewModel.formatUptime(uptimeSeconds) else "0h 0m",
                    subtitle = if (isProxyRunning) "Session Active" else "Inactive",
                    subtitleColor = if (isProxyRunning) EmeraldGreen else SlateGray500,
                    icon = Icons.Outlined.Timer
                )
                // Quality Card
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "QUALITY",
                    value = "98%",
                    subtitle = "Excellent",
                    subtitleColor = EmeraldGreen,
                    icon = Icons.Outlined.SignalCellularAlt
                )
            }
        }

        // Recent Activity Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showViewAllDialog = true }) {
                    Text("View All", color = Primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Live recent activity from Room DB
        if (recentLogs.isNotEmpty()) {
            items(recentLogs) { log ->
                val timeAgo = viewModel.formatTimeAgo(log.timestamp)
                ActivityListItem(
                    ActivityItem(
                        title = "Traffic Relay",
                        subtitle = "$timeAgo • ${"%.0f".format(log.mbTransferred)} MB → ${log.destinationNode}",
                        value = "+${"%.4f".format(log.usdcEarned)} USDC",
                        icon = Icons.Outlined.SwapHoriz
                    )
                )
            }
        } else {
            // Fallback demo items when DB is empty
            val demoActivities = listOf(
                ActivityItem("Encrypted Packet Routed", "2 mins ago • 45 MB", "+0.02 USDC", Icons.Outlined.EnhancedEncryption),
                ActivityItem("Connection Verified", "Local grid node", "Success", Icons.Outlined.VerifiedUser),
                ActivityItem("Traffic Relay", "15 mins ago • 120 MB", "+0.05 USDC", Icons.Outlined.SwapHoriz)
            )
            items(demoActivities) { activity ->
                ActivityListItem(activity)
            }
        }
    }
}

data class ActivityItem(
    val title: String,
    val subtitle: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ActivityListItem(item: ActivityItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGray400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Value
            Text(
                item.value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.value.startsWith("+")) EmeraldGreen else SlateGray400,
                fontWeight = if (item.value.startsWith("+")) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    subtitleColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box {
            // Subtle glow orb
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-16).dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.05f))
                    .blur(20.dp)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGray400,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    color = White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}
