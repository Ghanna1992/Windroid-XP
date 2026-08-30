from pathlib import Path

path = Path('app/src/main/java/com/windroid/xp/MainActivity.kt')
text = path.read_text(encoding='utf-8')

def rep(old, new, name):
    global text
    if old not in text:
        raise SystemExit(f'{name} anchor not found')
    text = text.replace(old, new, 1)

# Taskbar app context state.
rep('''    var resetConfirmOpen by remember { mutableStateOf(false) }
''', '''    var resetConfirmOpen by remember { mutableStateOf(false) }
    var taskbarContextApp by remember { mutableStateOf<LaunchableApp?>(null) }
''', 'taskbar context state')

rep('''    fun openAndroidApp(app: LaunchableApp) {''', '''    fun removeTaskbarApp(app: LaunchableApp) {
        launchedApps.removeAll { it.packageName == app.packageName }
        saveRecentApps()
        taskbarContextApp = null
    }

    fun openAndroidApp(app: LaunchableApp) {''', 'taskbar remove helper')

rep('''        when {
            resetConfirmOpen -> resetConfirmOpen = false''', '''        when {
            taskbarContextApp != null -> taskbarContextApp = null
            resetConfirmOpen -> resetConfirmOpen = false''', 'back taskbar context')

# Start menu About callback.
rep('''                onCheckUpdates = { checkForUpdates(true) },
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = taskbarHeight)''', '''                onCheckUpdates = { checkForUpdates(true) },
                onOpenAbout = { startOpen = false; aboutOpen = true },
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = taskbarHeight)''', 'start menu call about')

rep('''    onOpenRun: () -> Unit,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier''', '''    onOpenRun: () -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier''', 'start menu signature about')

rep('''                    RightMenuAssetItem(context, "computer", "About Windroid XP") {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") })
                    }''', '''                    RightMenuAssetItem(context, "computer", "About Windroid XP") { onOpenAbout() }''', 'start about action')

# Power dialog lives inside the Start menu and remains XP themed.
rep('''    var searchQuery by remember { mutableStateOf("") }
''', '''    var searchQuery by remember { mutableStateOf("") }
    var powerOpen by remember { mutableStateOf(false) }
''', 'power state')

rep('''    BackHandler(enabled = contextApp != null || showSearch || showAllPrograms) {
        when {
            contextApp != null -> contextApp = null
            showSearch -> { showSearch = false; searchQuery = "" }
            else -> showAllPrograms = false
        }
    }''', '''    BackHandler(enabled = contextApp != null || powerOpen || showSearch || showAllPrograms) {
        when {
            contextApp != null -> contextApp = null
            powerOpen -> powerOpen = false
            showSearch -> { showSearch = false; searchQuery = "" }
            else -> showAllPrograms = false
        }
    }''', 'start back power')

old_power = '''                Text(
                    "⏻ Turn Off Computer",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                        catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    }.padding(vertical = 8.dp)
                )'''
new_power = '''                Text(
                    "⏻ Turn Off Computer",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { powerOpen = true }.padding(vertical = 8.dp)
                )'''
rep(old_power, new_power, 'power button dialog')

power_dialog_anchor = '''        if (showSearch) {'''
power_dialog = '''        if (powerOpen) {
            Box(
                Modifier.align(Alignment.Center).width(300.dp).shadow(14.dp)
                    .background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(10.dp)
            ) {
                Column {
                    Text("Turn off computer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF003399))
                    Spacer(Modifier.height(8.dp))
                    Text("Choose what you want Windroid XP to do.", fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    ContextMenuRow("Return to Android / Change Home App") {
                        powerOpen = false
                        try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                        catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                    }
                    ContextMenuRow("Android Settings") {
                        powerOpen = false
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                    ContextMenuRow("Restart Windroid XP") {
                        powerOpen = false
                        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                    ContextMenuRow("Cancel") { powerOpen = false }
                }
            }
        }

'''
if power_dialog_anchor not in text: raise SystemExit('power dialog anchor not found')
text = text.replace(power_dialog_anchor, power_dialog + power_dialog_anchor, 1)

# Task buttons gain long-press support without changing existing normal clicks.
old_task = '''@Composable
private fun TaskButton(icon: ImageBitmap?, fallback: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(end = 3.dp).size(34.dp)
            .background(Color(0xFF3579D2), RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFF6AA6F1), RoundedCornerShape(2.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {'''
new_task = '''@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskButton(
    icon: ImageBitmap?,
    fallback: String,
    label: String,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        Modifier.padding(end = 3.dp).size(34.dp)
            .background(Color(0xFF3579D2), RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFF6AA6F1), RoundedCornerShape(2.dp))
            .combinedClickable(onClick = onClick, onLongClick = { onLongClick?.invoke() }),
        contentAlignment = Alignment.Center
    ) {'''
rep(old_task, new_task, 'task button long press')

