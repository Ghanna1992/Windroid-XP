from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

def must_replace(old, new, label):
    global text
    if old not in text:
        raise SystemExit(f"{label} not found; transformed source changed")
    text = text.replace(old, new, 1)

# Shared Windroid system-icon registry and Android equivalents for XP shell actions.
needle = 'private fun iconPrefKey(id: String) = "custom_icon_$id"\n'
insert = '''private fun iconPrefKey(id: String) = "custom_icon_$id"

private val XP_ICON_REGISTRY = mapOf(
    "computer" to "My Computer.png",
    "documents" to "My Documents.png",
    "recycle" to "Recycle Bin.png",
    "control" to "Control Panel.png",
    "appearance" to "Appearance.png",
    "programs" to "Change or Remove Programs.png",
    "network" to "Connection Status.png",
    "settings" to "Additional Settings.png",
    "back" to "Back.png",
    "storage" to "Hard Disk.png",
    "search" to "Search.png",
    "run" to "Run.png",
    "update" to "Automatic Updates.png",
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
'''
must_replace(needle, insert, "icon registry insertion")

must_replace(
'''    var updateWindowOpen by remember { mutableStateOf(false) }\n''',
'''    var updateWindowOpen by remember { mutableStateOf(false) }
    var controlPanelOpen by remember { mutableStateOf(false) }
    var recycleOpen by remember { mutableStateOf(false) }
    var runOpen by remember { mutableStateOf(false) }
''',
"shell window state"
)

must_replace(
'''    var desktopPackages by remember {
        mutableStateOf(prefs.getStringSet("desktop_apps", emptySet())?.toSet() ?: emptySet())
    }
''',
'''    var desktopPackages by remember {
        mutableStateOf(prefs.getStringSet("desktop_apps", emptySet())?.toSet() ?: emptySet())
    }
    var removedDesktopPackages by remember {
        mutableStateOf(prefs.getStringSet("removed_desktop_apps", emptySet())?.toSet() ?: emptySet())
    }
''',
"removed shortcuts state"
)

must_replace(
'''    fun setDesktopApp(packageName: String, enabled: Boolean) {
        desktopPackages = if (enabled) desktopPackages + packageName else desktopPackages - packageName
        prefs.edit().putStringSet("desktop_apps", desktopPackages).apply()
    }
''',
'''    fun setDesktopApp(packageName: String, enabled: Boolean) {
        desktopPackages = if (enabled) desktopPackages + packageName else desktopPackages - packageName
        removedDesktopPackages = if (enabled) removedDesktopPackages - packageName else removedDesktopPackages + packageName
        prefs.edit()
            .putStringSet("desktop_apps", desktopPackages)
            .putStringSet("removed_desktop_apps", removedDesktopPackages)
            .apply()
    }
''',
"desktop shortcut history"
)

must_replace(
'''            updateWindowOpen -> updateWindowOpen = false
            settingsOpen -> settingsOpen = false
''',
'''            runOpen -> runOpen = false
            recycleOpen -> recycleOpen = false
            controlPanelOpen -> controlPanelOpen = false
            updateWindowOpen -> updateWindowOpen = false
            settingsOpen -> settingsOpen = false
''',
"back handler shell windows"
)

# Make built-in system icons use the registry when no user override exists.
must_replace(
'''    val image = remember(version, id) { loadAssetImage(context, "icons", prefs.getString(iconPrefKey(id), null)) }
    DesktopIcon(image, fallback, label, onClick)
''',
'''    val image = remember(version, id) {
        loadAssetImage(context, "icons", prefs.getString(iconPrefKey(id), null)) ?: when (id) {
            "builtin_my_computer" -> xpIcon(context, "computer")
            "builtin_my_documents" -> xpIcon(context, "documents")
            "builtin_recycle_bin" -> xpIcon(context, "recycle")
            else -> null
        }
    }
    DesktopIcon(image, fallback, label, onClick)
''',
"default system desktop icons"
)

# Hook the built-in shortcuts into useful Android/Windroid equivalents.
text = text.replace(
'''DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_my_documents", "📁", "My Documents") { startOpen = false }''',
'''DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_my_documents", "📁", "My Documents") { startOpen = false; openDocuments(context) }'''
)
text = text.replace(
'''context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))''',
'''openDefaultBrowser(context)''',
1
)
text = text.replace(
'''DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_recycle_bin", "🗑️", "Recycle Bin") { startOpen = false }''',
'''DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_recycle_bin", "🗑️", "Recycle Bin") { startOpen = false; recycleOpen = true }'''
)

