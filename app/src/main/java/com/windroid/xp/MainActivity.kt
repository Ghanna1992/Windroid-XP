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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
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

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap? = null
)

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
            val icon = defaultXpAppIcon(context, packageName, label) ?: realIcon
            LaunchableApp(label = label, packageName = packageName, icon = icon)
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {
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

private fun iconPrefKey(id: String) = "custom_icon_$id"
private fun desktopPositionKey(id: String, axis: String) = "desktop_pos_${id}_$axis"

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

private fun xpIcon(context: Context, key: String): ImageBitmap? = XP_ICON_REGISTRY[key]?.let { loadAssetImage(context, "icons", it) }
private fun openDefaultBrowser(context: Context) { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
private fun openDocuments(context: Context) { try { context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) } }

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
    var aboutOpen by remember { mutableStateOf(false) }
    var resetConfirmOpen by remember { mutableStateOf(false) }
    var taskbarContextApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var desktopContextMenuOpen by remember { mutableStateOf(false) }
    var setupComplete by remember { mutableStateOf(prefs.getBoolean("setup_complete", false)) }
    val previousStartupComplete = remember { prefs.getBoolean("startup_completed", true) }
    var recoveryNotice by remember { mutableStateOf(!previousStartupComplete && setupComplete) }

    val savedRecentPackages = remember { prefs.getString("recent_apps", "").orEmpty().split('\n').map { it.trim() }.filter { it.isNotEmpty() } }
    val launchedApps = remember(apps) { mutableStateListOf<LaunchableApp>().apply { savedRecentPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }.forEach { add(it) } } }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "User") ?: "User") }
    var userAvatar by remember { mutableStateOf(prefs.getString("user_avatar", "🙂") ?: "🙂") }
    var selectedBackground by remember { mutableStateOf(prefs.getString("desktop_background", DEFAULT_DESKTOP_BACKGROUND) ?: DEFAULT_DESKTOP_BACKGROUND) }
    var customizationVersion by remember { mutableIntStateOf(0) }
    var desktopPackages by remember { mutableStateOf(prefs.getStringSet("desktop_apps", emptySet())?.toSet() ?: emptySet()) }
    var removedDesktopPackages by remember { mutableStateOf(prefs.getStringSet("removed_desktop_apps", emptySet())?.toSet() ?: emptySet()) }
    var hiddenBuiltinShortcuts by remember { mutableStateOf(prefs.getStringSet("hidden_builtin_shortcuts", emptySet())?.toSet() ?: emptySet()) }

    val backgrounds = remember { listAssetImages(context, "backgrounds") }
    val iconFiles = remember { listAssetImages(context, "icons").filterNot {
        val n = it.substringAfter("::", it).substringAfterLast("/").lowercase()
        it.startsWith("ui/") || n == "start_button.png" || n == "close_button.png" || n.contains("button") || n.contains("cursor")
    } }
    val startButtonImage = remember { loadStartButtonImage(context) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var updateStatus by remember { mutableStateOf("") }
    var downloadedUpdate by remember { mutableStateOf<File?>(null) }
    var updateProgress by remember { mutableIntStateOf(-1) }
    val updateScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { prefs.edit().putBoolean("startup_completed", false).apply(); kotlinx.coroutines.delay(1800); prefs.edit().putBoolean("startup_completed", true).apply() }
    fun saveRecentApps() { prefs.edit().putString("recent_apps", launchedApps.take(12).joinToString("\n") { it.packageName }).apply() }
    fun removeTaskbarApp(app: LaunchableApp) { launchedApps.removeAll { it.packageName == app.packageName }; saveRecentApps(); taskbarContextApp = null }
    fun openAndroidApp(app: LaunchableApp) { launchedApps.removeAll { it.packageName == app.packageName }; launchedApps.add(0, app); while (launchedApps.size > 12) launchedApps.removeAt(launchedApps.lastIndex); saveRecentApps(); startOpen = false; computerOpen = false; profileOpen = false; settingsOpen = false; launchApp(context, app) }
    fun setIcon(id: String, fileName: String?) { prefs.edit().apply { if (fileName == null) remove(iconPrefKey(id)) else putString(iconPrefKey(id), fileName) }.apply(); customizationVersion++ }
    fun setBuiltinShortcutVisible(id: String, visible: Boolean) { hiddenBuiltinShortcuts = if (visible) hiddenBuiltinShortcuts - id else hiddenBuiltinShortcuts + id; prefs.edit().putStringSet("hidden_builtin_shortcuts", hiddenBuiltinShortcuts).apply() }
    fun resetWindroid() {
        val editor = prefs.edit().remove("desktop_background").remove("desktop_apps").remove("removed_desktop_apps").remove("hidden_builtin_shortcuts").remove("recent_apps").remove("user_name").remove("user_avatar")
        prefs.all.keys.filter { it.startsWith("custom_icon_") || it.startsWith("desktop_pos_") }.forEach { editor.remove(it) }; editor.apply()
        selectedBackground = DEFAULT_DESKTOP_BACKGROUND; desktopPackages = emptySet(); removedDesktopPackages = emptySet(); hiddenBuiltinShortcuts = emptySet(); launchedApps.clear(); userName = "User"; userAvatar = "🙂"; customizationVersion++
    }
    fun setDesktopApp(packageName: String, enabled: Boolean) { desktopPackages = if (enabled) desktopPackages + packageName else desktopPackages - packageName; removedDesktopPackages = if (enabled) removedDesktopPackages - packageName else removedDesktopPackages + packageName; prefs.edit().putStringSet("desktop_apps", desktopPackages).putStringSet("removed_desktop_apps", removedDesktopPackages).apply() }
    fun checkForUpdates(manual: Boolean) { startOpen = false; updateProgress = -1; updateStatus = "Checking for updates..."; if (manual) updateWindowOpen = true; updateScope.launch { downloadedUpdate = null; when (val result = UpdateManager.checkForUpdate()) { is UpdateManager.CheckResult.UpdateAvailable -> { updateInfo = result.update; updateStatus = "Windroid XP ${result.update.versionName} is ready."; updateWindowOpen = true }; UpdateManager.CheckResult.UpToDate -> { updateInfo = null; updateStatus = "Your computer is up to date."; updateWindowOpen = manual }; is UpdateManager.CheckResult.Failed -> { updateInfo = null; updateStatus = result.message; updateWindowOpen = manual } } } }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2500); when (val result = UpdateManager.checkForUpdate()) { is UpdateManager.CheckResult.UpdateAvailable -> { updateInfo = result.update; updateStatus = "Windroid XP ${result.update.versionName} is ready."; updateWindowOpen = true }; else -> Unit } }

    BackHandler { when { taskbarContextApp != null -> taskbarContextApp = null; resetConfirmOpen -> resetConfirmOpen = false; aboutOpen -> aboutOpen = false; recoveryNotice -> recoveryNotice = false; runOpen -> runOpen = false; recycleOpen -> recycleOpen = false; controlPanelOpen -> controlPanelOpen = false; updateWindowOpen -> updateWindowOpen = false; settingsOpen -> settingsOpen = false; profileOpen -> profileOpen = false; computerOpen -> computerOpen = false; startOpen -> startOpen = false; else -> Unit } }

    val xpBlue = Color(0xFF245EDB)
    val taskbarHeight = 43.dp

    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
        WallpaperLayer(context, selectedBackground)

        BoxWithConstraints(Modifier.fillMaxSize().padding(start = 10.dp, top = 12.dp, end = 8.dp, bottom = taskbarHeight + 8.dp)) {
            val desktopApps = apps.filter { it.packageName in desktopPackages }
            val builtinShortcuts = listOf("builtin_my_computer", "builtin_my_documents", "builtin_internet", "builtin_recycle_bin").filterNot { it in hiddenBuiltinShortcuts }
            val slotHeight = 94f
            val slotWidth = 96f
            val rowsPerColumn = (maxHeight.value / slotHeight).toInt().coerceAtLeast(1)
            val desktopWidth = maxWidth
            val desktopHeight = maxHeight
            fun defaultX(index: Int) = (index / rowsPerColumn) * slotWidth
            fun defaultY(index: Int) = (index % rowsPerColumn) * slotHeight

            Box(Modifier.fillMaxSize()) {
                builtinShortcuts.forEachIndexed { index, id ->
                    MovableDesktopItem(prefs, id, defaultX(index), defaultY(index), desktopWidth, desktopHeight, dragEnabled = !desktopContextMenuOpen) {
                        when (id) {
                            "builtin_my_computer" -> DesktopSystemShortcut(context, prefs, customizationVersion, id, "computer", "🖥️", "My Computer", onClick = { computerOpen = true; startOpen = false }, onCustomize = { startOpen = false; settingsOpen = true }, onReset = { setIcon(id, null) }, onRemove = { setBuiltinShortcutVisible(id, false) }, onMenuStateChange = { desktopContextMenuOpen = it })
                            "builtin_my_documents" -> DesktopSystemShortcut(context, prefs, customizationVersion, id, "documents", "📁", "My Documents", onClick = { startOpen = false; openDocuments(context) }, onCustomize = { startOpen = false; settingsOpen = true }, onReset = { setIcon(id, null) }, onRemove = { setBuiltinShortcutVisible(id, false) }, onMenuStateChange = { desktopContextMenuOpen = it })
                            "builtin_internet" -> DesktopSystemShortcut(context, prefs, customizationVersion, id, "internet", "🌐", "Internet Explorer", onClick = { startOpen = false; openDefaultBrowser(context) }, onCustomize = { startOpen = false; settingsOpen = true }, onReset = { setIcon(id, null) }, onRemove = { setBuiltinShortcutVisible(id, false) }, onMenuStateChange = { desktopContextMenuOpen = it })
                            "builtin_recycle_bin" -> DesktopSystemShortcut(context, prefs, customizationVersion, id, "recycle", "🗑️", "Recycle Bin", onClick = { startOpen = false; recycleOpen = true }, onCustomize = { startOpen = false; settingsOpen = true }, onReset = { setIcon(id, null) }, onRemove = { setBuiltinShortcutVisible(id, false) }, onMenuStateChange = { desktopContextMenuOpen = it })
                        }
                    }
                }
                desktopApps.forEachIndexed { appIndex, app ->
                    val index = builtinShortcuts.size + appIndex
                    MovableDesktopItem(prefs, "app_${app.packageName}", defaultX(index), defaultY(index), desktopWidth, desktopHeight, dragEnabled = !desktopContextMenuOpen) {
                        DesktopAppIcon(context, prefs, customizationVersion, app, onClick = { openAndroidApp(app) }, onRemove = { setDesktopApp(app.packageName, false) }, onMenuStateChange = { desktopContextMenuOpen = it })
                    }
                }
            }
        }

        if (computerOpen) XPWindow("My Computer", Modifier.align(Alignment.Center), onClose = { computerOpen = false }) {
            Text("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Windroid XP ${BuildConfig.VERSION_NAME} • Android ${android.os.Build.VERSION.RELEASE}", fontSize = 10.sp, color = Color(0xFF555555))
            Spacer(Modifier.height(10.dp)); Text("Files Stored on This Computer", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(8.dp))
            XPSystemRow(context, "storage", "Internal Storage") { openDocuments(context) }; XPSystemRow(context, "documents", "My Documents") { openDocuments(context) }; XPSystemRow(context, "control", "Control Panel") { computerOpen = false; controlPanelOpen = true }; XPSystemRow(context, "settings", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }; XPSystemRow(context, "computer", "About Windroid XP") { computerOpen = false; aboutOpen = true }; Spacer(Modifier.height(10.dp)); XPActionButton("Close") { computerOpen = false }
        }

        if (controlPanelOpen) XpControlPanel(
            onClose = { controlPanelOpen = false },
            onAppearance = { controlPanelOpen = false; settingsOpen = true },
            onUserAccounts = { controlPanelOpen = false; profileOpen = true },
            onWindowsUpdate = { controlPanelOpen = false; checkForUpdates(true) },
            onAbout = { controlPanelOpen = false; aboutOpen = true },
            onRestoreDefaults = { resetConfirmOpen = true },
            modifier = Modifier.align(Alignment.Center)
        )

        if (recycleOpen) XPWindow("Recycle Bin", Modifier.align(Alignment.Center), onClose = { recycleOpen = false }) {
            Text("Removed desktop shortcuts", fontSize = 13.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); val removedApps = apps.filter { it.packageName in removedDesktopPackages }
            if (removedApps.isEmpty()) Text("The Recycle Bin is empty.", fontSize = 12.sp, color = Color(0xFF555555)) else Column(Modifier.heightIn(max = 310.dp).verticalScroll(rememberScrollState())) { removedApps.forEach { app -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { if (app.icon != null) Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(8.dp)); Text(app.label, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Restore", color = Color(0xFF003399), fontSize = 11.sp, modifier = Modifier.clickable { setDesktopApp(app.packageName, true) }.padding(5.dp)) } } }
        }

        if (runOpen) RunWindow(apps, context, onLaunch = { app -> runOpen = false; openAndroidApp(app) }, onClose = { runOpen = false }, modifier = Modifier.align(Alignment.Center))
        if (profileOpen) ProfileWindow(userName, userAvatar, onSave = { name, avatar -> userName = name.ifBlank { "User" }; userAvatar = avatar; prefs.edit().putString("user_name", userName).putString("user_avatar", userAvatar).apply(); profileOpen = false }, onCancel = { profileOpen = false }, modifier = Modifier.align(Alignment.Center))

        if (settingsOpen) AppearanceWindow(context, apps, backgrounds, iconFiles, selectedBackground, prefs, desktopPackages, hiddenBuiltinShortcuts, onBackgroundSelected = { fileName -> selectedBackground = fileName ?: DEFAULT_DESKTOP_BACKGROUND; prefs.edit().putString("desktop_background", selectedBackground).apply() }, onIconSelected = { id, fileName -> setIcon(id, fileName) }, onDesktopToggle = { pkg, enabled -> setDesktopApp(pkg, enabled) }, onBuiltinToggle = { id, visible -> setBuiltinShortcutVisible(id, visible) }, onClose = { settingsOpen = false }, modifier = Modifier.align(Alignment.Center))

        if (updateWindowOpen) {
    WindowsUpdateOverlay(
        context = context,
        onClose = { updateWindowOpen = false },
        modifier = Modifier.align(Alignment.Center)
    )
}

        taskbarContextApp?.let { app -> androidx.compose.ui.window.Popup(alignment = Alignment.BottomCenter, offset = IntOffset(0, -52), onDismissRequest = { taskbarContextApp = null }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) { Column(Modifier.width(205.dp).shadow(10.dp).background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(5.dp)) { Text(app.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp)); ContextMenuRow("Open") { taskbarContextApp = null; openAndroidApp(app) }; ContextMenuRow("App Info") { taskbarContextApp = null; openAppInfo(context, app.packageName) }; ContextMenuRow("Remove from Taskbar") { removeTaskbarApp(app) }; ContextMenuRow("Cancel") { taskbarContextApp = null } } } }

        if (!setupComplete) { Box(Modifier.fillMaxSize().background(Color(0x88000000))); FamilySetupWizard(context, userName, { userName = it }, { val safeName = userName.ifBlank { "User" }; userName = safeName; prefs.edit().putString("user_name", safeName).putBoolean("setup_complete", true).apply(); setupComplete = true }, Modifier.align(Alignment.Center)) }
        if (recoveryNotice) { Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { }); XPWindow("Windroid XP", Modifier.align(Alignment.Center).width(320.dp), onClose = { recoveryNotice = false }) { Text("Windroid XP recovered from an interrupted startup.", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(7.dp)); Text("Your Android apps and phone data were not changed. If something looks wrong, use Control Panel → Restore Windroid Defaults.", fontSize = 11.sp); Spacer(Modifier.height(10.dp)); XPActionButton("OK") { recoveryNotice = false } } }
        if (aboutOpen) { Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { }); XPWindow("About Windroid XP", Modifier.align(Alignment.Center).width(330.dp), onClose = { aboutOpen = false }) { Text("Windroid XP", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF003399)); Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 12.sp); Spacer(Modifier.height(9.dp)); Text("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", fontSize = 11.sp); Text("Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})", fontSize = 11.sp); Text("Installed apps detected: ${apps.size}", fontSize = 11.sp); Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { XPActionButton("Check for Updates") { aboutOpen = false; checkForUpdates(true) }; XPActionButton("Close") { aboutOpen = false } } } }
        if (resetConfirmOpen) { Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { }); XPWindow("Restore Windroid Defaults", Modifier.align(Alignment.Center).width(330.dp), onClose = { resetConfirmOpen = false }) { Text("Restore Windroid XP customization to defaults?", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(7.dp)); Text("This resets wallpaper, desktop icon positions and shortcuts, recent programs, username/avatar, and custom icon assignments. It does not uninstall apps or erase Android data.", fontSize = 11.sp); Spacer(Modifier.height(11.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { XPActionButton("Restore Defaults") { resetWindroid(); resetConfirmOpen = false }; XPActionButton("Cancel") { resetConfirmOpen = false } } } }

        if (startOpen) { Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { startOpen = false }); XpStartMenu(apps, launchedApps, context, prefs, customizationVersion, desktopPackages, userName, userAvatar, onEditProfile = { startOpen = false; profileOpen = true }, onLaunchApp = { openAndroidApp(it) }, onToggleDesktop = { app -> setDesktopApp(app.packageName, app.packageName !in desktopPackages) }, onOpenAppearance = { startOpen = false; settingsOpen = true }, onOpenComputer = { startOpen = false; computerOpen = true }, onOpenControlPanel = { startOpen = false; controlPanelOpen = true }, onOpenRecycle = { startOpen = false; recycleOpen = true }, onOpenRun = { startOpen = false; runOpen = true }, onCheckUpdates = { checkForUpdates(true) }, onOpenAbout = { startOpen = false; aboutOpen = true }, modifier = Modifier.align(Alignment.BottomStart).padding(bottom = taskbarHeight)) }

        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(taskbarHeight).background(Brush.verticalGradient(listOf(Color(0xFF3886E8), xpBlue, Color(0xFF1748AF)))), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.fillMaxHeight().width(108.dp).then(if (startButtonImage == null) Modifier.background(Brush.verticalGradient(listOf(Color(0xFF55B747), Color(0xFF33952E), Color(0xFF257E25))), RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp)) else Modifier).clickable { startOpen = !startOpen; computerOpen = false; profileOpen = false; settingsOpen = false }, contentAlignment = Alignment.CenterStart) { if (startButtonImage != null) Image(bitmap = startButtonImage, contentDescription = "Start", modifier = Modifier.fillMaxHeight().fillMaxWidth(), contentScale = ContentScale.Fit) else Text("⊞  start", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) }
            Spacer(Modifier.width(5.dp)); Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) { launchedApps.forEach { app -> val custom = remember(customizationVersion, app.packageName) { loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null)) }; TaskButton(custom ?: app.icon, "▣", app.label, onLongClick = { taskbarContextApp = app }) { openAndroidApp(app) } } }; XpNotificationTray(context)
        }
    }
}

