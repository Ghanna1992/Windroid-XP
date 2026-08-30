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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WindroidDesktop(this) }
    }
}

data class LaunchableApp(val label: String, val packageName: String, val icon: ImageBitmap? = null)

private const val DEFAULT_DESKTOP_BACKGROUND = "windows_xp_bliss-wide.jpg"

private fun installedApps(context: Context): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val pm = context.packageManager
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map { info ->
            val label = info.loadLabel(pm).toString()
            val packageName = info.activityInfo.packageName
            val realIcon = try { info.loadIcon(pm).toBitmap(96, 96).asImageBitmap() } catch (_: Exception) { null }
            LaunchableApp(label, packageName, defaultXpAppIcon(context, packageName, label) ?: realIcon)
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

private fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {
    val pkg = packageName.lowercase(); val name = label.lowercase(); val normalizedLabel = name.replace(Regex("[^a-z0-9]"), "")
    val appNamedIcon = listAssetImages(context, "icons/apps").firstOrNull { file ->
        val base = file.substringAfter("::", file).substringAfterLast('/').substringBeforeLast('.').lowercase()
        val normalizedFile = base.replace(Regex("[^a-z0-9]"), "")
        normalizedFile == normalizedLabel || (name == "google home" && normalizedFile == "home") || (name.contains("baby plus") && normalizedFile == "babyplus") || (name.contains("1kosmos") && normalizedFile == "1kosmos")
    }
    if (appNamedIcon != null) loadAssetImage(context, "icons/apps", appNamedIcon)?.let { return it }
    val asset = when {
        pkg in setOf("com.android.chrome", "com.opera.browser", "com.opera.gx", "org.mozilla.firefox", "com.microsoft.emmx", "com.sec.android.app.sbrowser") || name in setOf("chrome", "opera", "opera gx", "firefox", "microsoft edge", "samsung internet") -> "Internet Explorer 6.png"
        pkg in setOf("com.google.android.gm", "com.samsung.android.email.provider", "com.samsung.android.email.ui") || name in setOf("gmail", "email", "samsung email") -> "Outlook Express.png"
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

private fun launchApp(context: Context, app: LaunchableApp) { context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); context.startActivity(it) } }
private fun openAppInfo(context: Context, packageName: String) { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:$packageName"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }

private fun listAssetImages(context: Context, folder: String): List<String> {
    val allowed = setOf("png", "jpg", "jpeg", "webp"); val results = mutableListOf<String>()
    fun scan(assetFolder: String, relativePrefix: String = "") {
        val children = try { context.assets.list(assetFolder)?.toList().orEmpty() } catch (_: Exception) { emptyList() }
        for (name in children) {
            val full = "$assetFolder/$name"; val relative = if (relativePrefix.isBlank()) name else "$relativePrefix/$name"; val ext = name.substringAfterLast('.', "").lowercase()
            when {
                ext in allowed -> results.add(relative)
                ext == "zip" -> try { context.assets.open(full).use { raw -> java.util.zip.ZipInputStream(raw).use { zip -> var entry = zip.nextEntry; while (entry != null) { if (!entry.isDirectory && entry.name.substringAfterLast('.', "").lowercase() in allowed) results.add("$relative::${entry.name}"); zip.closeEntry(); entry = zip.nextEntry } } } } catch (_: Exception) { }
                ext.isBlank() -> scan(full, relative)
            }
        }
    }
    return try { scan(folder); results.sortedBy { it.substringAfter("::", it).lowercase() } } catch (_: Exception) { emptyList() }
}

private val assetImageCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap?>()
private fun loadAssetImage(context: Context, folder: String, fileName: String?): ImageBitmap? {
    if (fileName.isNullOrBlank()) return null; val key = "$folder::$fileName"; if (assetImageCache.containsKey(key)) return assetImageCache[key]
    fun candidatePaths(name: String): List<String> = if (folder != "icons" || name.contains('/')) listOf("$folder/$name") else listOf("icons/$name", "icons/xp/$name", "icons/apps/$name", "icons/ui/$name")
    val image = try {
        if (fileName.contains("::")) {
            val zipName = fileName.substringBefore("::"); val entryName = fileName.substringAfter("::"); var found: ImageBitmap? = null
            for (path in candidatePaths(zipName)) { try { context.assets.open(path).use { raw -> java.util.zip.ZipInputStream(raw).use { zip -> var entry = zip.nextEntry; while (entry != null) { if (!entry.isDirectory && entry.name == entryName) { val bytes = java.io.ByteArrayOutputStream(); zip.copyTo(bytes); found = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())?.asImageBitmap(); break }; zip.closeEntry(); entry = zip.nextEntry } } } } catch (_: Exception) { }; if (found != null) break }; found
        } else { var found: ImageBitmap? = null; for (path in candidatePaths(fileName)) { try { found = context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } } catch (_: Exception) { }; if (found != null) break }; found }
    } catch (_: Exception) { null }
    if (image != null) assetImageCache[key] = image; return image
}

