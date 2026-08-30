package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WindroidDesktop(this) }
    }
}

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap? = null
)

private fun installedApps(context: Context): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val pm = context.packageManager
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map { info ->
            val label = info.loadLabel(pm).toString()
            val packageName = info.activityInfo.packageName
            val realIcon = try { info.loadIcon(pm).toBitmap(96, 96).asImageBitmap() } catch (_: Exception) { null }
            val icon = defaultXpAppIcon(context, packageName, label) ?: realIcon
            LaunchableApp(
                label = label,
                packageName = packageName,
                icon = icon
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {
    val pkg = packageName.lowercase()
    val name = label.lowercase()

    val asset = when {
        pkg in setOf(
            "com.android.chrome",
            "com.opera.browser",
            "com.opera.gx",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser"
        ) || name in setOf("chrome", "opera", "opera gx", "firefox", "microsoft edge", "samsung internet") ->
            "Internet Explorer 6.png"

        pkg in setOf(
            "com.google.android.gm",
            "com.samsung.android.email.provider",
            "com.samsung.android.email.ui"
        ) || name in setOf("gmail", "email", "samsung email") ->
            "Outlook Express.png"

        pkg == "com.google.android.youtube" || name == "youtube" ->
            "Windows Media Player 10.png"

        pkg in setOf(
            "com.sec.android.gallery3d",
            "com.google.android.apps.photos"
        ) || name in setOf("gallery", "photos", "google photos") ->
            "Windows Picture and Fax Viewer.png"

        pkg in setOf(
            "com.sec.android.app.myfiles",
            "com.google.android.apps.nbu.files"
        ) || name in setOf("my files", "files", "files by google") ->
            "Explorer.png"

        pkg in setOf(
            "com.sec.android.app.popupcalculator",
            "com.google.android.calculator"
        ) || name == "calculator" ->
            "Calculator.png"

        pkg in setOf(
            "com.sec.android.app.camera",
            "com.google.android.googlecamera"
        ) || name == "camera" ->
            "Digital Camera.png"

        pkg in setOf(
            "com.samsung.android.app.contacts",
            "com.google.android.contacts"
        ) || name == "contacts" ->
            "Address Book.png"

        pkg in setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging"
        ) || name == "messages" ->
            "Windows Messenger.png"

        pkg in setOf(
            "com.samsung.android.dialer",
            "com.google.android.dialer"
        ) || name in setOf("phone", "dialer") ->
            "Phone.png"

        else -> null
    }

    return asset?.let { loadAssetImage(context, "icons", it) }
}

private fun launchApp(context: Context, app: LaunchableApp) {
    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(intent)
    }
}

private fun openAppInfo(context: Context, packageName: String) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun listAssetImages(context: Context, folder: String): List<String> {
    val allowed = setOf("png", "jpg", "jpeg", "webp")
    val results = mutableListOf<String>()
    return try {
        context.assets.list(folder)?.forEach { fileName ->
            val ext = fileName.substringAfterLast('.', "").lowercase()
            when {
                ext in allowed -> results.add(fileName)
                folder == "icons" && ext == "zip" -> {
                    try {
                        context.assets.open("$folder/$fileName").use { raw ->
                            java.util.zip.ZipInputStream(raw).use { zip ->
                                var entry = zip.nextEntry
                                while (entry != null) {
                                    if (!entry.isDirectory && entry.name.substringAfterLast('.', "").lowercase() in allowed) {
                                        results.add("$fileName::${entry.name}")
                                    }
                                    zip.closeEntry()
                                    entry = zip.nextEntry
                                }
                            }
                        }
                    } catch (_: Exception) { }
                }
            }
        }
        results.sortedBy { it.substringAfter("::", it).lowercase() }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun loadAssetImage(context: Context, folder: String, fileName: String?): ImageBitmap? {
    if (fileName.isNullOrBlank()) return null
    return try {
        if (fileName.contains("::")) {
            val zipName = fileName.substringBefore("::")
            val entryName = fileName.substringAfter("::")
            context.assets.open("$folder/$zipName").use { raw ->
                java.util.zip.ZipInputStream(raw).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name == entryName) {
                            val bytes = java.io.ByteArrayOutputStream()
                            zip.copyTo(bytes)
                            return BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())?.asImageBitmap()
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            null
        } else {
            context.assets.open("$folder/$fileName").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }
    } catch (_: Exception) {
        null
    }
}

private fun resolveIntentIcon(context: Context, intent: Intent): ImageBitmap? {
    return try {
        val info = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        info.loadIcon(context.packageManager).toBitmap(96, 96).asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun loadStartButtonImage(context: Context): ImageBitmap? {
    return try {
        val decoded = context.assets.open("icons/start_button.png").use { BitmapFactory.decodeStream(it) } ?: return null
        val bitmap = decoded.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap.asImageBitmap()

        fun removableBackground(pixel: Int): Boolean {
            val a = android.graphics.Color.alpha(pixel)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val average = (r + g + b) / 3
            return a == 0 || (max - min <= 30 && average >= 135)
        }

        val seen = BooleanArray(width * height)
        val queue = java.util.ArrayDeque<Int>()
        fun seed(x: Int, y: Int) {
            val index = y * width + x
            if (!seen[index] && removableBackground(bitmap.getPixel(x, y))) {
                seen[index] = true
                queue.add(index)
            }
        }
        for (x in 0 until width) {
            seed(x, 0)
            seed(x, height - 1)
        }
        for (y in 0 until height) {
            seed(0, y)
            seed(width - 1, y)
        }
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val x = index % width
            val y = index / width
            bitmap.setPixel(x, y, android.graphics.Color.TRANSPARENT)
            if (x > 0) seed(x - 1, y)
            if (x + 1 < width) seed(x + 1, y)
            if (y > 0) seed(x, y - 1)
            if (y + 1 < height) seed(x, y + 1)
        }
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        loadAssetImage(context, "icons", "start_button.png")
    }
}

private fun iconPrefKey(id: String) = "custom_icon_$id"

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
    "user" to "User Accounts.png"
)

private fun xpIcon(context: Context, key: String): ImageBitmap? =
    XP_ICON_REGISTRY[key]?.let { loadAssetImage(context, "icons", it) }

private fun openDefaultBrowser(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

private fun openDocuments(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        })
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
    }
}

