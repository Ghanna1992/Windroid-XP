package com.windroid.xp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap? = null
)

@Composable
internal fun rememberInstalledApps(context: Context): List<LaunchableApp> {
    var apps by remember(context) { mutableStateOf(installedApps(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                apps = installedApps(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    return apps
}

internal fun installedApps(context: Context): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val pm = context.packageManager
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map { info ->
            val label = info.loadLabel(pm).toString()
            val packageName = info.activityInfo.packageName
            val realIcon = try { info.loadIcon(pm).toBitmap(96, 96).asImageBitmap() } catch (_: Exception) { null }
            val icon = defaultXpAppIcon(context, packageName, label) ?: realIcon
            LaunchableApp(label = label, packageName = packageName, icon = icon)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

internal fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {
    val pkg = packageName.lowercase()
    val name = label.lowercase()
    val normalizedLabel = name.replace(Regex("[^a-z0-9]"), "")
    val appNamedIcon = listAssetImages(context, "icons/apps").firstOrNull { file ->
        val base = file.substringAfter("::", file).substringAfterLast('/').substringBeforeLast('.').lowercase()
        val normalizedFile = base.replace(Regex("[^a-z0-9]"), "")
        normalizedFile == normalizedLabel ||
            (name == "google home" && normalizedFile == "home") ||
            (name.contains("baby plus") && normalizedFile == "babyplus") ||
            (name.contains("1kosmos") && normalizedFile == "1kosmos")
    }
    if (appNamedIcon != null) loadAssetImage(context, "icons/apps", appNamedIcon)?.let { return it }

    val asset = when {
        pkg in setOf("com.android.chrome", "com.opera.browser", "com.opera.gx", "org.mozilla.firefox", "com.microsoft.emmx", "com.sec.android.app.sbrowser") ||
            name in setOf("chrome", "opera", "opera gx", "firefox", "microsoft edge", "samsung internet") -> "Internet Explorer 6.png"
        pkg in setOf("com.google.android.gm", "com.samsung.android.email.provider", "com.samsung.android.email.ui") ||
            name in setOf("gmail", "email", "samsung email") -> "Outlook Express.png"
        pkg == "com.google.android.youtube" || name == "youtube" -> "Windows Media Player 10.png"
        pkg in setOf("com.sec.android.gallery3d", "com.google.android.apps.photos") || name in setOf("gallery", "photos", "google photos") -> "Windows Picture and Fax Viewer.png"
        pkg in setOf("com.sec.android.app.myfiles", "com.google.android.apps.nbu.files") || name in setOf("my files", "files", "files by google") -> "Explorer.png"
        pkg in setOf("com.sec.android.app.popupcalculator", "com.google.android.calculator") || name == "calculator" -> "Calculator.png"
        pkg in setOf("com.sec.android.app.camera", "com.google.android.googlecamera") || name == "camera" -> "Digital Camera.png"
        pkg in setOf("com.samsung.android.app.contacts", "com.google.android.contacts") || name == "contacts" -> "Address Book.png"
        pkg in setOf("com.google.android.apps.messaging", "com.samsung.android.messaging") || name == "messages" -> "Windows Messenger.png"
        pkg in setOf("com.samsung.android.dialer", "com.google.android.dialer") || name in setOf("phone", "dialer") -> "Phone.png"
        else -> null
    }
    return asset?.let { loadAssetImage(context, "icons", it) }
}

internal fun launchApp(context: Context, app: LaunchableApp) {
    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(intent)
    }
}

internal fun openAppInfo(context: Context, packageName: String) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