private fun loadStartButtonImage(context: Context): ImageBitmap? = loadAssetImage(context, "icons", "ui/start_button.png")
private fun iconPrefKey(id: String) = "custom_icon_$id"
private fun desktopPositionKey(id: String, axis: String) = "desktop_pos_${id}_$axis"
private val XP_ICON_REGISTRY = mapOf("computer" to "My Computer.png", "documents" to "My Documents.png", "internet" to "Internet Explorer 6.png", "recycle" to "Recycle Bin (empty).png", "control" to "Control Panel.png", "appearance" to "Appearance.png", "programs" to "Change or Remove Programs.png", "network" to "Connection Status.png", "settings" to "Additional Settings.png", "back" to "Back.png", "storage" to "Hard Disk.png", "search" to "Search.png", "run" to "Run.png", "update" to "Windows Update.png", "user" to "User Accounts.png", "power" to "Power.png")
private fun xpIcon(context: Context, key: String): ImageBitmap? = XP_ICON_REGISTRY[key]?.let { loadAssetImage(context, "icons", it) }
private fun openDefaultBrowser(context: Context) { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
private fun openDocuments(context: Context) { try { context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) } }

@Composable
fun WindroidDesktop(context: Context) {
    val prefs = remember { context.getSharedPreferences("windroid_prefs", Context.MODE_PRIVATE) }; val apps = remember { installedApps(context) }
    var startOpen by remember { mutableStateOf(false) }; var computerOpen by remember { mutableStateOf(false) }; var settingsOpen by remember { mutableStateOf(false) }; var recycleOpen by remember { mutableStateOf(false) }; var updateWindowOpen by remember { mutableStateOf(false) }
    var selectedBackground by remember { mutableStateOf(prefs.getString("desktop_background", DEFAULT_DESKTOP_BACKGROUND) ?: DEFAULT_DESKTOP_BACKGROUND) }
    var customizationVersion by remember { mutableIntStateOf(0) }; var desktopPackages by remember { mutableStateOf(prefs.getStringSet("desktop_apps", emptySet())?.toSet() ?: emptySet()) }; var removedDesktopPackages by remember { mutableStateOf(prefs.getStringSet("removed_desktop_apps", emptySet())?.toSet() ?: emptySet()) }; var hiddenBuiltinShortcuts by remember { mutableStateOf(prefs.getStringSet("hidden_builtin_shortcuts", emptySet())?.toSet() ?: emptySet()) }
    val launchedApps = remember { mutableStateListOf<LaunchableApp>() }; val startButtonImage = remember { loadStartButtonImage(context) }; val updateScope = rememberCoroutineScope(); var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }; var updateStatus by remember { mutableStateOf("") }; var downloadedUpdate by remember { mutableStateOf<File?>(null) }
    fun setIcon(id: String, fileName: String?) { prefs.edit().apply { if (fileName == null) remove(iconPrefKey(id)) else putString(iconPrefKey(id), fileName) }.apply(); customizationVersion++ }
    fun setBuiltinShortcutVisible(id: String, visible: Boolean) { hiddenBuiltinShortcuts = if (visible) hiddenBuiltinShortcuts - id else hiddenBuiltinShortcuts + id; prefs.edit().putStringSet("hidden_builtin_shortcuts", hiddenBuiltinShortcuts).apply() }
    fun setDesktopApp(packageName: String, enabled: Boolean) { desktopPackages = if (enabled) desktopPackages + packageName else desktopPackages - packageName; removedDesktopPackages = if (enabled) removedDesktopPackages - packageName else removedDesktopPackages + packageName; prefs.edit().putStringSet("desktop_apps", desktopPackages).putStringSet("removed_desktop_apps", removedDesktopPackages).apply() }
    fun openAndroidApp(app: LaunchableApp) { if (launchedApps.none { it.packageName == app.packageName }) launchedApps.add(app); startOpen = false; launchApp(context, app) }
    fun checkForUpdates() { updateStatus = "Checking for updates..."; updateWindowOpen = true; updateScope.launch { when (val result = UpdateManager.checkForUpdate()) { is UpdateManager.CheckResult.UpdateAvailable -> { updateInfo = result.update; updateStatus = "Windroid XP ${result.update.versionName} is ready." }; UpdateManager.CheckResult.UpToDate -> { updateInfo = null; updateStatus = "Your computer is up to date." }; is UpdateManager.CheckResult.Failed -> { updateInfo = null; updateStatus = result.message } } } }
    val xpBlue = Color(0xFF245EDB); val taskbarHeight = 43.dp

    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
        WallpaperLayer(context, selectedBackground)
        BoxWithConstraints(Modifier.fillMaxSize().padding(start = 10.dp, top = 12.dp, end = 8.dp, bottom = taskbarHeight + 8.dp)) {
            val desktopApps = apps.filter { it.packageName in desktopPackages }; val builtinShortcuts = listOf("builtin_my_computer", "builtin_my_documents", "builtin_internet", "builtin_recycle_bin").filterNot { it in hiddenBuiltinShortcuts }
            val slotHeight = 94f; val slotWidth = 96f; val rowsPerColumn = (maxHeight.value / slotHeight).toInt().coerceAtLeast(1)
            val desktopWidth = maxWidth; val desktopHeight = maxHeight
            fun defaultX(index: Int) = (index / rowsPerColumn) * slotWidth; fun defaultY(index: Int) = (index % rowsPerColumn) * slotHeight
            Box(Modifier.fillMaxSize()) {
                builtinShortcuts.forEachIndexed { index, id -> MovableDesktopItem(prefs, id, defaultX(index), defaultY(index), desktopWidth, desktopHeight) {
                    when (id) {
                        "builtin_my_computer" -> DesktopSystemShortcut(context, prefs, customizationVersion, id, "computer", "🖥️", "My Computer", { computerOpen = true }, { settingsOpen = true }, { setIcon(id, null) }, { setBuiltinShortcutVisible(id, false) })
                        "builtin_my_documents" -> DesktopSystemShortcut(context, prefs, customizationVersion, id, "documents", "📁", "My Documents", { openDocuments(context) }, { settingsOpen = true }, { setIcon(id, null) }, { setBuiltinShortcutVisible(id, false) })
                        "builtin_internet" -> DesktopSystemShortcut(context, prefs, customizationVersion, id, "internet", "🌐", "Internet Explorer", { openDefaultBrowser(context) }, { settingsOpen = true }, { setIcon(id, null) }, { setBuiltinShortcutVisible(id, false) })
                        "builtin_recycle_bin" -> DesktopSystemShortcut(context, prefs, customizationVersion, id, "recycle", "🗑️", "Recycle Bin", { recycleOpen = true }, { settingsOpen = true }, { setIcon(id, null) }, { setBuiltinShortcutVisible(id, false) })
                    }
                } }
                desktopApps.forEachIndexed { appIndex, app -> val index = builtinShortcuts.size + appIndex; MovableDesktopItem(prefs, "app_${app.packageName}", defaultX(index), defaultY(index), desktopWidth, desktopHeight) { DesktopAppIcon(context, prefs, customizationVersion, app, { openAndroidApp(app) }, { setDesktopApp(app.packageName, false) }) } }
            }
        }

        if (computerOpen) XPWindow("My Computer", Modifier.align(Alignment.Center), onClose = { computerOpen = false }) { XPSystemRow(context, "storage", "Internal Storage") { openDocuments(context) }; XPSystemRow(context, "settings", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
        if (recycleOpen) XPWindow("Recycle Bin", Modifier.align(Alignment.Center), onClose = { recycleOpen = false }) { val removedApps = apps.filter { it.packageName in removedDesktopPackages }; if (removedApps.isEmpty()) Text("The Recycle Bin is empty.") else removedApps.forEach { app -> ContextMenuRow("Restore ${app.label}") { setDesktopApp(app.packageName, true) } } }
        if (settingsOpen) XPWindow("Display Properties", Modifier.align(Alignment.Center), onClose = { settingsOpen = false }) { Text("Desktop Background", fontWeight = FontWeight.Bold); XPActionButton("Use Bliss") { selectedBackground = DEFAULT_DESKTOP_BACKGROUND; prefs.edit().putString("desktop_background", selectedBackground).apply() }; Spacer(Modifier.height(8.dp)); Text("Desktop shortcuts can be dragged freely and may overlap.", fontSize = 11.sp) }
        if (updateWindowOpen) XPWindow("Windows Update", Modifier.align(Alignment.Center), onClose = { updateWindowOpen = false }) { Text(updateStatus); updateInfo?.let { found -> XPActionButton("Download and install") { updateScope.launch { val file = UpdateManager.downloadUpdate(context, found); if (file != null) { downloadedUpdate = file; UpdateManager.installUpdate(context, file) } } } } }
        if (startOpen) StartMenu(apps, context, onLaunchApp = { openAndroidApp(it) }, onToggleDesktop = { setDesktopApp(it.packageName, it.packageName !in desktopPackages) }, onOpenComputer = { computerOpen = true; startOpen = false }, onOpenAppearance = { settingsOpen = true; startOpen = false }, onOpenRecycle = { recycleOpen = true; startOpen = false }, onCheckUpdates = { checkForUpdates() }, modifier = Modifier.align(Alignment.BottomStart).padding(bottom = taskbarHeight))

        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(taskbarHeight).background(Brush.verticalGradient(listOf(Color(0xFF3886E8), xpBlue, Color(0xFF1748AF)))), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.fillMaxHeight().width(108.dp).clickable { startOpen = !startOpen }, contentAlignment = Alignment.CenterStart) { if (startButtonImage != null) Image(bitmap = startButtonImage, contentDescription = "Start", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) else Text("start", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp)) }
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) { launchedApps.forEach { app -> TaskButton(app.icon, "▣", app.label) { openAndroidApp(app) } } }
            XPSystemTray(context)
        }
    }
}