@Composable
private fun MovableDesktopItem(prefs: android.content.SharedPreferences, positionId: String, defaultX: Float, defaultY: Float, maxWidth: androidx.compose.ui.unit.Dp, maxHeight: androidx.compose.ui.unit.Dp, dragEnabled: Boolean = true, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val maxXPx = with(density) { (maxWidth - 88.dp).coerceAtLeast(0.dp).toPx() }
    val maxYPx = with(density) { (maxHeight - 82.dp).coerceAtLeast(0.dp).toPx() }
    val defaultXPx = with(density) { defaultX.dp.toPx() }
    val defaultYPx = with(density) { defaultY.dp.toPx() }
    val keyX = desktopPositionKey(positionId, "x")
    val keyY = desktopPositionKey(positionId, "y")
    var position by remember(positionId, maxXPx, maxYPx) {
        mutableStateOf(Offset(prefs.getFloat(keyX, defaultXPx).coerceIn(0f, maxXPx), prefs.getFloat(keyY, defaultYPx).coerceIn(0f, maxYPx)))
    }
    var dragging by remember(positionId) { mutableStateOf(false) }
    fun save() { prefs.edit().putFloat(keyX, position.x).putFloat(keyY, position.y).apply() }

    Box(
        Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .zIndex(if (dragging) 100f else 0f)
            .pointerInput(positionId, maxXPx, maxYPx, dragEnabled) {
                if (!dragEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val pointerId = down.id
                    val startParent = position + down.position
                    var lastParent = startParent
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed || !dragEnabled) break
                        val pointerParent = position + change.position
                        val total = pointerParent - startParent
                        if (!moved && total.getDistance() > viewConfiguration.touchSlop) {
                            moved = true
                            dragging = true
                        }
                        if (moved) {
                            val delta = pointerParent - lastParent
                            position = Offset(
                                (position.x + delta.x).coerceIn(0f, maxXPx),
                                (position.y + delta.y).coerceIn(0f, maxYPx)
                            )
                            change.consume()
                        }
                        lastParent = pointerParent
                    }
                    if (moved) {
                        dragging = false
                        save()
                    }
                }
            }
    ) { content() }
}

