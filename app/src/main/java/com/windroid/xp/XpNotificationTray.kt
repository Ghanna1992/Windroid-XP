package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun XpNotificationTray(context: Context) {
    var expanded by remember { mutableStateOf(false) }
    val packages by WindroidNotificationListener.packages.collectAsState()
    val accessGranted = notificationAccessGranted(context)
    val visible = packages.take(2)
    val overflow = packages.drop(2).take(8)
    val chevron = remember { loadTrayAsset(context, "chevron.png") }

    Row(
        Modifier
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2EA7F0), Color(0xFF1686D8), Color(0xFF0A6EC2))
                )
            )
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        if (expanded) {
            if (!accessGranted) {
                Text(
                    "Enable notification icons",
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .clickable {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                )
            } else {
                overflow.forEach { pkg ->
                    TrayNotificationIcon(context, pkg)
                    Spacer(Modifier.width(2.dp))
                }
            }
        }

        Box(
            Modifier
                .size(29.dp)
                .clickable {
                    if (!accessGranted) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    } else {
                        expanded = !expanded
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (chevron != null) {
                Image(
                    bitmap = chevron,
                    contentDescription = if (expanded) "Hide notification icons" else "Show notification icons",
                    modifier = Modifier.size(27.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(if (expanded) "▶" else "◀", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        visible.forEach { pkg ->
            Spacer(Modifier.width(2.dp))
            TrayNotificationIcon(context, pkg)
        }

        Spacer(Modifier.width(4.dp))
        XpTrayClock()
        Spacer(Modifier.width(2.dp))
    }
}

@Composable
private fun TrayNotificationIcon(context: Context, packageName: String) {
    val icon = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName).toBitmap(64, 64).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    val label = remember(packageName) {
        try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    Box(
        Modifier
            .size(28.dp)
            .clickable {
                context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(22.dp), contentScale = ContentScale.Fit)
        } else {
            Text("●", color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
private fun XpTrayClock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Date()
        }
    }
    val text = remember(now) { SimpleDateFormat("h:mm a", Locale.getDefault()).format(now) }
    Text(text, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
}

private fun notificationAccessGranted(context: Context): Boolean {
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
    return enabled.split(':').any { it.contains(context.packageName, ignoreCase = true) }
}

private fun loadTrayAsset(context: Context, fileName: String): ImageBitmap? = try {
    context.assets.open("icons/ui/$fileName").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
} catch (_: Exception) {
    null
}