@Composable
private fun MovableDesktopItem(prefs: android.content.SharedPreferences, positionId: String, defaultX: Float, defaultY: Float, maxWidth: androidx.compose.ui.unit.Dp, maxHeight: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    val density = LocalDensity.current; val maxXPx = with(density) { (maxWidth - 88.dp).coerceAtLeast(0.dp).toPx() }; val maxYPx = with(density) { (maxHeight - 82.dp).coerceAtLeast(0.dp).toPx() }; val defaultXPx = with(density) { defaultX.dp.toPx() }; val defaultYPx = with(density) { defaultY.dp.toPx() }
    val keyX = desktopPositionKey(positionId, "x"); val keyY = desktopPositionKey(positionId, "y")
    var position by remember(positionId, maxXPx, maxYPx) { mutableStateOf(Offset(prefs.getFloat(keyX, defaultXPx).coerceIn(0f, maxXPx), prefs.getFloat(keyY, defaultYPx).coerceIn(0f, maxYPx))) }; var dragging by remember(positionId) { mutableStateOf(false) }
    Box(Modifier.offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }.zIndex(if (dragging) 100f else 0f).pointerInput(positionId, maxXPx, maxYPx) { detectDragGestures(onDragStart = { dragging = true }, onDragEnd = { dragging = false; prefs.edit().putFloat(keyX, position.x).putFloat(keyY, position.y).apply() }, onDragCancel = { dragging = false }) { _, dragAmount -> position = Offset((position.x + dragAmount.x).coerceIn(0f, maxXPx), (position.y + dragAmount.y).coerceIn(0f, maxYPx)) } }) { content() }
}