rep('''                    TaskButton(custom ?: app.icon, "▣", app.label) { openAndroidApp(app) }''', '''                    TaskButton(
                        custom ?: app.icon,
                        "▣",
                        app.label,
                        onLongClick = { taskbarContextApp = app }
                    ) { openAndroidApp(app) }''', 'taskbar app call')

# Floating taskbar context menu.
start_anchor = '''        if (!setupComplete) {'''
taskbar_popup = '''        taskbarContextApp?.let { app ->
            androidx.compose.ui.window.Popup(
                alignment = Alignment.BottomCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, -52),
                onDismissRequest = { taskbarContextApp = null },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier.width(205.dp).shadow(10.dp).background(Color(0xFFF5F4EA))
                        .border(1.dp, Color(0xFF7F9DB9)).padding(5.dp)
                ) {
                    Text(app.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp))
                    ContextMenuRow("Open") { taskbarContextApp = null; openAndroidApp(app) }
                    ContextMenuRow("App Info") { taskbarContextApp = null; openAppInfo(context, app.packageName) }
                    ContextMenuRow("Remove from Taskbar") { removeTaskbarApp(app) }
                    ContextMenuRow("Cancel") { taskbarContextApp = null }
                }
            }
        }

'''
if start_anchor not in text: raise SystemExit('taskbar popup anchor not found')
text = text.replace(start_anchor, taskbar_popup + start_anchor, 1)

# Searchable/categorized icon picker prevents composing hundreds of decoded icons at once.
rep('''    var iconTargetLabel by remember { mutableStateOf("") }
''', '''    var iconTargetLabel by remember { mutableStateOf("") }
    var iconSearch by remember { mutableStateOf("") }
''', 'icon search state')

old_picker = '''                Spacer(Modifier.height(8.dp))
                Column(Modifier.height(330.dp).verticalScroll(rememberScrollState())) {
                    PickerRow("Use default", null) { onIconSelected(iconTarget!!, null); iconTarget = null }
                    iconFiles.forEach { file ->
                        val image = remember(file) { loadAssetImage(context, "icons", file) }
                        PickerRow(file.substringAfter("::", file).substringAfterLast("/"), image) { onIconSelected(iconTarget!!, file); iconTarget = null }
                    }
                    if (iconFiles.isEmpty()) Text("No custom icon images found yet.", fontSize = 11.sp, color = Color(0xFF666666), modifier = Modifier.padding(8.dp))
                }'''
new_picker = '''                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = iconSearch,
                    onValueChange = { iconSearch = it.take(50) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(7.dp)
                )
                Text("Search the XP icon library by name. Showing up to 120 matches.", fontSize = 9.sp, color = Color(0xFF666666), modifier = Modifier.padding(vertical = 5.dp))
                val visibleIcons = remember(iconFiles, iconSearch) {
                    val q = iconSearch.trim()
                    val filtered = if (q.isBlank()) iconFiles else iconFiles.filter {
                        it.substringAfter("::", it).substringAfterLast("/").contains(q, ignoreCase = true)
                    }
                    filtered.take(120)
                }
                val groupedIcons = remember(visibleIcons) { visibleIcons.groupBy { iconCategory(it) }.toSortedMap() }
                Column(Modifier.height(330.dp).verticalScroll(rememberScrollState())) {
                    PickerRow("Use default", null) { onIconSelected(iconTarget!!, null); iconTarget = null; iconSearch = "" }
                    groupedIcons.forEach { (category, files) ->
                        Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003399), modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp))
                        files.forEach { file ->
                            val image = remember(file) { loadAssetImage(context, "icons", file) }
                            PickerRow(file.substringAfter("::", file).substringAfterLast("/"), image) { onIconSelected(iconTarget!!, file); iconTarget = null; iconSearch = "" }
                        }
                    }
                    if (visibleIcons.isEmpty()) Text("No matching icons found.", fontSize = 11.sp, color = Color(0xFF666666), modifier = Modifier.padding(8.dp))
                }'''
rep(old_picker, new_picker, 'searchable icon picker')

# Add lightweight filename-based categories.
text += '''

private fun iconCategory(file: String): String {
    val name = file.substringAfter("::", file).substringAfterLast("/").lowercase()
    return when {
        listOf("folder", "document", "file", "explorer", "briefcase").any { it in name } -> "Files & Folders"
        listOf("computer", "disk", "drive", "cd", "dvd", "usb", "printer", "camera", "scanner").any { it in name } -> "Hardware"
        listOf("network", "internet", "connection", "mail", "messenger", "phone").any { it in name } -> "Internet & Communications"
        listOf("control", "setting", "user", "update", "security", "help", "system").any { it in name } -> "System & Control Panel"
        listOf("media", "music", "video", "picture", "photo", "paint").any { it in name } -> "Media"
        else -> "Programs & Other"
    }
}
'''

path.write_text(text, encoding='utf-8')
print('Applied family beta interaction polish')