@Composable private fun WallpaperLayer(context: Context, selectedBackground: String?) { val image = remember(selectedBackground) { loadAssetImage(context, "backgrounds", selectedBackground ?: DEFAULT_DESKTOP_BACKGROUND) }; if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else XPWallpaper() }
@Composable private fun XPWallpaper() { Canvas(Modifier.fillMaxSize()) { drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF2087D5), Color(0xFF55B6ED), Color(0xFFBDEAFF)), endY = size.height * 0.68f)); val farHill = Path().apply { moveTo(0f, size.height * 0.70f); cubicTo(size.width * 0.18f, size.height * 0.58f, size.width * 0.44f, size.height * 0.64f, size.width * 0.64f, size.height * 0.72f); cubicTo(size.width * 0.78f, size.height * 0.78f, size.width * 0.92f, size.height * 0.70f, size.width, size.height * 0.67f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }; drawPath(farHill, brush = Brush.verticalGradient(listOf(Color(0xFF79C752), Color(0xFF4DAA35)))); val nearHill = Path().apply { moveTo(0f, size.height * 0.82f); cubicTo(size.width * 0.20f, size.height * 0.69f, size.width * 0.40f, size.height * 0.70f, size.width * 0.58f, size.height * 0.82f); cubicTo(size.width * 0.75f, size.height * 0.93f, size.width * 0.90f, size.height * 0.85f, size.width, size.height * 0.80f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }; drawPath(nearHill, brush = Brush.verticalGradient(listOf(Color(0xFF56B33D), Color(0xFF248C2A)))) } }

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun DesktopSystemShortcut(context: Context, prefs: android.content.SharedPreferences, version: Int, id: String, registryKey: String, fallback: String, label: String, onClick: () -> Unit, onCustomize: () -> Unit, onReset: () -> Unit, onRemove: () -> Unit, onMenuStateChange: (Boolean) -> Unit = {}) {
    var menuOpen by remember { mutableStateOf(false) }; val customName = remember(id, version) { prefs.getString(iconPrefKey(id), null) }; val customIcon = remember(id, version, customName) { loadAssetImage(context, "icons", customName) }; val icon = customIcon ?: xpIcon(context, registryKey)
    fun closeMenu() { menuOpen = false; onMenuStateChange(false) }
    Box { Column(Modifier.width(88.dp).combinedClickable(onClick = onClick, onLongClick = { menuOpen = true; onMenuStateChange(true) }), horizontalAlignment = Alignment.CenterHorizontally) { if (icon != null) Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit) else Text(fallback, fontSize = 38.sp); Spacer(Modifier.height(3.dp)); Text(label, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }; if (menuOpen) androidx.compose.ui.window.Popup(alignment = Alignment.TopStart, offset = IntOffset(55, 28), onDismissRequest = { closeMenu() }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) { Column(Modifier.width(170.dp).background(Color(0xFFFFF8E7)).border(1.dp, Color(0xFF777777)).padding(4.dp)) { Text("Open", modifier = Modifier.fillMaxWidth().clickable { closeMenu(); onClick() }.padding(7.dp), fontSize = 12.sp); Text("Change Icon...", modifier = Modifier.fillMaxWidth().clickable { closeMenu(); onCustomize() }.padding(7.dp), fontSize = 12.sp); if (customName != null) Text("Reset Icon", modifier = Modifier.fillMaxWidth().clickable { closeMenu(); onReset() }.padding(7.dp), fontSize = 12.sp); Text("Remove from Desktop", modifier = Modifier.fillMaxWidth().clickable { closeMenu(); onRemove() }.padding(7.dp), fontSize = 12.sp); Text("Cancel", modifier = Modifier.fillMaxWidth().clickable { closeMenu() }.padding(7.dp), fontSize = 12.sp) } } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun DesktopAppIcon(context: Context, prefs: android.content.SharedPreferences, version: Int, app: LaunchableApp, onClick: () -> Unit, onRemove: () -> Unit, onMenuStateChange: (Boolean) -> Unit = {}) {
    val image = remember(version, app.packageName) { loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null)) }; var menuOpen by remember { mutableStateOf(false) }
    fun closeMenu() { menuOpen = false; onMenuStateChange(false) }
    Box(Modifier.width(88.dp)) { Column(Modifier.width(88.dp).combinedClickable(onClick = onClick, onLongClick = { menuOpen = true; onMenuStateChange(true) }), horizontalAlignment = Alignment.CenterHorizontally) { val icon = image ?: app.icon; if (icon != null) Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit) else Text("▣", fontSize = 38.sp); Spacer(Modifier.height(3.dp)); Text(app.label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp, maxLines = 2) }; if (menuOpen) androidx.compose.ui.window.Popup(alignment = Alignment.TopStart, offset = IntOffset(58, 28), onDismissRequest = { closeMenu() }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) { Column(Modifier.width(190.dp).shadow(10.dp).background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(5.dp)) { Text(app.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp)); ContextMenuRow("Open") { closeMenu(); onClick() }; ContextMenuRow("Remove from Desktop") { closeMenu(); onRemove() }; ContextMenuRow("App Info") { closeMenu(); openAppInfo(context, app.packageName) }; ContextMenuRow("Cancel") { closeMenu() } } } }
}

