from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

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

old_task = '''                    TaskButton(custom ?: app.icon, "▣", app.label) { startOpen = false; launchApp(context, app) }'''
new_task = '''                    TaskButton(custom ?: app.icon, "▣", app.label) { openAndroidApp(app) }'''
if old_task not in text:
    raise SystemExit("Taskbar launch block not found")
text = text.replace(old_task, new_task, 1)

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

# Pull the tray chevron right up against the clock. The old fixed-width clock box
# right-aligned its text, which created the visual hole between the chevron and time.
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
print("Applied runtime cleanup, Start-menu dismissal, and tight tray spacing")
