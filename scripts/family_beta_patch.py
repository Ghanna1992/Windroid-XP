from pathlib import Path

path = Path('app/src/main/java/com/windroid/xp/MainActivity.kt')
text = path.read_text(encoding='utf-8')

def rep(old, new, name):
    global text
    if old not in text:
        raise SystemExit(f'{name} anchor not found')
    text = text.replace(old, new, 1)

# Cache decoded asset images to keep the 400+ icon library from repeatedly decoding.
old_loader = '''private fun loadAssetImage(context: Context, folder: String, fileName: String?): ImageBitmap? {
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
'''
new_loader = '''private val assetImageCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap?>()

private fun loadAssetImage(context: Context, folder: String, fileName: String?): ImageBitmap? {
    if (fileName.isNullOrBlank()) return null
    val key = "$folder::$fileName"
    if (assetImageCache.containsKey(key)) return assetImageCache[key]
    val image = try {
        if (fileName.contains("::")) {
            val zipName = fileName.substringBefore("::")
            val entryName = fileName.substringAfter("::")
            var found: ImageBitmap? = null
            context.assets.open("$folder/$zipName").use { raw ->
                java.util.zip.ZipInputStream(raw).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name == entryName) {
                            val bytes = java.io.ByteArrayOutputStream()
                            zip.copyTo(bytes)
                            found = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())?.asImageBitmap()
                            break
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            found
        } else {
            context.assets.open("$folder/$fileName").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        }
    } catch (_: Exception) { null }
    assetImageCache[key] = image
    return image
}
'''
rep(old_loader, new_loader, 'asset loader')

# Hide UI-only artwork from normal icon assignment.
rep('''        listAssetImages(context, "icons").filterNot { it == "start_button.png" || it == "close_button.png" }''', '''        listAssetImages(context, "icons").filterNot {
            val n = it.substringAfter("::", it).substringAfterLast("/").lowercase()
            n == "start_button.png" || n == "close_button.png" || n.contains("button") || n.contains("cursor")
        }''', 'icon filter')

# Family-beta window/setup state.
rep('''    var runOpen by remember { mutableStateOf(false) }
''', '''    var runOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    var resetConfirmOpen by remember { mutableStateOf(false) }
    var setupComplete by remember { mutableStateOf(prefs.getBoolean("setup_complete", false)) }
    val previousStartupComplete = remember { prefs.getBoolean("startup_completed", true) }
    var recoveryNotice by remember { mutableStateOf(!previousStartupComplete && setupComplete) }
''', 'window state')

# Recovery marker: if a launch dies before this delay finishes, next launch shows a notice.
rep('''    val updateScope = rememberCoroutineScope()
''', '''    val updateScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        prefs.edit().putBoolean("startup_completed", false).apply()
        kotlinx.coroutines.delay(1800)
        prefs.edit().putBoolean("startup_completed", true).apply()
    }
''', 'startup marker')

# Better updater result handling.
old_check = '''        updateScope.launch {
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
        }'''
new_check = '''        updateScope.launch {
            downloadedUpdate = null
            when (val result = UpdateManager.checkForUpdate()) {
                is UpdateManager.CheckResult.UpdateAvailable -> {
                    updateInfo = result.update
                    updateStatus = "Windroid XP ${result.update.versionName} is ready."
                    updateWindowOpen = true
                }
                UpdateManager.CheckResult.UpToDate -> {
                    updateInfo = null
                    updateStatus = "Your computer is up to date."
                    updateWindowOpen = manual
                }
                is UpdateManager.CheckResult.Failed -> {
                    updateInfo = null
                    updateStatus = result.message
                    updateWindowOpen = manual
                }
            }
        }'''
rep(old_check, new_check, 'manual update check')

old_auto = '''    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        val found = UpdateManager.checkForUpdate()
        if (found != null) {
            updateInfo = found
            updateStatus = "Windroid XP ${found.versionName} is ready."
            updateWindowOpen = true
        }
    }'''
new_auto = '''    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        when (val result = UpdateManager.checkForUpdate()) {
            is UpdateManager.CheckResult.UpdateAvailable -> {
                updateInfo = result.update
                updateStatus = "Windroid XP ${result.update.versionName} is ready."
                updateWindowOpen = true
            }
            else -> Unit
        }
    }'''
rep(old_auto, new_auto, 'automatic update check')

# Reset helper.
rep('''    fun setDesktopApp(packageName: String, enabled: Boolean) {''', '''    fun resetWindroid() {
        prefs.edit()
            .remove("desktop_background")
            .remove("desktop_apps")
            .remove("removed_desktop_apps")
            .remove("recent_apps")
            .remove("user_name")
            .remove("user_avatar")
            .apply()
        prefs.all.keys.filter { it.startsWith("custom_icon_") }.forEach { prefs.edit().remove(it).apply() }
        selectedBackground = null
        desktopPackages = emptySet()
        removedDesktopPackages = emptySet()
        launchedApps.clear()
        userName = "User"
        userAvatar = "🙂"
        customizationVersion++
    }

    fun setDesktopApp(packageName: String, enabled: Boolean) {''', 'reset helper')

# Back closes family-beta modals first.
rep('''        when {
            runOpen -> runOpen = false''', '''        when {
            resetConfirmOpen -> resetConfirmOpen = false
            aboutOpen -> aboutOpen = false
            recoveryNotice -> recoveryNotice = false
            runOpen -> runOpen = false''', 'back handler')