@Composable private fun AppearanceWindow(context: Context, apps: List<LaunchableApp>, backgrounds: List<String>, iconFiles: List<String>, selectedBackground: String?, prefs: android.content.SharedPreferences, desktopPackages: Set<String>, hiddenBuiltinShortcuts: Set<String>, onBackgroundSelected: (String?) -> Unit, onIconSelected: (String, String?) -> Unit, onDesktopToggle: (String, Boolean) -> Unit, onBuiltinToggle: (String, Boolean) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var page by remember { mutableStateOf("home") }; var iconTarget by remember { mutableStateOf<String?>(null) }; var iconTargetLabel by remember { mutableStateOf("") }; var iconSearch by remember { mutableStateOf("") }; var myIconsOpen by remember { mutableStateOf(false) }
    XPWindow("Display Properties", modifier.width(350.dp), onClose = onClose) { when {
        myIconsOpen && iconTarget != null -> {
            CustomIconPicker(
                context = context,
                onSelected = { customId ->
                    onIconSelected(iconTarget!!, customId)
                    iconTarget = null
                    iconSearch = ""
                    myIconsOpen = false
                },
                onBack = { myIconsOpen = false }
            )
        }
        iconTarget != null -> { Text("Choose icon for $iconTargetLabel", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); BasicTextField(value = iconSearch, onValueChange = { iconSearch = it.take(50) }, singleLine = true, modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(7.dp)); Text("Search the XP icon library by name. Showing up to 120 matches.", fontSize = 9.sp, color = Color(0xFF666666), modifier = Modifier.padding(vertical = 5.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { XPActionButton("My Icons...") { myIconsOpen = true }; XPActionButton("Use default") { onIconSelected(iconTarget!!, null); iconTarget = null; iconSearch = "" } }; Spacer(Modifier.height(7.dp)); val visibleIcons = remember(iconFiles, iconSearch) { val q = iconSearch.trim(); (if (q.isBlank()) iconFiles else iconFiles.filter { it.substringAfter("::", it).substringAfterLast("/").contains(q, ignoreCase = true) }).take(120) }; val groupedIcons = remember(visibleIcons) { visibleIcons.groupBy { iconCategory(it) }.toSortedMap() }; Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) { groupedIcons.forEach { (category, files) -> Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003399), modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp)); files.forEach { file -> val image = remember(file) { loadAssetImage(context, "icons", file) }; PickerRow(file.substringAfter("::", file).substringAfterLast("/"), image) { onIconSelected(iconTarget!!, file); iconTarget = null; iconSearch = "" } } } }; Spacer(Modifier.height(8.dp)); XPActionButton("Back") { iconTarget = null } }
        page == "myIcons" -> { CustomIconPicker(context = context, onBack = { page = "home" }) }
        page == "background" -> { Text("Desktop Background", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); Column(Modifier.height(330.dp).verticalScroll(rememberScrollState())) { val bliss = remember { loadAssetImage(context, "backgrounds", DEFAULT_DESKTOP_BACKGROUND) }; PickerRow("Default (Bliss)", bliss, selectedBackground == DEFAULT_DESKTOP_BACKGROUND) { onBackgroundSelected(DEFAULT_DESKTOP_BACKGROUND) }; backgrounds.filterNot { it == DEFAULT_DESKTOP_BACKGROUND }.forEach { file -> val image = remember(file) { loadAssetImage(context, "backgrounds", file) }; PickerRow(file, image, selectedBackground == file) { onBackgroundSelected(file) } } }; Spacer(Modifier.height(8.dp)); XPActionButton("Back") { page = "home" } }
        page == "desktopIcons" -> { Text("Desktop Icons", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); Column(Modifier.height(330.dp).verticalScroll(rememberScrollState())) { listOf("builtin_my_computer" to "My Computer", "builtin_my_documents" to "My Documents", "builtin_internet" to "Internet Explorer", "builtin_recycle_bin" to "Recycle Bin").forEach { (id, label) -> AssignmentRow(label, prefs.getString(iconPrefKey(id), null), id !in hiddenBuiltinShortcuts, { onBuiltinToggle(id, id in hiddenBuiltinShortcuts) }, { iconTarget = id; iconTargetLabel = label }) } }; Spacer(Modifier.height(8.dp)); XPActionButton("Back") { page = "home" } }
        page == "apps" -> { Text("Applications", fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("Assign custom icons or pin apps to the XP desktop.", fontSize = 10.sp, color = Color(0xFF666666)); Spacer(Modifier.height(8.dp)); Column(Modifier.height(350.dp).verticalScroll(rememberScrollState())) { apps.forEach { app -> AppCustomizationRow(app, prefs.getString(iconPrefKey("app_${app.packageName}"), null), { iconTarget = "app_${app.packageName}"; iconTargetLabel = app.label }, { onDesktopToggle(app.packageName, app.packageName !in desktopPackages) }, app.packageName in desktopPackages) } }; Spacer(Modifier.height(8.dp)); XPActionButton("Back") { page = "home" } }
        else -> { Text("Customize Windroid XP", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.height(12.dp)); SettingsChoice("🖼️", "Desktop Background", "Choose a wallpaper; Bliss is the install default") { page = "background" }; SettingsChoice("🖥️", "Desktop Icons", "Assign or hide Windroid shortcuts") { page = "desktopIcons" }; SettingsChoice("📦", "Applications", "Custom app icons and desktop shortcuts") { page = "apps" }; SettingsChoice("🗂️", "My Icons", "Import and manage your personal icon library") { page = "myIcons" }; Spacer(Modifier.height(14.dp)); Text("Desktop icons can be dragged anywhere and may overlap, just like old-school Windows.", fontSize = 10.sp, color = Color(0xFF666666), modifier = Modifier.padding(8.dp)); Spacer(Modifier.height(12.dp)); XPActionButton("Close") { onClose() } }
    } }
}