@Composable
fun WindroidDesktop(context: Context) {
    val prefs = remember { context.getSharedPreferences("windroid_prefs", Context.MODE_PRIVATE) }
    val apps = remember { installedApps(context) }

    var startOpen by remember { mutableStateOf(false) }
    var computerOpen by remember { mutableStateOf(false) }
    var profileOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var updateWindowOpen by remember { mutableStateOf(false) }
    var controlPanelOpen by remember { mutableStateOf(false) }
    var recycleOpen by remember { mutableStateOf(false) }
    var runOpen by remember { mutableStateOf(false) }

    val savedRecentPackages = remember {
        prefs.getString("recent_apps", "").orEmpty()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    val launchedApps = remember(apps) {
        mutableStateListOf<LaunchableApp>().apply {
            savedRecentPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }.forEach { add(it) }
        }
    }

    var userName by remember { mutableStateOf(prefs.getString("user_name", "User") ?: "User") }
    var userAvatar by remember { mutableStateOf(prefs.getString("user_avatar", "🙂") ?: "🙂") }
    var selectedBackground by remember { mutableStateOf(prefs.getString("desktop_background", null)) }
    var customizationVersion by remember { mutableIntStateOf(0) }
    var desktopPackages by remember {
        mutableStateOf(prefs.getStringSet("desktop_apps", emptySet())?.toSet() ?: emptySet())
    }
    var removedDesktopPackages by remember {
        mutableStateOf(prefs.getStringSet("removed_desktop_apps", emptySet())?.toSet() ?: emptySet())
    }

    val backgrounds = remember { listAssetImages(context, "backgrounds") }
    val iconFiles = remember {
        listAssetImages(context, "icons").filterNot { it == "start_button.png" || it == "close_button.png" }
    }
    val startButtonImage = remember { loadStartButtonImage(context) }
    val defaultBrowserIcon = remember {
        resolveIntentIcon(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
    }
    val defaultFileIcon = remember {
        resolveIntentIcon(
            context,
            Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
        )
    }

    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var updateStatus by remember { mutableStateOf("") }
    var downloadedUpdate by remember { mutableStateOf<File?>(null) }
    val updateScope = rememberCoroutineScope()

    fun saveRecentApps() {
        prefs.edit().putString(
            "recent_apps",
            launchedApps.take(12).joinToString("\n") { it.packageName }
        ).apply()
    }

    fun openAndroidApp(app: LaunchableApp) {
        launchedApps.removeAll { it.packageName == app.packageName }
        launchedApps.add(0, app)
        while (launchedApps.size > 12) launchedApps.removeAt(launchedApps.lastIndex)
        saveRecentApps()
        startOpen = false
        computerOpen = false
        profileOpen = false
        settingsOpen = false
        launchApp(context, app)
    }

    fun setIcon(id: String, fileName: String?) {
        prefs.edit().apply {
            if (fileName == null) remove(iconPrefKey(id)) else putString(iconPrefKey(id), fileName)
        }.apply()
        customizationVersion++
    }

    fun setDesktopApp(packageName: String, enabled: Boolean) {
        desktopPackages = if (enabled) desktopPackages + packageName else desktopPackages - packageName
        removedDesktopPackages = if (enabled) removedDesktopPackages - packageName else removedDesktopPackages + packageName
        prefs.edit()
            .putStringSet("desktop_apps", desktopPackages)
            .putStringSet("removed_desktop_apps", removedDesktopPackages)
            .apply()
    }

    fun checkForUpdates(manual: Boolean) {
        startOpen = false
        updateStatus = "Checking for updates..."
        if (manual) updateWindowOpen = true
        updateScope.launch {
            val found = UpdateManager.checkForUpdate()
            updateInfo = found
            downloadedUpdate = null
            if (found != null) {
                updateStatus = "Windroid XP ${found.versionName} is ready."
                updateWindowOpen = true
            } else if (manual) {
                updateStatus = "Your computer is up to date."
                updateWindowOpen = true
            } else {
                updateWindowOpen = false
            }
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        val found = UpdateManager.checkForUpdate()
        if (found != null) {
            updateInfo = found
            updateStatus = "Windroid XP ${found.versionName} is ready."
            updateWindowOpen = true
        }
    }

    BackHandler {
        when {
            runOpen -> runOpen = false
            recycleOpen -> recycleOpen = false
            controlPanelOpen -> controlPanelOpen = false
            updateWindowOpen -> updateWindowOpen = false
            settingsOpen -> settingsOpen = false
            profileOpen -> profileOpen = false
            computerOpen -> computerOpen = false
            startOpen -> startOpen = false
            else -> Unit
        }
    }

    val xpBlue = Color(0xFF245EDB)
    val taskbarHeight = 43.dp

    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
        WallpaperLayer(context, selectedBackground)

        BoxWithConstraints(
            Modifier.fillMaxSize().padding(start = 10.dp, top = 12.dp, end = 8.dp, bottom = taskbarHeight + 8.dp)
        ) {
            val desktopApps = apps.filter { it.packageName in desktopPackages }
            val totalItems = 4 + desktopApps.size
            val slotHeight = 94.dp
            val rowsPerColumn = (maxHeight.value / slotHeight.value).toInt().coerceAtLeast(1)
            val columnCount = ((totalItems + rowsPerColumn - 1) / rowsPerColumn).coerceAtLeast(1)

            Row(
                Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(columnCount) { columnIndex ->
                    Column(
                        Modifier.width(88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(rowsPerColumn) { rowIndex ->
                            val itemIndex = columnIndex * rowsPerColumn + rowIndex
                            if (itemIndex < totalItems) {
                                when (itemIndex) {
                                    0 -> DesktopSystemShortcut(
                                        context, prefs, customizationVersion,
                                        "builtin_my_computer", "computer", "🖥️", "My Computer",
                                        onClick = { computerOpen = true; startOpen = false },
                                        onCustomize = { startOpen = false; settingsOpen = true },
                                        onReset = { setIcon("builtin_my_computer", null) }
                                    )
                                    1 -> DesktopSystemShortcut(
                                        context, prefs, customizationVersion,
                                        "builtin_my_documents", "documents", "📁", "My Documents",
                                        onClick = { startOpen = false; openDocuments(context) },
                                        onCustomize = { startOpen = false; settingsOpen = true },
                                        onReset = { setIcon("builtin_my_documents", null) }
                                    )
                                    2 -> DesktopSystemShortcut(
                                        context, prefs, customizationVersion,
                                        "builtin_internet", "internet", "🌐", "Internet Explorer",
                                        onClick = { startOpen = false; openDefaultBrowser(context) },
                                        onCustomize = { startOpen = false; settingsOpen = true },
                                        onReset = { setIcon("builtin_internet", null) }
                                    )
                                    3 -> DesktopSystemShortcut(
                                        context, prefs, customizationVersion,
                                        "builtin_recycle_bin", "recycle", "🗑️", "Recycle Bin",
                                        onClick = { startOpen = false; recycleOpen = true },
                                        onCustomize = { startOpen = false; settingsOpen = true },
                                        onReset = { setIcon("builtin_recycle_bin", null) }
                                    )
                                    else -> {
                                        val app = desktopApps[itemIndex - 4]
                                        DesktopAppIcon(
                                            context = context,
                                            prefs = prefs,
                                            version = customizationVersion,
                                            app = app,
                                            onClick = { openAndroidApp(app) },
                                            onRemove = { setDesktopApp(app.packageName, false) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (computerOpen) {
            XPWindow("My Computer", Modifier.align(Alignment.Center), onClose = { computerOpen = false }) {
                Text("Files Stored on This Computer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                XPSystemRow(context, "storage", "Internal Storage") { openDocuments(context) }
                XPSystemRow(context, "documents", "My Documents") { openDocuments(context) }
                XPSystemRow(context, "control", "Control Panel") { computerOpen = false; controlPanelOpen = true }
                XPSystemRow(context, "settings", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                Spacer(Modifier.height(10.dp))
                XPActionButton("Close") { computerOpen = false }
            }
        }

        if (controlPanelOpen) {
            XPWindow("Control Panel", Modifier.align(Alignment.Center), onClose = { controlPanelOpen = false }) {
                Text("Pick a category", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                XPSystemRow(context, "appearance", "Appearance and Themes") { controlPanelOpen = false; settingsOpen = true }
                XPSystemRow(context, "programs", "Add or Remove Programs") { context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS)) }
                XPSystemRow(context, "network", "Network Connections") { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
                XPSystemRow(context, "user", "User Accounts") { controlPanelOpen = false; profileOpen = true }
                XPSystemRow(context, "update", "Automatic Updates") { controlPanelOpen = false; checkForUpdates(true) }
                XPSystemRow(context, "settings", "Android System Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            }
        }

        if (recycleOpen) {
            XPWindow("Recycle Bin", Modifier.align(Alignment.Center), onClose = { recycleOpen = false }) {
                Text("Removed desktop shortcuts", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val removedApps = apps.filter { it.packageName in removedDesktopPackages }
                if (removedApps.isEmpty()) {
                    Text("The Recycle Bin is empty.", fontSize = 12.sp, color = Color(0xFF555555))
                } else {
                    Column(Modifier.heightIn(max = 310.dp).verticalScroll(rememberScrollState())) {
                        removedApps.forEach { app ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (app.icon != null) Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(app.label, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Restore", color = Color(0xFF003399), fontSize = 11.sp, modifier = Modifier.clickable { setDesktopApp(app.packageName, true) }.padding(5.dp))
                            }
                        }
                    }
                }
            }
        }

        if (runOpen) {
            RunWindow(
                apps = apps,
                context = context,
                onLaunch = { app -> runOpen = false; openAndroidApp(app) },
                onClose = { runOpen = false },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (profileOpen) {
            ProfileWindow(
                currentName = userName,
                currentAvatar = userAvatar,
                onSave = { name, avatar ->
                    userName = name.ifBlank { "User" }
                    userAvatar = avatar
                    prefs.edit().putString("user_name", userName).putString("user_avatar", userAvatar).apply()
                    profileOpen = false
                },
                onCancel = { profileOpen = false },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (settingsOpen) {
            AppearanceWindow(
                context = context,
                apps = apps,
                backgrounds = backgrounds,
                iconFiles = iconFiles,
                selectedBackground = selectedBackground,
                prefs = prefs,
                desktopPackages = desktopPackages,
                onBackgroundSelected = { fileName ->
                    selectedBackground = fileName
                    prefs.edit().apply {
                        if (fileName == null) remove("desktop_background") else putString("desktop_background", fileName)
                    }.apply()
                },
                onIconSelected = { id, fileName -> setIcon(id, fileName) },
                onDesktopToggle = { pkg, enabled -> setDesktopApp(pkg, enabled) },
                onClose = { settingsOpen = false },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (updateWindowOpen) {
            Box(
                Modifier.fillMaxSize().clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { }
            )
            XPWindow("Windows Update", Modifier.align(Alignment.Center), onClose = { updateWindowOpen = false }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    xpIcon(context, "update")?.let { icon ->
                        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(26.dp), contentScale = ContentScale.Fit)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Automatic Updates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(updateStatus, fontSize = 13.sp)
                updateInfo?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Spacer(Modifier.height(8.dp))
                    Text(notes.take(280), fontSize = 11.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(15.dp))
                val readyFile = downloadedUpdate
                val found = updateInfo
                if (readyFile != null) {
                    XPActionButton("Install update") {
                        val opened = UpdateManager.installUpdate(context, readyFile)
                        if (!opened) updateStatus = "Allow installs from Windroid XP, then return and tap Install update again."
                    }
                    Spacer(Modifier.height(8.dp))
                } else if (found != null) {
                    XPActionButton("Download and install") {
                        updateStatus = "Downloading update..."
                        updateScope.launch {
                            val file = UpdateManager.downloadUpdate(context, found)
                            if (file == null) {
                                updateStatus = "The update could not be downloaded. Try again later."
                            } else {
                                downloadedUpdate = file
                                updateStatus = "Download complete. Opening the Android installer..."
                                val opened = UpdateManager.installUpdate(context, file)
                                if (!opened) updateStatus = "Allow installs from Windroid XP, then return and tap Install update."
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    XPActionButton("Check again") { checkForUpdates(true) }
                    Spacer(Modifier.height(8.dp))
                }
                Text("Remind me later", color = Color(0xFF003399), fontSize = 12.sp, modifier = Modifier.clickable { updateWindowOpen = false })
            }
        }

        if (startOpen) {
            Box(
                Modifier.fillMaxSize().clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { startOpen = false }
            )
            StartMenu(
                apps = apps,
                recentApps = launchedApps,
                context = context,
                prefs = prefs,
                customizationVersion = customizationVersion,
                desktopPackages = desktopPackages,
                userName = userName,
                userAvatar = userAvatar,
                onEditProfile = { startOpen = false; profileOpen = true },
                onLaunchApp = { openAndroidApp(it) },
                onToggleDesktop = { app -> setDesktopApp(app.packageName, app.packageName !in desktopPackages) },
                onOpenAppearance = { startOpen = false; settingsOpen = true },
                onOpenComputer = { startOpen = false; computerOpen = true },
                onOpenControlPanel = { startOpen = false; controlPanelOpen = true },
                onOpenRecycle = { startOpen = false; recycleOpen = true },
                onOpenRun = { startOpen = false; runOpen = true },
                onCheckUpdates = { checkForUpdates(true) },
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = taskbarHeight)
            )
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(taskbarHeight).background(
                Brush.verticalGradient(listOf(Color(0xFF3886E8), xpBlue, Color(0xFF1748AF)))
            ), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.fillMaxHeight().width(108.dp)
                    .then(
                        if (startButtonImage == null) Modifier.background(
                            Brush.verticalGradient(listOf(Color(0xFF55B747), Color(0xFF33952E), Color(0xFF257E25))),
                            RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp)
                        ) else Modifier
                    )
                    .clickable {
                        startOpen = !startOpen
                        computerOpen = false
                        profileOpen = false
                        settingsOpen = false
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                if (startButtonImage != null) {
                    Image(
                        bitmap = startButtonImage,
                        contentDescription = "Start",
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("⊞  start", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(Modifier.width(5.dp))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                if (computerOpen) TaskButton(null, "🖥️", "My Computer") { computerOpen = true; startOpen = false }
                launchedApps.forEach { app ->
                    val custom = remember(customizationVersion, app.packageName) {
                        loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null))
                    }
                    TaskButton(custom ?: app.icon, "▣", app.label) { openAndroidApp(app) }
                }
            }

            XPSystemTray(context)
        }
    }
}

@Composable
private fun WallpaperLayer(context: Context, selectedBackground: String?) {
    val image = remember(selectedBackground) { loadAssetImage(context, "backgrounds", selectedBackground) }
    if (image != null) {
        Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else XPWallpaper()
}

@Composable
private fun XPWallpaper() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF2087D5), Color(0xFF55B6ED), Color(0xFFBDEAFF)),
                endY = size.height * 0.68f
            )
        )
        val farHill = Path().apply {
            moveTo(0f, size.height * 0.70f)
            cubicTo(size.width * 0.18f, size.height * 0.58f, size.width * 0.44f, size.height * 0.64f, size.width * 0.64f, size.height * 0.72f)
            cubicTo(size.width * 0.78f, size.height * 0.78f, size.width * 0.92f, size.height * 0.70f, size.width, size.height * 0.67f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(farHill, brush = Brush.verticalGradient(listOf(Color(0xFF79C752), Color(0xFF4DAA35))))
        val nearHill = Path().apply {
            moveTo(0f, size.height * 0.82f)
            cubicTo(size.width * 0.20f, size.height * 0.69f, size.width * 0.40f, size.height * 0.70f, size.width * 0.58f, size.height * 0.82f)
            cubicTo(size.width * 0.75f, size.height * 0.93f, size.width * 0.90f, size.height * 0.85f, size.width, size.height * 0.80f)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(nearHill, brush = Brush.verticalGradient(listOf(Color(0xFF56B33D), Color(0xFF248C2A))))
    }
}

@Composable
private fun DesktopResolvedBuiltInIcon(
    context: Context,
    prefs: android.content.SharedPreferences,
    version: Int,
    id: String,
    fallback: String,
    label: String,
    resolvedIcon: ImageBitmap?,
    onClick: () -> Unit
) {
    val custom = remember(version, id) {
        loadAssetImage(context, "icons", prefs.getString(iconPrefKey(id), null))
    }
    DesktopIcon(custom ?: resolvedIcon, fallback, label, onClick)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopSystemShortcut(
    context: Context,
    prefs: android.content.SharedPreferences,
    version: Int,
    id: String,
    registryKey: String,
    fallback: String,
    label: String,
    onClick: () -> Unit,
    onCustomize: () -> Unit,
    onReset: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val customName = remember(id, version) { prefs.getString(iconPrefKey(id), null) }
    val customIcon = remember(id, version, customName) { loadAssetImage(context, "icons", customName) }
    val icon = customIcon ?: xpIcon(context, registryKey)

    Box {
        Column(
            Modifier.width(86.dp).combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true }
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
            } else {
                Text(fallback, fontSize = 38.sp)
            }
            Spacer(Modifier.height(3.dp))
            Text(label, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        if (menuOpen) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(55, 28),
                onDismissRequest = { menuOpen = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier.width(150.dp).background(Color(0xFFFFF8E7)).border(1.dp, Color(0xFF777777)).padding(4.dp)
                ) {
                    Text("Open", modifier = Modifier.fillMaxWidth().clickable { menuOpen = false; onClick() }.padding(7.dp), fontSize = 12.sp)
                    Text("Change Icon...", modifier = Modifier.fillMaxWidth().clickable { menuOpen = false; onCustomize() }.padding(7.dp), fontSize = 12.sp)
                    if (customName != null) {
                        Text("Reset Icon", modifier = Modifier.fillMaxWidth().clickable { menuOpen = false; onReset() }.padding(7.dp), fontSize = 12.sp)
                    }
                    Text("Cancel", modifier = Modifier.fillMaxWidth().clickable { menuOpen = false }.padding(7.dp), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DesktopBuiltInIcon(
    context: Context,
    prefs: android.content.SharedPreferences,
    version: Int,
    id: String,
    fallback: String,
    label: String,
    onClick: () -> Unit
) {
    val image = remember(version, id) {
        loadAssetImage(context, "icons", prefs.getString(iconPrefKey(id), null)) ?: when (id) {
            "builtin_my_computer" -> xpIcon(context, "computer")
            "builtin_my_documents" -> xpIcon(context, "documents")
            "builtin_recycle_bin" -> xpIcon(context, "recycle")
            else -> null
        }
    }
    DesktopIcon(image, fallback, label, onClick)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopAppIcon(
    context: Context,
    prefs: android.content.SharedPreferences,
    version: Int,
    app: LaunchableApp,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val image = remember(version, app.packageName) {
        loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null))
    }
    var menuOpen by remember { mutableStateOf(false) }
    Box(Modifier.width(88.dp)) {
        Column(
            Modifier.width(88.dp).combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true }
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val icon = image ?: app.icon
            if (icon != null) Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(42.dp), contentScale = ContentScale.Fit)
            else Text("▣", fontSize = 37.sp)
            Text(app.label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp, maxLines = 2)
        }

        if (menuOpen) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(58, 28),
                onDismissRequest = { menuOpen = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier.width(190.dp).shadow(10.dp)
                        .background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(5.dp)
                ) {
                    Text(app.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp))
                    ContextMenuRow("Open") {
                        menuOpen = false
                        onClick()
                    }
                    ContextMenuRow("Remove from Desktop") {
                        menuOpen = false
                        onRemove()
                    }
                    ContextMenuRow("App Info") {
                        menuOpen = false
                        openAppInfo(context, app.packageName)
                    }
                    ContextMenuRow("Cancel") { menuOpen = false }
                }
            }
        }
    }
}

@Composable
private fun DesktopIcon(image: ImageBitmap?, fallback: String, label: String, onClick: () -> Unit) {
    Column(Modifier.width(88.dp).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.size(42.dp), contentScale = ContentScale.Fit)
        else Text(fallback, fontSize = 37.sp)
        Text(label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp, maxLines = 2)
    }
}

@Composable
private fun AppearanceWindow(
    context: Context,
    apps: List<LaunchableApp>,
    backgrounds: List<String>,
    iconFiles: List<String>,
    selectedBackground: String?,
    prefs: android.content.SharedPreferences,
    desktopPackages: Set<String>,
    onBackgroundSelected: (String?) -> Unit,
    onIconSelected: (String, String?) -> Unit,
    onDesktopToggle: (String, Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableStateOf("home") }
    var iconTarget by remember { mutableStateOf<String?>(null) }
    var iconTargetLabel by remember { mutableStateOf("") }

    XPWindow("Display Properties", modifier.width(350.dp), onClose = onClose) {
        when {
            iconTarget != null -> {
                Text("Choose icon for $iconTargetLabel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.height(330.dp).verticalScroll(rememberScrollState())) {
                    PickerRow("Use default", null) { onIconSelected(iconTarget!!, null); iconTarget = null }
                    iconFiles.forEach { file ->
                        val image = remember(file) { loadAssetImage(context, "icons", file) }
                        PickerRow(file.substringAfter("::", file).substringAfterLast("/"), image) { onIconSelected(iconTarget!!, file); iconTarget = null }
                    }
                    if (iconFiles.isEmpty()) Text("No custom icon images found yet.", fontSize = 11.sp, color = Color(0xFF666666), modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                XPActionButton("Back") { iconTarget = null }
            }
            page == "background" -> {
                Text("Desktop Background", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.height(330.dp).verticalScroll(rememberScrollState())) {
                    PickerRow("Windroid default", null, selectedBackground == null) { onBackgroundSelected(null) }
                    backgrounds.forEach { file ->
                        val image = remember(file) { loadAssetImage(context, "backgrounds", file) }
                        PickerRow(file, image, selectedBackground == file) { onBackgroundSelected(file) }
                    }
                    if (backgrounds.isEmpty()) Text("No images found in assets/backgrounds yet.", fontSize = 11.sp, color = Color(0xFF666666), modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                XPActionButton("Back") { page = "home" }
            }
            page == "desktopIcons" -> {
                Text("Desktop Icons", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Column(Modifier.height(330.dp).verticalScroll(rememberScrollState())) {
                    listOf(
                        "builtin_my_computer" to "My Computer",
                        "builtin_my_documents" to "My Documents",
                        "builtin_internet" to "Internet Explorer",
                        "builtin_recycle_bin" to "Recycle Bin"
                    ).forEach { (id, label) ->
                        AssignmentRow(label, prefs.getString(iconPrefKey(id), null)) {
                            iconTarget = id; iconTargetLabel = label
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                XPActionButton("Back") { page = "home" }
            }
            page == "apps" -> {
                Text("Applications", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Assign custom icons or pin apps to the XP desktop.", fontSize = 10.sp, color = Color(0xFF666666))
                Spacer(Modifier.height(8.dp))
                Column(Modifier.height(350.dp).verticalScroll(rememberScrollState())) {
                    apps.forEach { app ->
                        AppCustomizationRow(
                            app = app,
                            assignedFile = prefs.getString(iconPrefKey("app_${app.packageName}"), null),
                            onChangeIcon = {
                                iconTarget = "app_${app.packageName}"
                                iconTargetLabel = app.label
                            },
                            onToggleDesktop = { onDesktopToggle(app.packageName, app.packageName !in desktopPackages) },
                            onDesktop = app.packageName in desktopPackages
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                XPActionButton("Back") { page = "home" }
            }
            else -> {
                Text("Customize Windroid XP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                SettingsChoice("🖼️", "Desktop Background", "Choose any image from assets/backgrounds") { page = "background" }
                SettingsChoice("🖥️", "Desktop Icons", "Assign individual images to Windroid shortcuts") { page = "desktopIcons" }
                SettingsChoice("📦", "Applications", "Custom app icons and desktop shortcuts") { page = "apps" }
                Spacer(Modifier.height(14.dp))
                Text("Images added to the GitHub assets folders appear here automatically in the next build.", fontSize = 10.sp, color = Color(0xFF666666), modifier = Modifier.padding(8.dp))
                Spacer(Modifier.height(12.dp))
                XPActionButton("Close") { onClose() }
            }
        }
    }
}

@Composable
private fun SettingsChoice(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 25.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = Color(0xFF003399), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 10.sp, color = Color(0xFF555555))
        }
    }
}

@Composable
private fun PickerRow(label: String, image: ImageBitmap?, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }
            .background(if (selected) Color(0xFFDCEBFA) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(Color.White).border(1.dp, Color(0xFFB7B7B7)), contentAlignment = Alignment.Center) {
            if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else Text("Default", fontSize = 8.sp)
        }
        Spacer(Modifier.width(9.dp))
        Text(label, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AssignmentRow(label: String, assignedFile: String?, onChange: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(assignedFile ?: "Default icon", fontSize = 9.sp, color = Color(0xFF666666), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        XPActionButton("Change") { onChange() }
    }
}

@Composable
private fun AppCustomizationRow(
    app: LaunchableApp,
    assignedFile: String?,
    onChangeIcon: () -> Unit,
    onToggleDesktop: () -> Unit,
    onDesktop: Boolean
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (app.icon != null) Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(28.dp)) else Text("▣", fontSize = 20.sp)
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(assignedFile ?: "Android icon", fontSize = 8.sp, color = Color(0xFF666666), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("Icon", color = Color(0xFF003399), fontSize = 10.sp, modifier = Modifier.clickable { onChangeIcon() }.padding(5.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (onDesktop) "Remove" else "Desktop", color = Color(0xFF003399), fontSize = 10.sp, modifier = Modifier.clickable { onToggleDesktop() }.padding(5.dp))
    }
}

@Composable
private fun XPActionButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.background(Color(0xFFECE9D8), RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFF7F9DB9), RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 12.sp, color = Color.Black) }
}

@Composable
private fun TaskButton(icon: ImageBitmap?, fallback: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(end = 3.dp).size(34.dp)
            .background(Color(0xFF3579D2), RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFF6AA6F1), RoundedCornerShape(2.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(23.dp), contentScale = ContentScale.Fit)
        } else {
            Text(fallback, color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun XPSystemTray(context: Context) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxHeight().width(72.dp).padding(end = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(17.dp).clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Text("⌃", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(1.dp))
        Clock()
    }

    if (expanded) {
        androidx.compose.ui.window.Popup(
            alignment = Alignment.BottomEnd,
            offset = androidx.compose.ui.unit.IntOffset(0, -44),
            onDismissRequest = { expanded = false },
            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
        ) {
            Column(
                Modifier.width(194.dp).shadow(10.dp)
                    .background(Color(0xFFF5F4EA))
                    .border(1.dp, Color(0xFF7F9DB9))
                    .padding(5.dp)
            ) {
                ContextMenuRow("Wi-Fi settings") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                ContextMenuRow("Bluetooth settings") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
                ContextMenuRow("Flashlight / quick controls") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartMenu(
    apps: List<LaunchableApp>,
    recentApps: List<LaunchableApp>,
    context: Context,
    prefs: android.content.SharedPreferences,
    customizationVersion: Int,
    desktopPackages: Set<String>,
    userName: String,
    userAvatar: String,
    onEditProfile: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleDesktop: (LaunchableApp) -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenComputer: () -> Unit,
    onOpenControlPanel: () -> Unit,
    onOpenRecycle: () -> Unit,
    onOpenRun: () -> Unit,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xpBlue = Color(0xFF1D62C8)
    var showAllPrograms by remember { mutableStateOf(false) }
    var contextApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = contextApp != null || showSearch || showAllPrograms) {
        when {
            contextApp != null -> contextApp = null
            showSearch -> { showSearch = false; searchQuery = "" }
            else -> showAllPrograms = false
        }
    }

    Box(modifier.width(350.dp).heightIn(max = 590.dp)) {
        Column(Modifier.fillMaxSize().shadow(8.dp).border(2.dp, Color(0xFF174EA6)).background(Color.White)) {
            Row(
                Modifier.fillMaxWidth().height(68.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF2F7BDC), Color(0xFF1855B6))))
                    .clickable { onEditProfile() }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(48.dp).background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color(0xFFB7CBE6), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Text(userAvatar, fontSize = 28.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Tap to change account", color = Color(0xFFDDEBFF), fontSize = 9.sp)
                }
            }

            Row(Modifier.weight(1f)) {
                Column(Modifier.weight(1.35f).fillMaxHeight().background(Color.White)) {
                    if (showAllPrograms) {
                        Row(Modifier.fillMaxWidth().clickable { showAllPrograms = false }.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("◀", fontSize = 14.sp, color = Color(0xFF174EA6))
                            Spacer(Modifier.width(8.dp))
                            Text("Back", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF174EA6))
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD6D6D6)))
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            apps.forEach { app ->
                                StartMenuAppItem(
                                    context = context,
                                    prefs = prefs,
                                    version = customizationVersion,
                                    app = app,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { contextApp = app }
                                )
                            }
                        }
                    } else {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            StartMenuItem("🌐", "Internet") { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) }
                            StartMenuItem("📧", "E-mail") { }
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD6D6D6)))
                            if (recentApps.isEmpty()) {
                                Text("Recently used programs will appear here.", color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp))
                            } else {
                                recentApps.forEach { app ->
                                    StartMenuAppItem(
                                        context = context,
                                        prefs = prefs,
                                        version = customizationVersion,
                                        app = app,
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { contextApp = app }
                                    )
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD6D6D6)))
                        Row(Modifier.fillMaxWidth().clickable { showAllPrograms = true }.padding(horizontal = 10.dp, vertical = 10.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            Text("All Programs", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Text("▶", color = Color(0xFF248B23), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(Modifier.weight(0.95f).fillMaxHeight().background(Color(0xFFDCEBFA)).padding(vertical = 7.dp)) {
                    RightMenuAssetItem(context, "documents", "My Documents") { openDocuments(context) }
                    RightMenuAssetItem(context, "computer", "My Computer") { onOpenComputer() }
                    RightMenuAssetItem(context, "recycle", "Recycle Bin") { onOpenRecycle() }
                    Spacer(Modifier.height(5.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFB4CCE7))); Spacer(Modifier.height(5.dp))
                    RightMenuAssetItem(context, "control", "Control Panel") { onOpenControlPanel() }
                    RightMenuAssetItem(context, "appearance", "Appearance") { onOpenAppearance() }
                    RightMenuAssetItem(context, "settings", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    RightMenuAssetItem(context, "update", "Windows Update") { onCheckUpdates() }
                    RightMenuAssetItem(context, "search", "Search") {
                        showSearch = true
                        showAllPrograms = false
                        contextApp = null
                    }
                    RightMenuAssetItem(context, "run", "Run...") { onOpenRun() }
                    RightMenuItem("❓", "Help and Support") { }
                }
            }

            Row(Modifier.fillMaxWidth().height(47.dp).background(xpBlue).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("🔑 Log Off", color = Color.White, fontSize = 12.sp)
                Spacer(Modifier.width(16.dp))
                Text("⏻ Turn Off Computer", color = Color.White, fontSize = 12.sp)
            }
        }

        if (showSearch) {
            Column(
                Modifier.align(Alignment.Center).width(290.dp).heightIn(max = 470.dp).shadow(12.dp)
                    .background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(10.dp)
            ) {
                Text("Search Programs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(60) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .border(1.dp, Color(0xFF7F9DB9)).padding(horizontal = 8.dp, vertical = 7.dp)
                )
                Spacer(Modifier.height(8.dp))
                val normalizedQuery = searchQuery.trim()
                val results = if (normalizedQuery.isBlank()) {
                    apps
                } else {
                    apps.filter {
                        it.label.contains(normalizedQuery, ignoreCase = true) ||
                            it.packageName.contains(normalizedQuery, ignoreCase = true)
                    }.sortedWith(
                        compareBy<LaunchableApp> {
                            when {
                                it.label.equals(normalizedQuery, ignoreCase = true) -> 0
                                it.label.startsWith(normalizedQuery, ignoreCase = true) -> 1
                                else -> 2
                            }
                        }.thenBy { it.label.lowercase() }
                    )
                }
                Column(Modifier.weight(1f, fill = false).heightIn(max = 330.dp).verticalScroll(rememberScrollState())) {
                    if (results.isEmpty()) {
                        Text("No programs found.", color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                    } else {
                        results.take(60).forEach { app ->
                            StartMenuAppItem(
                                context = context,
                                prefs = prefs,
                                version = customizationVersion,
                                app = app,
                                onClick = {
                                    showSearch = false
                                    searchQuery = ""
                                    onLaunchApp(app)
                                },
                                onLongClick = {
                                    showSearch = false
                                    contextApp = app
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                XPActionButton("Close") { showSearch = false; searchQuery = "" }
            }
        }

        contextApp?.let { app ->
            Column(
                Modifier.align(Alignment.Center).width(235.dp).shadow(12.dp)
                    .background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(6.dp)
            ) {
                Text(app.label, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(7.dp))
                ContextMenuRow(if (app.packageName in desktopPackages) "Remove from Desktop" else "Add to Desktop") {
                    onToggleDesktop(app)
                    contextApp = null
                }
                ContextMenuRow("App Info") {
                    openAppInfo(context, app.packageName)
                    contextApp = null
                }
                ContextMenuRow("Cancel") { contextApp = null }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartMenuAppItem(
    context: Context,
    prefs: android.content.SharedPreferences,
    version: Int,
    app: LaunchableApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val custom = remember(version, app.packageName) {
        loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null))
    }
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = custom ?: app.icon
        if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(28.dp)) else Text("▣", fontSize = 22.sp)
        Spacer(Modifier.width(9.dp))
        Text(app.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ContextMenuRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 12.sp,
        color = Color.Black,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 10.dp, vertical = 9.dp)
    )
}

@Composable
private fun StartMenuItem(icon: String, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(9.dp))
        Text(label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RightMenuItem(icon: String, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(7.dp))
        Text(label, color = Color(0xFF163C73), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun XPSystemRow(context: Context, iconKey: String, label: String, onClick: () -> Unit) {
    val icon = remember(iconKey) { xpIcon(context, iconKey) }
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(30.dp), contentScale = ContentScale.Fit)
        else Box(Modifier.size(30.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, color = Color(0xFF003399), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RightMenuAssetItem(context: Context, iconKey: String, label: String, onClick: () -> Unit) {
    val icon = remember(iconKey) { xpIcon(context, iconKey) }
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(20.dp), contentScale = ContentScale.Fit)
        else Box(Modifier.size(20.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = Color(0xFF163C73), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RunWindow(
    apps: List<LaunchableApp>,
    context: Context,
    onLaunch: (LaunchableApp) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query, apps) {
        if (query.isBlank()) emptyList() else apps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }.take(6)
    }
    XPWindow("Run", modifier, onClose = onClose) {
        Text("Type the name of a program, package, or command.", fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it.take(80) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(7.dp)
        )
        if (matches.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            matches.forEach { app ->
                Row(Modifier.fillMaxWidth().clickable { onLaunch(app) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (app.icon != null) Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(app.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XPActionButton("OK") {
                when (query.trim().lowercase()) {
                    "settings", "control" -> context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    "files", "documents", "explorer" -> openDocuments(context)
                    "browser", "internet", "iexplore" -> openDefaultBrowser(context)
                    else -> matches.firstOrNull()?.let(onLaunch)
                }
            }
            XPActionButton("Cancel") { onClose() }
        }
    }
}

@Composable
private fun ProfileWindow(
    currentName: String,
    currentAvatar: String,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draftName by remember(currentName) { mutableStateOf(currentName) }
    var draftAvatar by remember(currentAvatar) { mutableStateOf(currentAvatar) }
    val avatars = listOf("🙂", "😎", "🤖", "🐺", "🦊", "🐱", "👾", "🧑")

    XPWindow("User Accounts", modifier, onClose = onCancel) {
        Text("Pick a name and picture for your account.", fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Text("User name", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = draftName,
            onValueChange = { if (it.length <= 24) draftName = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(7.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Account picture", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            avatars.forEach { avatar ->
                Box(
                    Modifier.padding(end = 7.dp).size(42.dp).background(Color.White, RoundedCornerShape(4.dp))
                        .border(if (avatar == draftAvatar) 2.dp else 1.dp, if (avatar == draftAvatar) Color(0xFF245EDB) else Color(0xFFB7B7B7), RoundedCornerShape(4.dp))
                        .clickable { draftAvatar = avatar },
                    contentAlignment = Alignment.Center
                ) { Text(avatar, fontSize = 25.sp) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XPActionButton("Save") { onSave(draftName, draftAvatar) }
            XPActionButton("Cancel") { onCancel() }
        }
    }
}

@Composable
private fun XPWindow(
    title: String,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val closeImage = remember { loadAssetImage(context, "icons", "close_button.png") }

    Column(modifier.width(316.dp).shadow(10.dp).background(Color(0xFFECE9D8)).border(2.dp, Color(0xFF245EDB))) {
        Row(
            Modifier.fillMaxWidth().height(31.dp)
                .background(Brush.horizontalGradient(listOf(Color(0xFF0A56D8), Color(0xFF3A8AF1))))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            val closeModifier = if (onClose != null) Modifier.clickable { onClose() } else Modifier
            if (closeImage != null && onClose != null) {
                Image(
                    bitmap = closeImage,
                    contentDescription = "Close",
                    modifier = closeModifier.size(22.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    closeModifier.size(20.dp).background(if (onClose != null) Color(0xFFE95B45) else Color(0xFF999999), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = Date()
        }
    }
    Text(
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(now),
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 1
    )
}
