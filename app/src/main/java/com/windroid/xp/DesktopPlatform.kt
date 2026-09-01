package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap

internal const val DEFAULT_DESKTOP_BACKGROUND = "windows_xp_bliss-wide.jpg"

internal fun iconPrefKey(id: String) = "custom_icon_$id"
internal fun desktopPositionKey(id: String, axis: String) = "desktop_pos_${id}_$axis"

private val XP_ICON_REGISTRY = mapOf(
    "computer" to "My Computer.png",
    "documents" to "My Documents.png",
    "internet" to "Internet Explorer 6.png",
    "recycle" to "Recycle Bin (empty).png",
    "control" to "Control Panel.png",
    "appearance" to "Appearance.png",
    "programs" to "Change or Remove Programs.png",
    "network" to "Connection Status.png",
    "settings" to "Additional Settings.png",
    "back" to "Back.png",
    "storage" to "Hard Disk.png",
    "search" to "Search.png",
    "run" to "Run.png",
    "update" to "Windows Update.png",
    "user" to "User Accounts.png",
    "power" to "Power.png"
)

internal fun xpIcon(context: Context, key: String): ImageBitmap? = XP_ICON_REGISTRY[key]?.let { loadAssetImage(context, "icons", it) }
internal fun openDefaultBrowser(context: Context) { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
internal fun openDocuments(context: Context) { try { context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) } }
