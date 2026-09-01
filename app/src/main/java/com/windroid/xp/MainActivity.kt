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

@Composable
fun WindroidDesktop(context: Context) {
    val prefs = remember { context.getSharedPreferences("windroid_prefs", Context.MODE_PRIVATE) }
    val apps = rememberInstalledApps(context)
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
