package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class WizardPermissionItem(
    val id: Int,
    val title: String,
    val hindiReason: String,
    val technicalName: String,
    val icon: ImageVector,
    val isDangerous: Boolean,
    val androidPermission: String? = null
)

@Composable
fun PermissionWizardScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    val permissionsList = remember {
        listOf(
            WizardPermissionItem(
                id = 1,
                title = "Internet & Network",
                hindiReason = "Broker login aur real-time OTC price charts stream karne ke liye.",
                technicalName = "INTERNET + ACCESS_NETWORK_STATE",
                icon = Icons.Default.Wifi,
                isDangerous = false
            ),
            WizardPermissionItem(
                id = 2,
                title = "Floating Signal Overlay",
                hindiReason = "Broker app ke upar floating signal card aur Lucifer avatar dikhane ke liye.",
                technicalName = "SYSTEM_ALERT_WINDOW",
                icon = Icons.Default.Layers,
                isDangerous = true
            ),
            WizardPermissionItem(
                id = 3,
                title = "Background Signal Engine",
                hindiReason = "Background mein bina rukhe continuous price action monitor karne ke liye.",
                technicalName = "FOREGROUND_SERVICE",
                icon = Icons.Default.Visibility,
                isDangerous = false
            ),
            WizardPermissionItem(
                id = 4,
                title = "Chart Recording Engine",
                hindiReason = "In-app WebView chart ko capture karke real-time pattern analyse karne ke liye.",
                technicalName = "FOREGROUND_SERVICE_MEDIA_PROJECTION",
                icon = Icons.Default.ScreenShare,
                isDangerous = true
            ),
            WizardPermissionItem(
                id = 5,
                title = "Lucifer Voice Assistant",
                hindiReason = "Lucifer AI voice assistant se bol kar trade signals lene ke liye.",
                technicalName = "RECORD_AUDIO",
                icon = Icons.Default.Mic,
                isDangerous = true,
                androidPermission = Manifest.permission.RECORD_AUDIO
            ),
            WizardPermissionItem(
                id = 6,
                title = "Signal Push Alerts",
                hindiReason = "CALL aur PUT signal aate hi phone par instant alert notification paane ke liye.",
                technicalName = "POST_NOTIFICATIONS",
                icon = Icons.Default.Notifications,
                isDangerous = true,
                androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.POST_NOTIFICATIONS else null
            ),
            WizardPermissionItem(
                id = 7,
                title = "Haptic Vibration Alert",
                hindiReason = "Signal generate hote hi instant vibration feedback ke liye.",
                technicalName = "VIBRATE",
                icon = Icons.Default.Vibration,
                isDangerous = false
            ),
            WizardPermissionItem(
                id = 8,
                title = "Wake Lock Monitoring",
                hindiReason = "Screen lock na ho jab trading session chal raha ho.",
                technicalName = "WAKE_LOCK",
                icon = Icons.Default.PermDeviceInformation,
                isDangerous = false
            ),
            WizardPermissionItem(
                id = 9,
                title = "Chart Snapshot Storage",
                hindiReason = "High-accuracy chart patterns aur trading history snapshots save karne ke liye.",
                technicalName = "READ/WRITE_EXTERNAL_STORAGE",
                icon = Icons.Default.SdCard,
                isDangerous = false
            ),
            WizardPermissionItem(
                id = 10,
                title = "Broker Apps Detection",
                hindiReason = "Device mein installed Quotex ya IQ Option apps ko direct link karne ke liye.",
                technicalName = "QUERY_ALL_PACKAGES",
                icon = Icons.Default.Apps,
                isDangerous = false
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }
    val permissionStatus = remember { mutableStateMapOf<Int, Boolean>() }

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionStatus[currentStep + 1] = isGranted
        if (currentStep < permissionsList.size) {
            currentStep++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(16.dp)
    ) {
        if (currentStep < permissionsList.size) {
            val item = permissionsList[currentStep]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Progress
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "PERMISSION WIZARD",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Step ${currentStep + 1} of ${permissionsList.size}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (currentStep + 1) / permissionsList.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonGreen,
                        trackColor = DarkBorder,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Card with icon + Hindi explanation
                AnimatedContent(
                    targetState = item,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "WizardStep"
                ) { currentItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .border(2.dp, NeonGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = currentItem.icon,
                                    contentDescription = currentItem.title,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = currentItem.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = currentItem.technicalName,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                                    .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "🎯 Kyu chahiye yeh permission?",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = currentItem.hindiReason,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action Buttons: Allow / Skip
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (item.technicalName == "SYSTEM_ALERT_WINDOW") {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                                permissionStatus[item.id] = true
                                currentStep++
                            } else if (item.androidPermission != null) {
                                runtimeLauncher.launch(item.androidPermission)
                            } else {
                                permissionStatus[item.id] = true
                                currentStep++
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "ALLOW PERMISSION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            permissionStatus[item.id] = false
                            currentStep++
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Skip (Feature Disabled)",
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            // Summary / Completion Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Ready",
                    tint = NeonGreen,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Permission Setup Complete!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Aapka OTC trading environment fully ready hai.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Configured Permissions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        permissionsList.forEach { p ->
                            val granted = permissionStatus[p.id] ?: true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = p.title,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (granted) "✅ Granted" else "⚠️ Skipped",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (granted) NeonGreen else NeonRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "ENTER TRADING COCKPIT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { currentStep = 0 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricCyan.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "Retry Missing Permissions", fontSize = 14.sp)
                }
            }
        }
    }
}
