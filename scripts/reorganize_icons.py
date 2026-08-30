from pathlib import Path
import shutil

icons = Path('app/src/main/assets/icons')
apps = icons / 'apps'
xp = icons / 'xp'
ui = icons / 'ui'
for d in (apps, xp, ui):
    d.mkdir(parents=True, exist_ok=True)

# App-specific icons we've started creating. Future app icons belong in icons/apps/.
app_icons = {
    '1Kosmos.png',
    'Baby Plus.png',
    'Home.png',
    'ChatGPT.png',
    'Discord.png',
}
ui_icons = {'start_button.png', 'close_button.png'}

for item in list(icons.iterdir()):
    if not item.is_file():
        continue
    if item.name in app_icons:
        dest = apps / item.name
    elif item.name in ui_icons:
        dest = ui / item.name
    else:
        dest = xp / item.name
    if dest.exists():
        item.unlink()
    else:
        shutil.move(str(item), str(dest))

p = Path('app/src/main/java/com/windroid/xp/MainActivity.kt')
s = p.read_text()

start = s.index('private fun defaultXpAppIcon(')
end = s.index('\nprivate fun launchApp(', start)
default_icon = r'''private fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {
    val pkg = packageName.lowercase()
    val name = label.lowercase()

    // App replacements live in icons/apps and are named after the Android app.
    // Normalize case/spaces/punctuation so adding a new icon usually needs zero Kotlin changes.
    val normalizedLabel = name.replace(Regex("[^a-z0-9]"), "")
    val appNamedIcon = listAssetImages(context, "icons/apps").firstOrNull { file ->
        val base = file.substringAfter("::", file).substringAfterLast('/').substringBeforeLast('.').lowercase()
        val normalizedFile = base.replace(Regex("[^a-z0-9]"), "")
        normalizedFile == normalizedLabel ||
            (name == "google home" && normalizedFile == "home") ||
            (name.contains("baby plus") && normalizedFile == "babyplus") ||
            (name.contains("1kosmos") && normalizedFile == "1kosmos")
    }
    if (appNamedIcon != null) {
        loadAssetImage(context, "icons/apps", appNamedIcon)?.let { return it }
    }

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
}'''
s = s[:start] + default_icon + s[end:]

start = s.index('private fun listAssetImages(')
end = s.index('\nprivate val assetImageCache', start)
asset_funcs = r'''private fun listAssetImages(context: Context, folder: String): List<String> {
    val allowed = setOf("png", "jpg", "jpeg", "webp")
    val results = mutableListOf<String>()

    fun scan(assetFolder: String, relativePrefix: String = "") {
        val children = try { context.assets.list(assetFolder)?.toList().orEmpty() } catch (_: Exception) { emptyList() }
        for (name in children) {
            val full = "$assetFolder/$name"
            val relative = if (relativePrefix.isBlank()) name else "$relativePrefix/$name"
            val ext = name.substringAfterLast('.', "").lowercase()
            when {
                ext in allowed -> results.add(relative)
                ext == "zip" -> {
                    try {
                        context.assets.open(full).use { raw ->
                            java.util.zip.ZipInputStream(raw).use { zip ->
                                var entry = zip.nextEntry
                                while (entry != null) {
                                    if (!entry.isDirectory && entry.name.substringAfterLast('.', "").lowercase() in allowed) {
                                        results.add("$relative::${entry.name}")
                                    }
                                    zip.closeEntry()
                                    entry = zip.nextEntry
                                }
                            }
                        }
                    } catch (_: Exception) { }
                }
                ext.isBlank() -> scan(full, relative)
            }
        }
    }

    return try {
        scan(folder)
        results.sortedBy { it.substringAfter("::", it).lowercase() }
    } catch (_: Exception) {
        emptyList()
    }
}'''
s = s[:start] + asset_funcs + s[end:]

start = s.index('private fun loadAssetImage(')
end = s.index('\nprivate fun resolveIntentIcon(', start)
loader = r'''private fun loadAssetImage(context: Context, folder: String, fileName: String?): ImageBitmap? {
    if (fileName.isNullOrBlank()) return null
    val key = "$folder::$fileName"
    if (assetImageCache.containsKey(key)) return assetImageCache[key]

    fun candidatePaths(name: String): List<String> {
        if (folder != "icons" || name.contains('/')) return listOf("$folder/$name")
        // Compatibility for preferences and registry entries saved before the folder migration.
        return listOf("icons/$name", "icons/xp/$name", "icons/apps/$name", "icons/ui/$name")
    }

    val image = try {
        if (fileName.contains("::")) {
            val zipName = fileName.substringBefore("::")
            val entryName = fileName.substringAfter("::")
            var found: ImageBitmap? = null
            for (path in candidatePaths(zipName)) {
                try {
                    context.assets.open(path).use { raw ->
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
                } catch (_: Exception) { }
                if (found != null) break
            }
            found
        } else {
            var found: ImageBitmap? = null
            for (path in candidatePaths(fileName)) {
                try {
                    found = context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
                } catch (_: Exception) { }
                if (found != null) break
            }
            found
        }
    } catch (_: Exception) { null }
    if (image != null) assetImageCache[key] = image
    return image
}'''
s = s[:start] + loader + s[end:]

s = s.replace('context.assets.open("icons/start_button.png")', 'context.assets.open("icons/ui/start_button.png")')
s = s.replace('loadAssetImage(context, "icons", "start_button.png")', 'loadAssetImage(context, "icons", "ui/start_button.png")')

# Keep UI-only art out of the manual icon picker after the folder migration.
s = s.replace(
    'n == "start_button.png" || n == "close_button.png" || n.contains("button") || n.contains("cursor")',
    'it.startsWith("ui/") || n == "start_button.png" || n == "close_button.png" || n.contains("button") || n.contains("cursor")'
)

p.write_text(s)

# Retire the earlier one-purpose patch helper; this migration includes that behavior.
old_patch = Path('scripts/auto_app_icons_patch.py')
if old_patch.exists(): old_patch.unlink()

# One-time helper: remove itself after applying so later builds are clean/no-op.
Path(__file__).unlink()
