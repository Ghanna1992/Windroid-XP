package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XpNotificationTray(context: Context) {
    val prefs = remember { context.getSharedPreferences("windroid_prefs", Context.MODE_PRIVATE) }
    var expanded by remember { mutableStateOf(false) }
    var accessGranted by remember { mutableStateOf(notificationAccessGranted(context)) }
    var hiddenPackages by remember {
        mutableStateOf(prefs.getStringSet("hidden_tray_packages", emptySet())?.toSet() ?: emptySet())
    }
    val packages by WindroidNotificationListener.packages.collectAsState()
    val activePackages = packages.filterNot { it in hiddenPackages }
    val hasOverflow = activePackages.size > 2
    val shownPackages = if (expanded && hasOverflow) activePackages.take(10) else activePackages.take(2)
    val chevron = remember { loadTrayAsset(context, "chevron.png") }

    if (!hasOverflow && expanded) expanded = false

    LaunchedEffect(Unit) {
        while (true) {
            val current = notificationAccessGranted(context)
            if (current != accessGranted) accessGranted = current
            delay(1000)
        }
    }

    Row(
        Modifier
            .fillMaxHeight()
            // The tray owns its entire visible area so taps never fall through into
            // Android app task buttons while it is expanded over the taskbar.
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF36B9F3),
                        Color(0xFF1696DF),
                        Color(0xFF0B79CB)
                    )
                )
            )
            .padding(end = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // Thin dark seam between the normal XP taskbar and the cyan notification area.
        // The chevron is intentionally centered on this seam so roughly half of the
        // button sits over each color, matching the original XP tray treatment.
        Box(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(Color(0xFF111111))
        )

        if (!accessGranted) {
            Text(
                "Notifications",
                color = Color.White,
                fontSize = 9.sp,
                modifier = Modifier
                    .combinedClickable(
                        onClick = { openNotificationAccess(context) },
                        onLongClick = { }
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            )
        } else {
            if (hasOverflow) {
                // Reserve only the right half of the chevron inside the cyan tray.
                // The image itself is shifted left so its center rides directly on the
                // black seam; when the tray grows leftward this whole boundary moves too.
                Box(
                    Modifier
                        .width(15.dp)
                        .fillMaxHeight()
                        .combinedClickable(
                            onClick = { expanded = !expanded },
                            onLongClick = { }
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (chevron != null) {
                        Image(
                            bitmap = chevron,
                            contentDescription = if (expanded) "Hide notification icons" else "Show notification icons",
                            modifier = Modifier
                                .offset(x = (-14).dp)
                                .size(28.dp)
                                .rotate(if (expanded) 180f else 0f),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            if (expanded) "▶" else "◀",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(x = (-5).dp)
                        )
                    }
                }
                Spacer(Modifier.width(1.dp))
            }

            shownPackages.forEach { pkg ->
                TrayNotificationIcon(
                    context = context,
                    packageName = pkg,
                    onRemove = {
                        val updated = hiddenPackages + pkg
                        hiddenPackages = updated
                        prefs.edit().putStringSet("hidden_tray_packages", updated).apply()
                    }
                )
                Spacer(Modifier.width(2.dp))
            }
        }

        Spacer(Modifier.width(3.dp))
        XpTrayClock()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrayNotificationIcon(
    context: Context,
    packageName: String,
    onRemove: () -> Unit
) {
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
            .combinedClickable(
                onClick = {
                    context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                },
                onLongClick = onRemove
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit
            )
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
    Text(text, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
}

private fun openNotificationAccess(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
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
