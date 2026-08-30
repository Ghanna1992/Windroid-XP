from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

# Upgrade icon loading so assets/icons can contain normal image files OR zip packs.
# ZIP entries are represented internally as "pack.zip::path/in/pack.png".
start = text.index("private fun listAssetImages(context: Context, folder: String): List<String> {")
end = text.index("\nprivate fun loadStartButtonImage(context: Context): ImageBitmap?", start)
new_asset_helpers = r'''private fun listAssetImages(context: Context, folder: String): List<String> {
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
'''
text = text[:start] + new_asset_helpers + text[end:]

# Cache icons for the phone's actual default browser and document/file handler.
needle = '    val startButtonImage = remember { loadStartButtonImage(context) }\n'
replacement = '''    val startButtonImage = remember { loadStartButtonImage(context) }
    val defaultBrowserIcon = remember {
        resolveIntentIcon(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
    }
    val defaultFileIcon = remember {
        resolveIntentIcon(
            context,
            Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
        )
    }
'''
if needle not in text:
    raise SystemExit("Start image declaration not found; source changed")
text = text.replace(needle, replacement, 1)

# In the already-patched desktop grid, My Documents uses the default document/file
# handler and Internet Explorer uses the default web browser. Custom Windroid icon
# assignments still win if the user explicitly selected one.
old_documents = '''                                    1 -> DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_my_documents", "📁", "My Documents") { startOpen = false }
                                    2 -> DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_internet", "🌐", "Internet Explorer") {
                                        startOpen = false
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
                                    }
'''
new_documents = '''                                    1 -> DesktopResolvedBuiltInIcon(
                                        context, prefs, customizationVersion,
                                        "builtin_my_documents", "📁", "My Documents", defaultFileIcon
                                    ) {
                                        startOpen = false
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_OPEN_DOCUMENT)
                                                    .addCategory(Intent.CATEGORY_OPENABLE)
                                                    .setType("*/*")
                                            )
                                        } catch (_: Exception) {
                                            context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
                                        }
                                    }
                                    2 -> DesktopResolvedBuiltInIcon(
                                        context, prefs, customizationVersion,
                                        "builtin_internet", "🌐", "Internet Explorer", defaultBrowserIcon
                                    ) {
                                        startOpen = false
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
                                    }
'''
if old_documents not in text:
    raise SystemExit("Patched desktop Documents/Internet block not found")
text = text.replace(old_documents, new_documents, 1)

# Add a built-in shortcut renderer that can inherit the resolved Android app icon
# while preserving Windroid's per-shortcut custom icon override.
insert_at = text.index("\n@Composable\nprivate fun DesktopBuiltInIcon(")
resolved_composable = r'''
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
'''
text = text[:insert_at] + resolved_composable + text[insert_at:]

# Make ZIP-backed entries display a clean filename in the icon picker rather than
# exposing the internal pack.zip::folder/file.png reference.
text = text.replace(
    'PickerRow(file, image) { onIconSelected(iconTarget!!, file); iconTarget = null }',
    'PickerRow(file.substringAfter("::", file).substringAfterLast("/"), image) { onIconSelected(iconTarget!!, file); iconTarget = null }',
    1
)

path.write_text(text, encoding="utf-8")
print("Patched ZIP icon packs and default browser/file shortcuts")
