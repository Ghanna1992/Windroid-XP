from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

# Fix registry entries to match the actual bundled XP asset filenames.
text = text.replace('''    "recycle" to "Recycle Bin.png",''', '''    "recycle" to "Recycle Bin (empty).png",''', 1)
text = text.replace('''    "update" to "Automatic Updates.png",''', '''    "update" to "Windows Update.png",''', 1)

# Give common Android apps a conservative XP-era visual equivalent. These are only defaults:
# a user-selected custom icon still overrides them everywhere else in the launcher.
old_installed = '''        .map { info ->
            val icon = try { info.loadIcon(pm).toBitmap(96, 96).asImageBitmap() } catch (_: Exception) { null }
            LaunchableApp(
                label = info.loadLabel(pm).toString(),
                packageName = info.activityInfo.packageName,
                icon = icon
            )
        }'''
new_installed = '''        .map { info ->
            val label = info.loadLabel(pm).toString()
            val packageName = info.activityInfo.packageName
            val realIcon = try { info.loadIcon(pm).toBitmap(96, 96).asImageBitmap() } catch (_: Exception) { null }
            val icon = defaultXpAppIcon(context, packageName, label) ?: realIcon
            LaunchableApp(
                label = label,
                packageName = packageName,
                icon = icon
            )
        }'''
if old_installed not in text:
    raise SystemExit("installedApps mapping block not found")
text = text.replace(old_installed, new_installed, 1)

anchor = '''private fun launchApp(context: Context, app: LaunchableApp) {'''
helper = '''private fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {
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

'''
if anchor not in text:
    raise SystemExit("launchApp anchor not found")
text = text.replace(anchor, helper + anchor, 1)

old_browser = '''private fun openDefaultBrowser(context: Context) {
    try {
        openDefaultBrowser(context)
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}'''
new_browser = '''private fun openDefaultBrowser(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}'''
if old_browser not in text:
    raise SystemExit("openDefaultBrowser block not found")
text = text.replace(old_browser, new_browser, 1)

old_search = '''                val results = apps.filter {
                    searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true)
                }'''
new_search = '''                val normalizedQuery = searchQuery.trim()
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
                }'''
if old_search not in text:
    raise SystemExit("Start search block not found")
text = text.replace(old_search, new_search, 1)

# Keep taskbar launches consistent with Start/desktop launches so recents stay ordered.
old_task = '''                    TaskButton(custom ?: app.icon, "▣", app.label) { startOpen = false; launchApp(context, app) }'''
new_task = '''                    TaskButton(custom ?: app.icon, "▣", app.label) { openAndroidApp(app) }'''
if old_task not in text:
    raise SystemExit("Taskbar launch block not found")
text = text.replace(old_task, new_task, 1)

# Add an invisible click-catcher behind the Start menu so outside taps dismiss it.
old_start = '''        if (startOpen) {
            StartMenu(
'''
new_start = '''        if (startOpen) {
            Box(
                Modifier.fillMaxSize().clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { startOpen = false }
            )
            StartMenu(
'''
if old_start not in text:
    raise SystemExit("Start menu host block not found")
text = text.replace(old_start, new_start, 1)

# Windows Update is modal: consume every tap outside the window so desktop/taskbar items
# behind it cannot accidentally activate.
old_update = '''        if (updateWindowOpen) {
            XPWindow("Windows Update", Modifier.align(Alignment.Center), onClose = { updateWindowOpen = false }) {
                Text("🛡️  Automatic Updates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
'''
new_update = '''        if (updateWindowOpen) {
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
'''
if old_update not in text:
    raise SystemExit("Windows Update host block not found")
text = text.replace(old_update, new_update, 1)

# Use the bundled XP search asset instead of an emoji in Windroid-owned Start UI.
old_search_item = '''                    RightMenuItem("🔍", "Search") {
                        showSearch = true
                        showAllPrograms = false
                        contextApp = null
                    }'''
new_search_item = '''                    RightMenuAssetItem(context, "search", "Search") {
                        showSearch = true
                        showAllPrograms = false
                        contextApp = null
                    }'''
if old_search_item not in text:
    raise SystemExit("Start Search menu item not found")
text = text.replace(old_search_item, new_search_item, 1)

# Pull the tray chevron right up against the clock and remove the dead clock box gap.
old_tray = '''    Row(
        Modifier.fillMaxHeight().width(82.dp).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(18.dp).clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Text("⌃", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(2.dp))
        Clock()
    }'''
new_tray = '''    Row(
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
    }'''
if old_tray not in text:
    raise SystemExit("System tray layout block not found")
text = text.replace(old_tray, new_tray, 1)

old_clock = '''    Box(Modifier.width(58.dp), contentAlignment = Alignment.CenterEnd) {
        Text(
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(now),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }'''
new_clock = '''    Text(
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(now),
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 1
    )'''
if old_clock not in text:
    raise SystemExit("Clock layout block not found")
text = text.replace(old_clock, new_clock, 1)

path.write_text(text, encoding="utf-8")
print("Applied runtime cleanup, corrected XP system icons, common Android app XP mapping, modal update blocking, Start dismissal, and tight tray spacing")
