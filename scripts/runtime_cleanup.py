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

# Keep taskbar app clicks consistent with Start/desktop launches so recent ordering persists.
old_task = '''                    TaskButton(custom ?: app.icon, "▣", app.label) { startOpen = false; launchApp(context, app) }'''
new_task = '''                    TaskButton(custom ?: app.icon, "▣", app.label) { openAndroidApp(app) }'''
if old_task not in text:
    raise SystemExit("Taskbar launch block not found")
text = text.replace(old_task, new_task, 1)

path.write_text(text, encoding="utf-8")
print("Applied runtime cleanup")
