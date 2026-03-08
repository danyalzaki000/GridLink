package com.tether.depin.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.tether.depin.ui.theme.*

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
    )
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

    val primaryColor = android.graphics.Color.parseColor("#0DF2F2")
    val primaryDark = android.graphics.Color.parseColor("#0ABFBF")
    val bgColor = android.graphics.Color.parseColor("#0A1616")

    for (x in 0 until size) {
        for (y in 0 until size) {
            if (matrix.get(x, y)) {
                // Slight gradient: top rows brighter, bottom rows dimmer
                val blend = y.toFloat() / size
                val r = ((1 - blend) * android.graphics.Color.red(primaryColor) + blend * android.graphics.Color.red(primaryDark)).toInt()
                val g = ((1 - blend) * android.graphics.Color.green(primaryColor) + blend * android.graphics.Color.green(primaryDark)).toInt()
                val b = ((1 - blend) * android.graphics.Color.blue(primaryColor) + blend * android.graphics.Color.blue(primaryDark)).toInt()
                bitmap.setPixel(x, y, android.graphics.Color.rgb(r, g, b))
            } else {
                bitmap.setPixel(x, y, bgColor)
            }
        }
    }
    return bitmap
}

@Composable
fun InviteScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val referralCode = "TETHER-NODE-88X"
    val qrContent = "https://limewire.com/d/gz3V1#bcmTeZUzIw"
    val shareText = "Turn your phone into a DePIN proxy node. Join my GridLink grid to earn SOL: https://gridlink.network/invite/88X"

    val qrBitmap = remember { generateQrBitmap(qrContent).asImageBitmap() }

    val infiniteTransition = rememberInfiniteTransition(label = "invite")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "glow"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "ring"
    )

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Background glow
        Box(modifier = Modifier.offset(x = 100.dp, y = (-40).dp).size(250.dp).clip(CircleShape)
            .background(Primary.copy(alpha = 0.04f)).blur(80.dp))
        Box(modifier = Modifier.offset(x = (-50).dp, y = 400.dp).size(200.dp).clip(CircleShape)
            .background(Primary.copy(alpha = 0.03f)).blur(60.dp))

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Expand the Mesh", style = MaterialTheme.typography.titleLarge,
                    color = White, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // GLOWING QR CODE
                Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                    // Animated glow ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Primary.copy(alpha = glowAlpha * 0.5f), Color.Transparent),
                                center = center, radius = 150.dp.toPx()
                            ),
                            radius = 150.dp.toPx(), center = center
                        )
                        drawCircle(
                            color = Primary.copy(alpha = 0.08f * (1.15f - ringScale).coerceAtLeast(0f)),
                            radius = 140.dp.toPx() * ringScale.coerceIn(0.8f, 1.15f),
                            center = center, style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // QR card
                    Surface(
                        modifier = Modifier.size(230.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0A1616),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary.copy(alpha = 0.25f)),
                        shadowElevation = 24.dp
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Referral code
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                ) {
                    Text(referralCode,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleLarge, color = Primary,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Invite a peer to your grid and earn\na 5% bandwidth bonus",
                    style = MaterialTheme.typography.bodyLarge, color = SlateGray400,
                    textAlign = TextAlign.Center, lineHeight = 24.sp)

                Spacer(modifier = Modifier.height(12.dp))

                // Bonus stats
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    InviteStat("Referrals", "7")
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(SlateGray700))
                    InviteStat("Bonus Earned", "+$0.84")
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(SlateGray700))
                    InviteStat("Grid Size", "12")
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Share button
                Button(
                    onClick = { shareInvite(context, shareText) },
                    modifier = Modifier.fillMaxWidth().height(58.dp).shadow(
                        elevation = 20.dp, shape = RoundedCornerShape(50),
                        ambientColor = Primary.copy(alpha = 0.35f), spotColor = Primary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("SHARE INVITE LINK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Your referral earns passive SOL for every GB your peer shares",
                    style = MaterialTheme.typography.bodySmall, color = SlateGray500,
                    textAlign = TextAlign.Center, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InviteStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = SlateGray500, fontWeight = FontWeight.Medium)
    }
}

private fun shareInvite(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Join GridLink — Earn SOL by sharing bandwidth")
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