@Composable private fun SettingsChoice(icon: String, title: String, subtitle: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 25.sp); Spacer(Modifier.width(10.dp)); Column { Text(title, color = Color(0xFF003399), fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 10.sp, color = Color(0xFF555555)) } } }
@Composable private fun PickerRow(label: String, image: ImageBitmap?, selected: Boolean = false, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable { onClick() }.background(if (selected) Color(0xFFDCEBFA) else Color.Transparent).padding(horizontal = 6.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Color.White).border(1.dp, Color(0xFFB7B7B7)), contentAlignment = Alignment.Center) { if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) else Text("Default", fontSize = 8.sp) }; Spacer(Modifier.width(9.dp)); Text(label, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
@Composable private fun AssignmentRow(label: String, assignedFile: String?, visible: Boolean, onToggleVisibility: () -> Unit, onChange: () -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(if (visible) (assignedFile ?: "Default icon") else "Hidden from desktop", fontSize = 9.sp, color = Color(0xFF666666), maxLines = 1, overflow = TextOverflow.Ellipsis) }; Text(if (visible) "Remove" else "Restore", color = Color(0xFF003399), fontSize = 10.sp, modifier = Modifier.clickable { onToggleVisibility() }.padding(5.dp)); Spacer(Modifier.width(4.dp)); XPActionButton("Change") { onChange() } } }
@Composable private fun AppCustomizationRow(app: LaunchableApp, assignedFile: String?, onChangeIcon: () -> Unit, onToggleDesktop: () -> Unit, onDesktop: Boolean) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { if (app.icon != null) Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(28.dp)) else Text("▣", fontSize = 20.sp); Spacer(Modifier.width(7.dp)); Column(Modifier.weight(1f)) { Text(app.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(assignedFile ?: "Android icon", fontSize = 8.sp, color = Color(0xFF666666), maxLines = 1, overflow = TextOverflow.Ellipsis) }; Text("Icon", color = Color(0xFF003399), fontSize = 10.sp, modifier = Modifier.clickable { onChangeIcon() }.padding(5.dp)); Spacer(Modifier.width(4.dp)); Text(if (onDesktop) "Remove" else "Desktop", color = Color(0xFF003399), fontSize = 10.sp, modifier = Modifier.clickable { onToggleDesktop() }.padding(5.dp)) } }
@Composable private fun XPActionButton(label: String, onClick: () -> Unit) { Box(Modifier.background(Color(0xFFECE9D8), RoundedCornerShape(2.dp)).border(1.dp, Color(0xFF7F9DB9), RoundedCornerShape(2.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 6.dp)) { Text(label, fontSize = 12.sp, color = Color.Black) } }

@Composable private fun XPProgressBar(progress: Int) {
    val clamped = progress.coerceIn(0, 100)
    val segments = 20
    val filled = ((clamped / 100f) * segments).toInt()
    Row(Modifier.fillMaxWidth().height(18.dp).background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(segments) { index -> Box(Modifier.weight(1f).fillMaxHeight().background(if (index < filled) Color(0xFF00A000) else Color.Transparent)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun TaskButton(icon: ImageBitmap?, fallback: String, label: String, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) { Box(Modifier.padding(end = 3.dp).size(34.dp).background(Color(0xFF3579D2), RoundedCornerShape(2.dp)).border(1.dp, Color(0xFF6AA6F1), RoundedCornerShape(2.dp)).combinedClickable(onClick = onClick, onLongClick = { onLongClick?.invoke() }), contentAlignment = Alignment.Center) { if (icon != null) Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(23.dp), contentScale = ContentScale.Fit) else Text(fallback, color = Color.White, fontSize = 15.sp) } }

@Composable private fun XPSystemTray(context: Context) { var expanded by remember { mutableStateOf(false) }; Row(Modifier.fillMaxHeight().width(72.dp).padding(end = 2.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(17.dp).clickable { expanded = true }, contentAlignment = Alignment.Center) { Text("⌃", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(1.dp)); Clock() }; if (expanded) androidx.compose.ui.window.Popup(alignment = Alignment.BottomEnd, offset = IntOffset(0, -44), onDismissRequest = { expanded = false }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) { Column(Modifier.width(194.dp).shadow(10.dp).background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(5.dp)) { ContextMenuRow("Wi-Fi settings") { expanded = false; context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }; ContextMenuRow("Bluetooth settings") { expanded = false; context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }; ContextMenuRow("Flashlight / quick controls") { expanded = false; context.startActivity(Intent(Settings.ACTION_SETTINGS)) } } } }

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun StartMenu(apps: List<LaunchableApp>, recentApps: List<LaunchableApp>, context: Context, prefs: android.content.SharedPreferences, customizationVersion: Int, desktopPackages: Set<String>, userName: String, userAvatar: String, onEditProfile: () -> Unit, onLaunchApp: (LaunchableApp) -> Unit, onToggleDesktop: (LaunchableApp) -> Unit, onOpenAppearance: () -> Unit, onOpenComputer: () -> Unit, onOpenControlPanel: () -> Unit, onOpenRecycle: () -> Unit, onOpenRun: () -> Unit, onCheckUpdates: () -> Unit, onOpenAbout: () -> Unit, modifier: Modifier = Modifier) {
    val xpBlue = Color(0xFF1D62C8); var showAllPrograms by remember { mutableStateOf(false) }; var contextApp by remember { mutableStateOf<LaunchableApp?>(null) }; var showSearch by remember { mutableStateOf(false) }; var searchQuery by remember { mutableStateOf("") }; var powerOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = contextApp != null || powerOpen || showSearch || showAllPrograms) { when { contextApp != null -> contextApp = null; powerOpen -> powerOpen = false; showSearch -> { showSearch = false; searchQuery = "" }; else -> showAllPrograms = false } }
    Box(modifier.width(350.dp).heightIn(max = 590.dp)) { Column(Modifier.fillMaxSize().shadow(8.dp).border(2.dp, Color(0xFF174EA6)).background(Color.White)) { Row(Modifier.fillMaxWidth().height(68.dp).background(Brush.verticalGradient(listOf(Color(0xFF2F7BDC), Color(0xFF1855B6)))).clickable { onEditProfile() }.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).background(Color.White, RoundedCornerShape(4.dp)).border(1.dp, Color(0xFFB7CBE6), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text(userAvatar, fontSize = 28.sp) }; Spacer(Modifier.width(10.dp)); Column { Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Tap to change account", color = Color(0xFFDDEBFF), fontSize = 9.sp) } }; Row(Modifier.weight(1f)) { Column(Modifier.weight(1.35f).fillMaxHeight().background(Color.White)) { if (showAllPrograms) { Row(Modifier.fillMaxWidth().clickable { showAllPrograms = false }.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Text("◀", fontSize = 14.sp, color = Color(0xFF174EA6)); Spacer(Modifier.width(8.dp)); Text("Back", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF174EA6)) }; Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD6D6D6))); Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { apps.forEach { app -> StartMenuAppItem(context, prefs, customizationVersion, app, { onLaunchApp(app) }, { contextApp = app }) } } } else { Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { if (recentApps.isEmpty()) Text("Recently used programs will appear here.", color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) else recentApps.forEach { app -> StartMenuAppItem(context, prefs, customizationVersion, app, { onLaunchApp(app) }, { contextApp = app }) } }; Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD6D6D6))); Row(Modifier.fillMaxWidth().clickable { showAllPrograms = true }.padding(horizontal = 10.dp, vertical = 10.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { Text("All Programs", fontSize = 13.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); Text("▶", color = Color(0xFF248B23), fontSize = 14.sp, fontWeight = FontWeight.Bold) } } }; Column(Modifier.weight(0.95f).fillMaxHeight().background(Color(0xFFDCEBFA)).padding(vertical = 7.dp)) { RightMenuAssetItem(context, "documents", "My Documents") { openDocuments(context) }; RightMenuAssetItem(context, "computer", "My Computer") { onOpenComputer() }; RightMenuAssetItem(context, "recycle", "Recycle Bin") { onOpenRecycle() }; Spacer(Modifier.height(5.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFB4CCE7))); Spacer(Modifier.height(5.dp)); RightMenuAssetItem(context, "control", "Control Panel") { onOpenControlPanel() }; RightMenuAssetItem(context, "appearance", "Appearance") { onOpenAppearance() }; RightMenuAssetItem(context, "settings", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }; RightMenuAssetItem(context, "update", "Windows Update") { onCheckUpdates() }; RightMenuAssetItem(context, "search", "Search") { showSearch = true; showAllPrograms = false; contextApp = null }; RightMenuAssetItem(context, "run", "Run...") { onOpenRun() }; RightMenuAssetItem(context, "computer", "About Windroid XP") { onOpenAbout() } } }; Row(Modifier.fillMaxWidth().height(47.dp).background(xpBlue).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { Text("🔑 Log Off", color = Color.White, fontSize = 12.sp); Spacer(Modifier.width(16.dp)); Row(Modifier.clickable { powerOpen = true }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { val powerIcon = remember { xpIcon(context, "power") }; if (powerIcon != null) { Image(bitmap = powerIcon, contentDescription = "Turn Off Computer", modifier = Modifier.size(24.dp)); Spacer(Modifier.width(5.dp)) }; Text("Turn Off Computer", color = Color.White, fontSize = 12.sp) } } }
        if (powerOpen) Box(Modifier.align(Alignment.Center).width(300.dp).shadow(14.dp).background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(10.dp)) { Column { Text("Turn off computer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF003399)); Spacer(Modifier.height(8.dp)); Text("Choose what you want Windroid XP to do.", fontSize = 11.sp); Spacer(Modifier.height(8.dp)); ContextMenuRow("Return to Android / Change Home App") { powerOpen = false; try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }; ContextMenuRow("Android Settings") { powerOpen = false; context.startActivity(Intent(Settings.ACTION_SETTINGS)) }; ContextMenuRow("Restart Windroid XP") { powerOpen = false; context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(it) } }; ContextMenuRow("Cancel") { powerOpen = false } } }
        if (showSearch) Column(Modifier.align(Alignment.Center).width(290.dp).heightIn(max = 470.dp).shadow(12.dp).background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(10.dp)) { Text("Search Programs", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); BasicTextField(value = searchQuery, onValueChange = { searchQuery = it.take(60) }, singleLine = true, modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(horizontal = 8.dp, vertical = 7.dp)); Spacer(Modifier.height(8.dp)); val normalizedQuery = searchQuery.trim(); val results = if (normalizedQuery.isBlank()) apps else apps.filter { it.label.contains(normalizedQuery, true) || it.packageName.contains(normalizedQuery, true) }.sortedWith(compareBy<LaunchableApp> { when { it.label.equals(normalizedQuery, true) -> 0; it.label.startsWith(normalizedQuery, true) -> 1; else -> 2 } }.thenBy { it.label.lowercase() }); Column(Modifier.weight(1f, fill = false).heightIn(max = 330.dp).verticalScroll(rememberScrollState())) { if (results.isEmpty()) Text("No programs found.", color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.padding(10.dp)) else results.take(60).forEach { app -> StartMenuAppItem(context, prefs, customizationVersion, app, { showSearch = false; searchQuery = ""; onLaunchApp(app) }, { showSearch = false; contextApp = app }) } }; Spacer(Modifier.height(8.dp)); XPActionButton("Close") { showSearch = false; searchQuery = "" } }
        contextApp?.let { app -> Column(Modifier.align(Alignment.Center).width(235.dp).shadow(12.dp).background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(6.dp)) { Text(app.label, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(7.dp)); ContextMenuRow(if (app.packageName in desktopPackages) "Remove from Desktop" else "Add to Desktop") { onToggleDesktop(app); contextApp = null }; ContextMenuRow("App Info") { openAppInfo(context, app.packageName); contextApp = null }; ContextMenuRow("Cancel") { contextApp = null } } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun StartMenuAppItem(context: Context, prefs: android.content.SharedPreferences, version: Int, app: LaunchableApp, onClick: () -> Unit, onLongClick: () -> Unit) { val custom = remember(version, app.packageName) { loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null)) }; Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { val icon = custom ?: app.icon; if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(28.dp)) else Text("▣", fontSize = 22.sp); Spacer(Modifier.width(9.dp)); Text(app.label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun ContextMenuRow(label: String, onClick: () -> Unit) { Text(label, fontSize = 12.sp, color = Color.Black, modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 10.dp, vertical = 9.dp)) }
@Composable private fun XPSystemRow(context: Context, iconKey: String, label: String, onClick: () -> Unit) { val icon = remember(iconKey) { xpIcon(context, iconKey) }; Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(30.dp)) else Box(Modifier.size(30.dp)); Spacer(Modifier.width(9.dp)); Text(label, color = Color(0xFF003399), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }
@Composable private fun RightMenuAssetItem(context: Context, iconKey: String, label: String, onClick: () -> Unit) { val icon = remember(iconKey) { xpIcon(context, iconKey) }; Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(20.dp)) else Box(Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text(label, color = Color(0xFF163C73), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }

@Composable private fun RunWindow(apps: List<LaunchableApp>, context: Context, onLaunch: (LaunchableApp) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) { var query by remember { mutableStateOf("") }; val matches = remember(query, apps) { if (query.isBlank()) emptyList() else apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }.take(6) }; XPWindow("Run", modifier, onClose = onClose) { Text("Type the name of a program, package, or command.", fontSize = 11.sp); Spacer(Modifier.height(8.dp)); BasicTextField(value = query, onValueChange = { query = it.take(80) }, singleLine = true, modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(7.dp)); if (matches.isNotEmpty()) { Spacer(Modifier.height(7.dp)); matches.forEach { app -> Row(Modifier.fillMaxWidth().clickable { onLaunch(app) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { if (app.icon != null) Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(7.dp)); Text(app.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } } }; Spacer(Modifier.height(9.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { XPActionButton("OK") { when (query.trim().lowercase()) { "settings", "control" -> context.startActivity(Intent(Settings.ACTION_SETTINGS)); "files", "documents", "explorer" -> openDocuments(context); "browser", "internet", "iexplore" -> openDefaultBrowser(context); else -> matches.firstOrNull()?.let(onLaunch) } }; XPActionButton("Cancel") { onClose() } } } }
@Composable private fun ProfileWindow(currentName: String, currentAvatar: String, onSave: (String, String) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) { var draftName by remember(currentName) { mutableStateOf(currentName) }; var draftAvatar by remember(currentAvatar) { mutableStateOf(currentAvatar) }; val avatars = listOf("🙂", "😎", "🤖", "🐺", "🦊", "🐱", "👾", "🧑"); XPWindow("User Accounts", modifier, onClose = onCancel) { Text("Pick a name and picture for your account.", fontSize = 13.sp); Spacer(Modifier.height(12.dp)); Text("User name", fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); BasicTextField(value = draftName, onValueChange = { if (it.length <= 24) draftName = it }, singleLine = true, modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(7.dp)); Spacer(Modifier.height(12.dp)); Text("Account picture", fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) { avatars.forEach { avatar -> Box(Modifier.padding(end = 7.dp).size(42.dp).background(Color.White, RoundedCornerShape(4.dp)).border(if (avatar == draftAvatar) 2.dp else 1.dp, if (avatar == draftAvatar) Color(0xFF245EDB) else Color(0xFFB7B7B7), RoundedCornerShape(4.dp)).clickable { draftAvatar = avatar }, contentAlignment = Alignment.Center) { Text(avatar, fontSize = 25.sp) } } }; Spacer(Modifier.height(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { XPActionButton("Save") { onSave(draftName, draftAvatar) }; XPActionButton("Cancel") { onCancel() } } } }

@Composable private fun XPWindow(title: String, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val context = LocalContext.current
    var maximized by remember(title) { mutableStateOf(false) }
    val barImage = remember { loadAssetImage(context, "icons", "ui/Window Bar.jpg") }
    val closeImage = remember { loadAssetImage(context, "icons", "ui/Window X.jpg") }
    val minimizeImage = remember { loadAssetImage(context, "icons", "ui/Minimize.jpg") }
    val maximizeImage = remember { loadAssetImage(context, "icons", "ui/Maximize.jpg") }
    val frameModifier = if (maximized) modifier.fillMaxSize().padding(bottom = 43.dp) else modifier.width(316.dp)
    Column(frameModifier.shadow(10.dp).background(Color(0xFFECE9D8)).border(2.dp, Color(0xFF245EDB))) {
        Box(Modifier.fillMaxWidth().height(31.dp)) {
            if (barImage != null) Image(bitmap = barImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) else Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF0A56D8), Color(0xFF3A8AF1)))))
            Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                if (onClose != null) {
                    if (minimizeImage != null) Image(bitmap = minimizeImage, contentDescription = "Minimize", modifier = Modifier.size(22.dp).clickable { onClose() }, contentScale = ContentScale.Fit) else XPTitleFallback("_", onClose)
                    Spacer(Modifier.width(2.dp))
                    if (maximizeImage != null) Image(bitmap = maximizeImage, contentDescription = if (maximized) "Restore" else "Maximize", modifier = Modifier.size(22.dp).clickable { maximized = !maximized }, contentScale = ContentScale.Fit) else XPTitleFallback("□") { maximized = !maximized }
                    Spacer(Modifier.width(2.dp))
                    if (closeImage != null) Image(bitmap = closeImage, contentDescription = "Close", modifier = Modifier.size(22.dp).clickable { onClose() }, contentScale = ContentScale.Fit) else XPTitleFallback("×", onClose)
                }
            }
        }
        Column(Modifier.padding(18.dp).then(if (maximized) Modifier.fillMaxSize() else Modifier), content = content)
    }
}

@Composable private fun XPTitleFallback(label: String, onClick: () -> Unit) { Box(Modifier.size(20.dp).background(Color(0xFF3579D2), RoundedCornerShape(2.dp)).border(1.dp, Color.White, RoundedCornerShape(2.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) { Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) } }
@Composable private fun Clock() { var now by remember { mutableStateOf(Date()) }; LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); now = Date() } }; Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(now), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Normal, maxLines = 1) }
@Composable private fun FamilySetupWizard(context: Context, userName: String, onNameChanged: (String) -> Unit, onFinish: () -> Unit, modifier: Modifier = Modifier) { var page by remember { mutableIntStateOf(0) }; XPWindow("Welcome to Windroid XP", modifier.width(350.dp), onClose = null) { when (page) { 0 -> { Text("Welcome to Windroid XP", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF003399)); Spacer(Modifier.height(8.dp)); Text("This replaces your Home screen with a Windows XP-style launcher. Your Android apps, photos, messages, and settings stay on the phone.", fontSize = 11.sp); Spacer(Modifier.height(12.dp)); XPActionButton("Next") { page = 1 } }; 1 -> { Text("Choose your account name", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.height(8.dp)); BasicTextField(value = userName, onValueChange = { onNameChanged(it.take(24)) }, singleLine = true, modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(8.dp)); Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { XPActionButton("Back") { page = 0 }; XPActionButton("Next") { page = 2 } } }; else -> { Text("Make Windroid XP your Home app", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.height(8.dp)); Text("Android will show the Home-app chooser. Select Windroid XP if it is not already selected. You can always get back there from Start → Turn Off Computer.", fontSize = 11.sp); Spacer(Modifier.height(10.dp)); XPActionButton("Open Home App Settings") { try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }; Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { XPActionButton("Back") { page = 1 }; XPActionButton("Finish") { onFinish() } } } } } }

private fun iconCategory(file: String): String { val name = file.substringAfter("::", file).substringAfterLast("/").lowercase(); return when { file.startsWith("apps/") -> "App Icons"; file.startsWith("custom/") -> "Custom Icons"; listOf("folder", "document", "file", "explorer", "briefcase").any { it in name } -> "Windows XP • Files & Folders"; listOf("computer", "disk", "drive", "cd", "dvd", "usb", "printer", "camera", "scanner").any { it in name } -> "Windows XP • Hardware"; listOf("network", "internet", "connection", "mail", "messenger", "phone").any { it in name } -> "Windows XP • Internet & Communications"; listOf("control", "setting", "user", "update", "security", "help", "system").any { it in name } -> "Windows XP • System & Control Panel"; listOf("media", "music", "video", "picture", "photo", "paint").any { it in name } -> "Windows XP • Media"; else -> "Windows XP • Programs & Other" } }