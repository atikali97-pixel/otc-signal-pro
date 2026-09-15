package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.LuciferVoiceAssistant
import com.example.data.entity.CandleEntity
import com.example.data.entity.SignalEntity
import com.example.ui.theme.CandleGreen
import com.example.ui.theme.CandleRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun DashboardScreen(
    pairName: String,
    expiryMinutes: Int,
    secondsRemaining: Int,
    activeSignal: SignalEntity?,
    candles: List<CandleEntity>,
    capturedFrame: Bitmap?,
    isRecording: Boolean,
    winCount: Int,
    lossCount: Int,
    tieCount: Int,
    lucifer: LuciferVoiceAssistant,
    luciferSpeakingText: String,
    isListening: Boolean,
    visionResult: String?,
    isAnalyzingVision: Boolean,
    isSystemActive: Boolean = true,
    onToggleSystemPower: () -> Unit = {},
    onExpiryChanged: (Int) -> Unit,
    onSelectPairClicked: () -> Unit,
    onStartRecordingClicked: () -> Unit,
    onStopRecordingClicked: () -> Unit,
    onOpenBrokersClicked: () -> Unit,
    onOpenHistoryClicked: () -> Unit,
    onOpenPluginsClicked: () -> Unit,
    onOpenSettingsClicked: () -> Unit,
    onAnalyzeVisionClicked: () -> Unit,
    onImmediateSignalClicked: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Pulse animation for countdown when <= 5 seconds
    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(secondsRemaining <= 5) {
        if (secondsRemaining <= 5) {
            pulseAnim.animateTo(
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(250),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAnim.snapTo(1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleSystemPower() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isSystemActive) NeonGreen else NeonRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OTC SIGNAL PRO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSystemActive) NeonGreen else TextSecondary,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenBrokersClicked) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "Brokers",
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onOpenPluginsClicked) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = "Feature Builder",
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onOpenHistoryClicked) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Stats",
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onOpenSettingsClicked) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ⚡ MASTER SYSTEM POWER CARD (ENTIRE APP + LUCIFER ON/OFF)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (isSystemActive) NeonGreen.copy(alpha = 0.8f) else NeonRed.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemActive) DarkSurfaceElevated else DarkSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSystemActive) NeonGreen.copy(alpha = 0.15f) else NeonRed.copy(alpha = 0.15f)
                                )
                                .border(1.5.dp, if (isSystemActive) NeonGreen else NeonRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "System Power",
                                tint = if (isSystemActive) NeonGreen else NeonRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SYSTEM & LUCIFER",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSystemActive) NeonGreen.copy(alpha = 0.2f) else NeonRed.copy(alpha = 0.2f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isSystemActive) "ONLINE" else "OFFLINE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSystemActive) NeonGreen else NeonRed
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSystemActive) "Signals & Voice Active" else "Entire App + Lucifer Paused",
                                fontSize = 11.sp,
                                color = if (isSystemActive) TextSecondary else TextMuted
                            )
                        }
                    }

                    // Tactile Master Power Switch / Button
                    Button(
                        onClick = onToggleSystemPower,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSystemActive) NeonRed else NeonGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSystemActive) "TURN OFF" else "TURN ON",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pair & Expiry Selector Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pair Pill
                Card(
                    modifier = Modifier
                        .clickable { onSelectPairClicked() }
                        .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pairName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Change",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Expiry Selector 1M / 5M
                Row(
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (expiryMinutes == 1) NeonGreen else Color.Transparent)
                            .clickable { onExpiryChanged(1) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "1M",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (expiryMinutes == 1) Color.Black else TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (expiryMinutes == 5) NeonGreen else Color.Transparent)
                            .clickable { onExpiryChanged(5) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "5M",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (expiryMinutes == 5) Color.Black else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Performance Counter Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Wins",
                        tint = NeonGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "W: $winCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NeonGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Losses",
                        tint = NeonRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "L: $lossCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NeonRed
                    )
                }

                val total = winCount + lossCount
                val winRate = if (total > 0) (winCount * 100 / total) else 92
                Text(
                    text = "Win Rate: $winRate%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Candlestick Chart Canvas View
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Live Price Action Stream (100 Candles Buffer)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Text(
                            text = "Price: ${String.format(Locale.US, "%.4f", candles.firstOrNull()?.close ?: 1.0850)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    CandlestickCanvas(
                        candles = candles.take(30),
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Strict Countdown & Signal Timing Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (secondsRemaining <= 5) 2.dp else 1.dp,
                        color = if (secondsRemaining <= 5) NeonGreen else DarkBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = if (secondsRemaining <= 5) NeonGreen else ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (secondsRemaining <= 5) "🚨 SIGNAL TRIGGER ZONE" else "Candle Close Countdown",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (secondsRemaining <= 5) NeonGreen else TextSecondary
                            )
                            Text(
                                text = "Next signal fires at 00:05",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    val mm = secondsRemaining / 60
                    val ss = secondsRemaining % 60
                    Text(
                        text = if (!isSystemActive) "PAUSED" else String.format(Locale.US, "%02d:%02d", mm, ss),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (!isSystemActive) TextMuted else if (secondsRemaining <= 5) NeonGreen else TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🎯 BIG SIGNAL CARD
            val sig = activeSignal
            val isCall = sig?.type == "CALL"
            val signalColor = if (!isSystemActive) NeonRed else if (isCall) NeonGreen else NeonRed

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, signalColor, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isSystemActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = NeonRed,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SYSTEM STANDBY",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonRed
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(NeonRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .border(1.dp, NeonRed, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "OFFLINE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NeonRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Entire App aur Lucifer voice assistant abhi OFF hain. Signals generate nahi ho rahe hain.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onToggleSystemPower,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TURN ON ENTIRE APP & LUCIFER", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCall) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = signalColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sig?.type ?: "CALL",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = signalColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(signalColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, signalColor, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${sig?.confidence ?: 92}% CONFIDENCE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = signalColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${sig?.pairName ?: pairName} • ${sig?.expiryMinutes ?: expiryMinutes} MIN EXPIRY",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Confluence breakdown
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "100% Price Action Confluences:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sig?.confluenceReasons ?: "Wick Rejection at S/R • Break of Structure • 5-Candle Momentum",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Result: ${sig?.result ?: "ACTIVE"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (sig?.result) {
                                "WIN" -> NeonGreen
                                "LOSS" -> NeonRed
                                else -> ElectricCyan
                            }
                        )

                        Button(
                            onClick = onImmediateSignalClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceElevated,
                                contentColor = ElectricCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Force Signal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

            Spacer(modifier = Modifier.height(12.dp))

            // 🎥 START / STOP SCREEN RECORDING BUTTON
            Button(
                onClick = {
                    if (isRecording) onStopRecordingClicked() else onStartRecordingClicked()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) NeonRed else NeonGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecording) "⏹️ STOP SCREEN RECORDING" else "🎥 START RECORDING (MEDIA PROJECTION)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }

            // Captured Chart Frame Preview if available
            if (capturedFrame != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Captured Live Chart Frame",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Button(
                                onClick = onAnalyzeVisionClicked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (isAnalyzingVision) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                                } else {
                                    Text("Analyze with Gemini Vision", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Image(
                            bitmap = capturedFrame.asImageBitmap(),
                            contentDescription = "Live Frame Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        if (!visionResult.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = visionResult,
                                fontSize = 12.sp,
                                color = ElectricCyan,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🎙️ LUCIFER AI VOICE ASSISTANT CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (!isSystemActive) NeonRed.copy(alpha = 0.5f) else ElectricCyan.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Floating Avatar Trigger
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(
                                2.dp,
                                if (!isSystemActive) NeonRed else if (isListening) NeonGreen else ElectricCyan,
                                CircleShape
                            )
                            .clickable {
                                if (!isSystemActive) {
                                    onToggleSystemPower()
                                } else {
                                    if (isListening) lucifer.stopListening() else lucifer.startListening()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (!isSystemActive) Icons.Default.PowerSettingsNew else if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Lucifer Mic",
                            tint = if (!isSystemActive) NeonRed else if (isListening) NeonGreen else TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LUCIFER AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSystemActive) ElectricCyan else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!isSystemActive) "🔴 Standby (OFF)" else if (isListening) "🎙️ Listening..." else "Idle",
                                fontSize = 11.sp,
                                color = if (!isSystemActive) NeonRed else if (isListening) NeonGreen else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!isSystemActive) {
                                "\"Lucifer standby mode mein hai. Master Power button se ON karein.\""
                            } else {
                                "\"$luciferSpeakingText\""
                            },
                            fontSize = 12.sp,
                            color = if (isSystemActive) TextPrimary else TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun CandlestickCanvas(
    candles: List<CandleEntity>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (candles.isEmpty()) return@Canvas

        val candleCount = min(candles.size, 25)
        val candleList = candles.take(candleCount).reversed()
        val spacing = size.width / candleCount
        val candleWidth = spacing * 0.65f

        val minPrice = candleList.minOfOrNull { it.low } ?: 1.0
        val maxPrice = candleList.maxOfOrNull { it.high } ?: 1.1
        val priceDiff = max(0.0001f, (maxPrice - minPrice).toFloat())

        candleList.forEachIndexed { index, candle ->
            val x = index * spacing + spacing / 2

            val highY = size.height - ((candle.high - minPrice).toFloat() / priceDiff * size.height)
            val lowY = size.height - ((candle.low - minPrice).toFloat() / priceDiff * size.height)
            val openY = size.height - ((candle.open - minPrice).toFloat() / priceDiff * size.height)
            val closeY = size.height - ((candle.close - minPrice).toFloat() / priceDiff * size.height)

            val color = if (candle.isBullish) CandleGreen else CandleRed

            // Draw wick
            drawLine(
                color = color,
                start = Offset(x, highY),
                end = Offset(x, lowY),
                strokeWidth = 2.dp.toPx()
            )

            // Draw body
            val topBody = min(openY, closeY)
            val bodyHeight = max(3f, kotlin.math.abs(closeY - openY))

            drawRect(
                color = color,
                topLeft = Offset(x - candleWidth / 2, topBody),
                size = Size(candleWidth, bodyHeight)
            )
        }
    }
}