@Composable private fun WallpaperLayer(context: Context, selectedBackground: String?) { val image = remember(selectedBackground) { loadAssetImage(context, "backgrounds", selectedBackground ?: DEFAULT_DESKTOP_BACKGROUND) }; if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else XPWallpaper() }
@Composable private fun XPWallpaper() { Canvas(Modifier.fillMaxSize()) { drawRect(Brush.verticalGradient(listOf(Color(0xFF2087D5), Color(0xFF55B6ED), Color(0xFFBDEAFF)))); drawRect(Color(0xFF4DAA35), topLeft = Offset(0f, size.height * .7f)) } }

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun DesktopSystemShortcut(context: Context, prefs: android.content.SharedPreferences, version: Int, id: String, registryKey: String, fallback: String, label: String, onClick: () -> Unit, onCustomize: () -> Unit, onReset: () -> Unit, onRemove: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }; val customName = remember(id, version) { prefs.getString(iconPrefKey(id), null) }; val icon = remember(customName, version) { loadAssetImage(context, "icons", customName) } ?: xpIcon(context, registryKey)
    Box { Column(Modifier.width(88.dp).combinedClickable(onClick = onClick, onLongClick = { menuOpen = true }), horizontalAlignment = Alignment.CenterHorizontally) { if (icon != null) Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit) else Text(fallback, fontSize = 38.sp); Spacer(Modifier.height(3.dp)); Text(label, color = Color.White, fontSize = 12.sp, maxLines = 2) }; if (menuOpen) Column(Modifier.background(Color(0xFFF5F4EA)).border(1.dp, Color.Gray).padding(4.dp)) { ContextMenuRow("Open") { menuOpen = false; onClick() }; ContextMenuRow("Change Icon...") { menuOpen = false; onCustomize() }; ContextMenuRow("Remove from Desktop") { menuOpen = false; onRemove() } } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun DesktopAppIcon(context: Context, prefs: android.content.SharedPreferences, version: Int, app: LaunchableApp, onClick: () -> Unit, onRemove: () -> Unit) {
    val image = remember(version, app.packageName) { loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null)) }; var menuOpen by remember { mutableStateOf(false) }; val icon = image ?: app.icon
    Box(Modifier.width(88.dp)) { Column(Modifier.width(88.dp).combinedClickable(onClick = onClick, onLongClick = { menuOpen = true }), horizontalAlignment = Alignment.CenterHorizontally) { if (icon != null) Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit) else Text("▣", fontSize = 38.sp); Spacer(Modifier.height(3.dp)); Text(app.label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp, maxLines = 2) }; if (menuOpen) Column(Modifier.background(Color(0xFFF5F4EA)).border(1.dp, Color.Gray).padding(4.dp)) { ContextMenuRow("Open") { menuOpen = false; onClick() }; ContextMenuRow("Remove from Desktop") { menuOpen = false; onRemove() }; ContextMenuRow("App Info") { menuOpen = false; openAppInfo(context, app.packageName) } } }
}

