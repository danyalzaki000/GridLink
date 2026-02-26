package com.tether.depin.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.tether.depin.ui.viewmodel.NetworkViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ProxyTrafficItem(
    val title: String,
    val subtitle: String,
    val tag: String?,
    val speed: String,
    val color: Color,
    val progress: Float,
    val clientCompany: String,
    val clientLocation: String,
    val jobType: String
)

@Composable
fun NetworkScreen(viewModel: NetworkViewModel = viewModel()) {
    val neighborhoodName by viewModel.neighborhoodName.collectAsState()
    val latencyMs by viewModel.latencyMs.collectAsState()
    val activePeers by viewModel.activePeers.collectAsState()
    val signalQuality by viewModel.signalQuality.collectAsState()
    val uptimeSeconds by viewModel.uptimeSeconds.collectAsState()
    val isNodeActive by viewModel.isNodeActive.collectAsState()

    var selectedTrafficItem by remember { mutableStateOf<ProxyTrafficItem?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "network")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseScale"
    )
    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart),
        label = "dashOffset"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase"
    )
    val particleT by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "particleT"
    )

    val trafficItems = remember {
        listOf(
            ProxyTrafficItem("AI Inference Request", "San Francisco, US • Port 8080", "GPT-4", "2.4", IndigoAccent, 0.7f,
                "Apex Data Logistics", "San Francisco AWS US-West-2", "High-Frequency ML Inference Pipeline"),
            ProxyTrafficItem("Data Scraping Job", "London, UK • Port 443", "Idx", "1.1", PurpleAccent, 0.45f,
                "Meridian Analytics Ltd.", "London GCP Europe-West2", "High-Frequency Search Indexing"),
            ProxyTrafficItem("Secure Tunnel", "Singapore, SG • Port 1194", null, "0.8", AmberAccent, 0.25f,
                "SecureNet Asia Pacific", "Singapore AWS AP-Southeast-1", "Encrypted VPN Tunnel Relay")
        )
    }

    // Traffic detail dialog
    selectedTrafficItem?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedTrafficItem = null },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(item.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                item.title.contains("AI") -> Icons.Default.SmartToy
                                item.title.contains("Scraping") -> Icons.Default.Dataset
                                else -> Icons.Default.VpnLock
                            },
                            contentDescription = null, tint = item.color, modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(item.title, color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Divider(color = Primary.copy(alpha = 0.1f))
                    DetailRow("Client Company", item.clientCompany, Primary)
                    DetailRow("Client Location", item.clientLocation, IndigoAccent)
                    DetailRow("Job Type", item.jobType, PurpleAccent)
                    DetailRow("Transfer Speed", "${item.speed} MB/s", EmeraldGreen)
                    DetailRow("Connection", item.subtitle, SlateGray400)

                    // Mini progress
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("JOB COMPLETION", style = MaterialTheme.typography.labelSmall, color = SlateGray500, letterSpacing = 1.sp)
                    Box(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(SlateGray700)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(item.progress).clip(RoundedCornerShape(50)).background(
                                Brush.horizontalGradient(listOf(item.color.copy(alpha = 0.7f), item.color))
                            )
                        )
                    }
                    Text(
                        "${(item.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall, color = item.color, fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTrafficItem = null }) {
                    Text("CLOSE", color = Primary, fontWeight = FontWeight.Bold)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Network Map", style = MaterialTheme.typography.headlineMedium, color = White, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isNodeActive) Primary.copy(alpha = 0.1f) else Red500.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isNodeActive) Primary.copy(alpha = 0.2f) else Red500.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp)) {
                            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(
                                if (isNodeActive) Primary.copy(alpha = pulseAlpha * 0.75f) else Red500.copy(alpha = 0.5f)
                            ))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isNodeActive) Primary else Red500))
                        }
                        Text(
                            if (isNodeActive) "ONLINE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNodeActive) Primary else Red500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Geo-map
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0D1B1B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.15f)),
                    shadowElevation = 20.dp
                ) {
                    Box {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width; val h = size.height
                            val centerX = w * 0.5f; val centerY = h * 0.5f

                            // Contour lines
                            val contourColors = listOf(Primary.copy(0.03f), Primary.copy(0.05f), Primary.copy(0.04f), Primary.copy(0.06f), Primary.copy(0.03f))
                            for (i in contourColors.indices) {
                                val contourPath = Path()
                                val yBase = h * (0.15f + i * 0.16f)
                                contourPath.moveTo(0f, yBase)
                                var x = 0f
                                while (x <= w) {
                                    val y = yBase + sin((x / w * 3f * PI + wavePhase + i * 0.8f).toFloat()) * 18f +
                                            cos((x / w * 5f * PI + wavePhase * 0.7f + i * 1.2f).toFloat()) * 10f
                                    contourPath.lineTo(x, y); x += 3f
                                }
                                drawPath(contourPath, contourColors[i], style = Stroke(1.5f))
                            }

                            // Grid
                            val gridColor = Primary.copy(0.04f)
                            for (i in 1 until 8) { drawLine(gridColor, Offset(w * i / 8, 0f), Offset(w * i / 8, h), 0.5f) }
                            for (i in 1 until 8) { drawLine(gridColor, Offset(0f, h * i / 8), Offset(w, h * i / 8), 0.5f) }

                            // Zone rings
                            listOf(50.dp.toPx(), 100.dp.toPx(), 150.dp.toPx()).forEachIndexed { idx, r ->
                                drawCircle(Primary.copy(0.05f - idx * 0.01f), r, Offset(centerX, centerY),
                                    style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()), dashOffset * 0.3f)))
                            }

                            // Peers + connections
                            val peers = listOf(Offset(w * 0.25f, h * 0.22f), Offset(w * 0.72f, h * 0.38f), Offset(w * 0.35f, h * 0.75f), Offset(w * 0.82f, h * 0.7f))
                            val grayNodes = listOf(Offset(w * 0.12f, h * 0.45f), Offset(w * 0.88f, h * 0.22f))
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), dashOffset)

                            peers.forEach { peer ->
                                drawLine(Primary.copy(0.06f), Offset(centerX, centerY), peer, 3.dp.toPx(), pathEffect = dashEffect)
                                drawLine(Primary.copy(0.15f), Offset(centerX, centerY), peer, 1.dp.toPx(), pathEffect = dashEffect)
                            }
                            grayNodes.forEach { gn -> drawLine(SlateGray700.copy(0.3f), peers[0], gn, 0.5.dp.toPx()) }

                            // Particles
                            peers.forEachIndexed { idx, peer ->
                                val t = ((particleT + idx * 0.25f) % 1f)
                                drawCircle(Primary.copy(0.8f * (1f - t)), 3.dp.toPx() * (1f - t * 0.5f),
                                    Offset(centerX + (peer.x - centerX) * t, centerY + (peer.y - centerY) * t))
                            }

                            grayNodes.forEach { drawCircle(SlateGray500.copy(0.4f), 3.dp.toPx(), it) }

                            // Peer nodes
                            peers.forEachIndexed { index, peer ->
                                val pa = ((pulseScale + index * 0.3f) % 1f)
                                drawCircle(Primary.copy(0.08f * (1f - pa)), 16.dp.toPx() * (1f + pa * 0.4f), peer)
                                drawCircle(Primary.copy(0.15f), 8.dp.toPx(), peer)
                                drawCircle(Primary.copy(0.7f), 4.dp.toPx(), peer)
                                drawCircle(Primary, 2.dp.toPx(), peer)
                            }

                            // Center node
                            drawCircle(Primary.copy(0.04f * (1.2f - pulseScale).coerceAtLeast(0f)),
                                55.dp.toPx() * pulseScale.coerceIn(0.3f, 1.2f), Offset(centerX, centerY))
                            drawCircle(Primary.copy(0.06f), 35.dp.toPx(), Offset(centerX, centerY), style = Stroke(1.dp.toPx()))
                            drawCircle(brush = Brush.radialGradient(listOf(Primary.copy(0.25f), Color.Transparent), Offset(centerX, centerY), 25.dp.toPx()),
                                radius = 25.dp.toPx(), center = Offset(centerX, centerY))
                            drawCircle(Primary.copy(0.6f), 14.dp.toPx(), Offset(centerX, centerY), style = Stroke(2.5.dp.toPx()))
                            drawCircle(Primary, 9.dp.toPx(), Offset(centerX, centerY))
                            drawCircle(White.copy(0.7f), 3.dp.toPx(), Offset(centerX, centerY))
                        }

                        // Routing chip
                        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF0A1616).copy(alpha = 0.9f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.25f)),
                                shadowElevation = 12.dp
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Hub, contentDescription = null, tint = Primary, modifier = Modifier.size(14.dp))
                                    Text("Routing via $neighborhoodName Grid", style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // YOU label
                        Box(modifier = Modifier.align(Alignment.Center).offset(y = 32.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Primary.copy(0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(0.2f))) {
                                Text("YOU", modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        // Stats Grid: 4 cards (live VPS data)
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NetworkStatCard(Modifier.weight(1f), "LATENCY", "$latencyMs", "ms",
                    if (latencyMs < 50) EmeraldGreen else AmberAccent, signalQuality, Icons.Outlined.Speed)
                NetworkStatCard(Modifier.weight(1f), "PEERS", "$activePeers", "",
                    EmeraldGreen, "Synced", Icons.Outlined.ShareLocation)
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 4.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NetworkStatCard(Modifier.weight(1f), "QUALITY", signalQuality, "",
                    if (signalQuality == "Excellent" || signalQuality == "Good") EmeraldGreen else AmberAccent, "VPS Live", Icons.Outlined.SignalCellularAlt)
                NetworkStatCard(Modifier.weight(1f), "UPTIME", viewModel.formatUptime(), "",
                    EmeraldGreen, "Active", Icons.Outlined.Timer)
            }
        }

        // Live Proxy Traffic Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Proxy Traffic", style = MaterialTheme.typography.titleLarge, color = White, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(8.dp), color = Primary.copy(alpha = 0.1f)) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Primary))
                        Text("Live", style = MaterialTheme.typography.bodySmall, color = Primary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Clickable traffic items
        items(trafficItems) { item ->
            ProxyTrafficListItem(item = item, onClick = { selectedTrafficItem = item })
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, accentColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = SlateGray500, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ProxyTrafficListItem(item: ProxyTrafficItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp), color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(item.color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(
                    when {
                        item.title.contains("AI") -> Icons.Default.SmartToy
                        item.title.contains("Scraping") -> Icons.Default.Dataset
                        else -> Icons.Default.VpnLock
                    },
                    contentDescription = null, tint = item.color, modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, color = White, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (item.tag != null) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.05f))) {
                            Text(item.tag, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, color = SlateGray400, fontSize = 10.sp)
                        }
                    }
                }
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = SlateGray400, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White, fontFamily = FontFamily.Monospace)) { append(item.speed) }
                    withStyle(SpanStyle(fontSize = 12.sp, color = SlateGray500)) { append(" MB/s") }
                })
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.width(64.dp).height(4.dp).clip(RoundedCornerShape(50)).background(SlateGray700)) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(item.progress).clip(RoundedCornerShape(50)).background(item.color))
                }
            }
        }
    }
}

@Composable
fun NetworkStatCard(modifier: Modifier = Modifier, title: String, value: String, unit: String,
                    statusDot: Color, statusText: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.05f))) {
        Box {
            Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 16.dp, y = (-16).dp).size(64.dp)
                .clip(CircleShape).background(Primary.copy(0.05f)).blur(20.dp))
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(title, style = MaterialTheme.typography.labelSmall, color = SlateGray400, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
                    Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = White)) { append(value) }
                    if (unit.isNotEmpty()) { withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SlateGray400)) { append(" $unit") } }
                })
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusDot))
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusDot, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            }
        }
    }
}