# Make My Computer show useful device/version info and About.
rep('''                Text("Files Stored on This Computer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))''', '''                Text("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Windroid XP ${BuildConfig.VERSION_NAME} • Android ${android.os.Build.VERSION.RELEASE}", fontSize = 10.sp, color = Color(0xFF555555))
                Spacer(Modifier.height(10.dp))
                Text("Files Stored on This Computer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))''', 'my computer info')
rep('''                XPSystemRow(context, "settings", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                Spacer(Modifier.height(10.dp))''', '''                XPSystemRow(context, "settings", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                XPSystemRow(context, "computer", "About Windroid XP") { computerOpen = false; aboutOpen = true }
                Spacer(Modifier.height(10.dp))''', 'my computer about')

# Control Panel beta tools.
rep('''                XPSystemRow(context, "settings", "Android System Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
''', '''                XPSystemRow(context, "settings", "Android System Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                XPSystemRow(context, "computer", "About Windroid XP") { controlPanelOpen = false; aboutOpen = true }
                XPSystemRow(context, "back", "Restore Windroid Defaults") { resetConfirmOpen = true }
''', 'control panel beta tools')

# Add family-beta modal windows before Start menu.
anchor = '''        if (startOpen) {'''
modal = '''        if (!setupComplete) {
            Box(Modifier.fillMaxSize().background(Color(0x88000000)))
            FamilySetupWizard(
                context = context,
                userName = userName,
                onNameChanged = { userName = it },
                onFinish = {
                    val safeName = userName.ifBlank { "User" }
                    userName = safeName
                    prefs.edit().putString("user_name", safeName).putBoolean("setup_complete", true).apply()
                    setupComplete = true
                },
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (recoveryNotice) {
            Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { })
            XPWindow("Windroid XP", Modifier.align(Alignment.Center).width(320.dp), onClose = { recoveryNotice = false }) {
                Text("Windroid XP recovered from an interrupted startup.", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(7.dp))
                Text("Your Android apps and phone data were not changed. If something looks wrong, use Control Panel → Restore Windroid Defaults.", fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                XPActionButton("OK") { recoveryNotice = false }
            }
        }

        if (aboutOpen) {
            Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { })
            XPWindow("About Windroid XP", Modifier.align(Alignment.Center).width(330.dp), onClose = { aboutOpen = false }) {
                Text("Windroid XP", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF003399))
                Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 12.sp)
                Spacer(Modifier.height(9.dp))
                Text("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", fontSize = 11.sp)
                Text("Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})", fontSize = 11.sp)
                Text("Installed apps detected: ${apps.size}", fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    XPActionButton("Check for Updates") { aboutOpen = false; checkForUpdates(true) }
                    XPActionButton("Close") { aboutOpen = false }
                }
            }
        }

        if (resetConfirmOpen) {
            Box(Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { })
            XPWindow("Restore Windroid Defaults", Modifier.align(Alignment.Center).width(330.dp), onClose = { resetConfirmOpen = false }) {
                Text("Restore Windroid XP customization to defaults?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(7.dp))
                Text("This resets wallpaper, desktop app shortcuts, recent programs, username/avatar, and custom icon assignments. It does not uninstall apps or erase Android data.", fontSize = 11.sp)
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    XPActionButton("Restore Defaults") { resetWindroid(); resetConfirmOpen = false }
                    XPActionButton("Cancel") { resetConfirmOpen = false }
                }
            }
        }

'''
if anchor not in text: raise SystemExit('start anchor not found')
text = text.replace(anchor, modal + anchor, 1)

# Start-menu Turn Off Computer becomes the visible escape hatch.
rep('''                Text("⏻ Turn Off Computer", color = Color.White, fontSize = 12.sp)''', '''                Text(
                    "⏻ Turn Off Computer",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                        catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    }.padding(vertical = 8.dp)
                )''', 'power button')

# Replace remaining Help emoji with a useful About entry.
rep('''                    RightMenuItem("❓", "Help and Support") { }''', '''                    RightMenuAssetItem(context, "computer", "About Windroid XP") {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") })
                    }''', 'help entry')

# Add setup wizard composable.
text += '''

@Composable
private fun FamilySetupWizard(
    context: Context,
    userName: String,
    onNameChanged: (String) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by remember { mutableIntStateOf(0) }
    XPWindow("Welcome to Windroid XP", modifier.width(350.dp), onClose = null) {
        when (page) {
            0 -> {
                Text("Welcome to Windroid XP", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF003399))
                Spacer(Modifier.height(8.dp))
                Text("This replaces your Home screen with a Windows XP-style launcher. Your Android apps, photos, messages, and settings stay on the phone.", fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                XPActionButton("Next") { page = 1 }
            }
            1 -> {
                Text("Choose your account name", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = userName,
                    onValueChange = { onNameChanged(it.take(24)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(8.dp)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    XPActionButton("Back") { page = 0 }
                    XPActionButton("Next") { page = 2 }
                }
            }
            else -> {
                Text("Make Windroid XP your Home app", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("Android will show the Home-app chooser. Select Windroid XP if it is not already selected. You can always get back there from Start → Turn Off Computer.", fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                XPActionButton("Open Home App Settings") {
                    try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                    catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    XPActionButton("Back") { page = 1 }
                    XPActionButton("Finish") { onFinish() }
                }
            }
        }
    }
}
'''

path.write_text(text, encoding='utf-8')
print('Applied family beta hardening pass')