@Composable private fun XPActionButton(label: String, onClick: () -> Unit) { Box(Modifier.background(Color(0xFFECE9D8)).border(1.dp, Color(0xFF7F9DB9)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp)) { Text(label, fontSize = 12.sp) } }
@Composable private fun ContextMenuRow(label: String, onClick: () -> Unit) { Text(label, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp), fontSize = 12.sp) }
@Composable private fun XPSystemRow(context: Context, iconKey: String, label: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { xpIcon(context, iconKey)?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(30.dp)) }; Spacer(Modifier.width(8.dp)); Text(label, color = Color(0xFF003399)) } }
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun TaskButton(icon: ImageBitmap?, fallback: String, label: String, onClick: () -> Unit) { Box(Modifier.padding(3.dp).size(34.dp).background(Color(0xFF3579D2)).combinedClickable(onClick = onClick), contentAlignment = Alignment.Center) { if (icon != null) Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(23.dp)) else Text(fallback, color = Color.White) } }
@Composable private fun XPSystemTray(context: Context) { Row(Modifier.fillMaxHeight().width(72.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { Text("⌃", color = Color.White); Spacer(Modifier.width(3.dp)); Clock() } }
@Composable private fun Clock() { var now by remember { mutableStateOf(Date()) }; LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); now = Date() } }; Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(now), color = Color.White, fontSize = 13.sp) }

@Composable private fun StartMenu(apps: List<LaunchableApp>, context: Context, onLaunchApp: (LaunchableApp) -> Unit, onToggleDesktop: (LaunchableApp) -> Unit, onOpenComputer: () -> Unit, onOpenAppearance: () -> Unit, onOpenRecycle: () -> Unit, onCheckUpdates: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.width(340.dp).heightIn(max = 560.dp).background(Color.White).border(2.dp, Color(0xFF174EA6))) { Row(Modifier.fillMaxWidth().height(55.dp).background(Color(0xFF245EDB)), verticalAlignment = Alignment.CenterVertically) { Text("  Windroid XP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }; Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { apps.take(20).forEach { app -> Row(Modifier.fillMaxWidth().combinedClickable(onClick = { onLaunchApp(app) }, onLongClick = { onToggleDesktop(app) }).padding(8.dp), verticalAlignment = Alignment.CenterVertically) { app.icon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(28.dp)) }; Spacer(Modifier.width(8.dp)); Text(app.label) } } }; Row(Modifier.fillMaxWidth().background(Color(0xFFDCEBFA)), horizontalArrangement = Arrangement.SpaceEvenly) { Text("My Computer", modifier = Modifier.clickable(onClick = onOpenComputer).padding(10.dp)); Text("Appearance", modifier = Modifier.clickable(onClick = onOpenAppearance).padding(10.dp)); Text("Recycle", modifier = Modifier.clickable(onClick = onOpenRecycle).padding(10.dp)); Text("Update", modifier = Modifier.clickable(onClick = onCheckUpdates).padding(10.dp)) } }
}

@Composable private fun XPWindow(title: String, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) { Column(modifier.width(316.dp).shadow(10.dp).background(Color(0xFFECE9D8)).border(2.dp, Color(0xFF245EDB))) { Row(Modifier.fillMaxWidth().height(31.dp).background(Color(0xFF0A56D8)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); if (onClose != null) Text("×", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onClose)) }; Column(Modifier.padding(18.dp), content = content) } }