# Replace the bare My Computer body with an XP-style useful system launcher.
old_computer = '''                Text("Files Stored on This Computer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text("📱  Android Device", fontSize = 14.sp)
                Spacer(Modifier.height(5.dp))
                Text("📁  Internal Storage", fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                Text("⚙️  Android Settings", color = Color(0xFF003399), fontSize = 14.sp, modifier = Modifier.clickable {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                })
                Spacer(Modifier.height(18.dp))
                Text("Close", color = Color(0xFF003399), modifier = Modifier.clickable { computerOpen = false })
'''
new_computer = '''                Text("Files Stored on This Computer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                XPSystemRow(context, "storage", "Internal Storage") { openDocuments(context) }
                XPSystemRow(context, "documents", "My Documents") { openDocuments(context) }
                XPSystemRow(context, "control", "Control Panel") { computerOpen = false; controlPanelOpen = true }
                XPSystemRow(context, "settings", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                Spacer(Modifier.height(10.dp))
                XPActionButton("Close") { computerOpen = false }
'''
must_replace(old_computer, new_computer, "My Computer contents")

# Add Control Panel, Recycle Bin and Run windows before the profile window.
anchor = '''        if (profileOpen) {\n'''
windows = '''        if (controlPanelOpen) {
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

'''
must_replace(anchor, windows + anchor, "system windows insertion")

# Expand Start Menu callbacks.
must_replace(
'''                onOpenAppearance = { startOpen = false; settingsOpen = true },
                onCheckUpdates = { checkForUpdates(true) },
''',
'''                onOpenAppearance = { startOpen = false; settingsOpen = true },
                onOpenComputer = { startOpen = false; computerOpen = true },
                onOpenControlPanel = { startOpen = false; controlPanelOpen = true },
                onOpenRecycle = { startOpen = false; recycleOpen = true },
                onOpenRun = { startOpen = false; runOpen = true },
                onCheckUpdates = { checkForUpdates(true) },
''',
"StartMenu callback call"
)

must_replace(
'''    onOpenAppearance: () -> Unit,
    onCheckUpdates: () -> Unit,
''',
'''    onOpenAppearance: () -> Unit,
    onOpenComputer: () -> Unit,
    onOpenControlPanel: () -> Unit,
    onOpenRecycle: () -> Unit,
    onOpenRun: () -> Unit,
    onCheckUpdates: () -> Unit,
''',
"StartMenu callback signature"
)

# Replace right-side dead/emoji entries with actual XP-style system actions.
old_right = '''                    RightMenuItem("📄", "My Documents") { }
                    RightMenuItem("🖼️", "My Pictures") { }
                    RightMenuItem("🎵", "My Music") { }
                    Spacer(Modifier.height(5.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFB4CCE7))); Spacer(Modifier.height(5.dp))
                    RightMenuItem("🖥️", "My Computer") { }
                    RightMenuItem("🎨", "Appearance") { onOpenAppearance() }
                    RightMenuItem("⚙️", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    RightMenuItem("🛡️", "Windows Update") { onCheckUpdates() }
'''
new_right = '''                    RightMenuAssetItem(context, "documents", "My Documents") { openDocuments(context) }
                    RightMenuAssetItem(context, "computer", "My Computer") { onOpenComputer() }
                    RightMenuAssetItem(context, "recycle", "Recycle Bin") { onOpenRecycle() }
                    Spacer(Modifier.height(5.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFB4CCE7))); Spacer(Modifier.height(5.dp))
                    RightMenuAssetItem(context, "control", "Control Panel") { onOpenControlPanel() }
                    RightMenuAssetItem(context, "appearance", "Appearance") { onOpenAppearance() }
                    RightMenuAssetItem(context, "settings", "Android Settings") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    RightMenuAssetItem(context, "update", "Windows Update") { onCheckUpdates() }
'''
must_replace(old_right, new_right, "Start right system items")

# Add a useful Run item after Search (search patch may have changed the Search callback body but not label).
search_marker = 'RightMenuItem("🔍", "Search")'
if search_marker in text:
    line_start = text.index(search_marker)
    line_end = text.index('\n', line_start)
    search_line = text[line_start:line_end]
    text = text[:line_end+1] + '                    RightMenuAssetItem(context, "run", "Run...") { onOpenRun() }\n' + text[line_end+1:]
else:
    # Search patch renders this as an asset/alternate item on some builds; insert before Help.
    help_marker = '                    RightMenuItem("❓", "Help and Support") { }\n'
    if help_marker in text:
        text = text.replace(help_marker, '                    RightMenuAssetItem(context, "run", "Run...") { onOpenRun() }\n' + help_marker, 1)

# System rows and Run dialog helpers.
helper_anchor = '''@Composable
private fun ProfileWindow('''
helpers = '''@Composable
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

'''
must_replace(helper_anchor, helpers + helper_anchor, "system helper insertion")

path.write_text(text, encoding="utf-8")
print("Applied Windroid XP system shell expansion")
