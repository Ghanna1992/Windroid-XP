from pathlib import Path
import shutil

repo = Path('.')
icons = repo / 'app/src/main/assets/icons'
apps_dir = icons / 'apps'
xp_dir = icons / 'xp'
ui_dir = icons / 'ui'
for d in (apps_dir, xp_dir, ui_dir):
    d.mkdir(parents=True, exist_ok=True)

# App-specific artwork: these are matched to installed Android apps by app label.
app_moves = {
    repo / 'chatgpt.png': apps_dir / 'ChatGPT.png',
    repo / 'discord.png': apps_dir / 'Discord.png',
    icons / '1Kosmos.png': apps_dir / '1Kosmos.png',
    icons / 'Baby Plus.png': apps_dir / 'Baby Plus.png',
    icons / 'Home.png': apps_dir / 'Home.png',
}
for src, dst in app_moves.items():
    if src.exists():
        if dst.exists():
            src.unlink()
        else:
            shutil.move(str(src), str(dst))

# Windroid UI artwork should never appear as an assignable app icon.
for name in ('start_button.png', 'close_button.png'):
    src = icons / name
    dst = ui_dir / name
    if src.exists():
        if dst.exists():
            src.unlink()
        else:
            shutil.move(str(src), str(dst))

# Everything else currently at the icon root is the legacy Windows XP library.
for child in list(icons.iterdir()):
    if child.is_file():
        dst = xp_dir / child.name
        if dst.exists():
            child.unlink()
        else:
            shutil.move(str(child), str(dst))

main = repo / 'app/src/main/java/com/windroid/xp/MainActivity.kt'
s = main.read_text()

old_list = '''private fun listAssetImages(context: Context, folder: String): List<String> {\n    val allowed = setOf("png", "jpg", "jpeg", "webp")\n    val results = mutableListOf<String>()\n    return try {\n        context.assets.list(folder)?.forEach { fileName ->\n            val ext = fileName.substringAfterLast('.', "").lowercase()\n            when {\n                ext in allowed -> results.add(fileName)\n                folder == "icons" && ext == "zip" -> {\n                    try {\n                        context.assets.open("$folder/$fileName").use { raw ->\n                            java.util.zip.ZipInputStream(raw).use { zip ->\n                                var entry = zip.nextEntry\n                                while (entry != null) {\n                                    if (!entry.isDirectory && entry.name.substringAfterLast('.', "").lowercase() in allowed) {\n                                        results.add("$fileName::${entry.name}")\n                                    }\n                                    zip.closeEntry()\n                                    entry = zip.nextEntry\n                                }\n                            }\n                        }\n                    } catch (_: Exception) { }\n                }\n            }\n        }\n        results.sortedBy { it.substringAfter("::", it).lowercase() }\n    } catch (_: Exception) {\n        emptyList()\n    }\n}\n'''
new_list = '''private fun listAssetImages(context: Context, folder: String): List<String> {\n    val allowed = setOf("png", "jpg", "jpeg", "webp")\n    val results = mutableListOf<String>()\n\n    fun scan(assetFolder: String, relativePrefix: String = "") {\n        context.assets.list(assetFolder)?.forEach { fileName ->\n            val assetPath = "$assetFolder/$fileName"\n            val relativePath = if (relativePrefix.isBlank()) fileName else "$relativePrefix/$fileName"\n            val children = try { context.assets.list(assetPath) } catch (_: Exception) { null }\n            if (!children.isNullOrEmpty()) {\n                scan(assetPath, relativePath)\n                return@forEach\n            }\n\n            val ext = fileName.substringAfterLast('.', "").lowercase()\n            when {\n                ext in allowed -> results.add(relativePath)\n                folder.startsWith("icons") && ext == "zip" -> {\n                    try {\n                        context.assets.open(assetPath).use { raw ->\n                            java.util.zip.ZipInputStream(raw).use { zip ->\n                                var entry = zip.nextEntry\n                                while (entry != null) {\n                                    if (!entry.isDirectory && entry.name.substringAfterLast('.', "").lowercase() in allowed) {\n                                        results.add("$relativePath::${entry.name}")\n                                    }\n                                    zip.closeEntry()\n                                    entry = zip.nextEntry\n                                }\n                            }\n                        }\n                    } catch (_: Exception) { }\n                }\n            }\n        }\n    }\n\n    return try {\n        scan(folder)\n        results.sortedBy { it.substringAfter("::", it).lowercase() }\n    } catch (_: Exception) {\n        emptyList()\n    }\n}\n'''
if old_list not in s:
    raise SystemExit('listAssetImages anchor missing')
s = s.replace(old_list, new_list, 1)

old_loader = '''        } else {\n            context.assets.open("$folder/$fileName").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }\n        }\n    } catch (_: Exception) { null }'''
new_loader = '''        } else {\n            val candidates = if (folder == "icons" && !fileName.contains('/')) {\n                listOf("icons/apps/$fileName", "icons/xp/$fileName", "icons/ui/$fileName", "icons/$fileName")\n            } else {\n                listOf("$folder/$fileName")\n            }\n            var decoded: ImageBitmap? = null\n            for (path in candidates) {\n                try {\n                    decoded = context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }\n                    if (decoded != null) break\n                } catch (_: Exception) { }\n            }\n            decoded\n        }\n    } catch (_: Exception) { null }'''
if old_loader not in s:
    raise SystemExit('loadAssetImage anchor missing')
s = s.replace(old_loader, new_loader, 1)

s = s.replace('context.assets.open("icons/start_button.png")', 'context.assets.open("icons/ui/start_button.png")')

old_auto = '''private fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {\n    val pkg = packageName.lowercase()\n    val name = label.lowercase()\n\n    val asset = when {'''
new_auto = '''private fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {\n    val pkg = packageName.lowercase()\n    val name = label.lowercase()\n\n    // App artwork under icons/apps is named after the Android app. Matching ignores\n    // spaces, punctuation, and case so adding a new PNG normally needs no Kotlin change.\n    val normalizedLabel = name.replace(Regex("[^a-z0-9]"), "")\n    val appNamedIcon = listAssetImages(context, "icons/apps").firstOrNull { file ->\n        val base = file.substringAfter("::", file).substringAfterLast('/').substringBeforeLast('.').lowercase()\n        val normalizedFile = base.replace(Regex("[^a-z0-9]"), "")\n        normalizedFile == normalizedLabel ||\n            (name == "google home" && normalizedFile == "home") ||\n            (name.contains("baby plus") && normalizedFile == "babyplus") ||\n            (name.contains("1kosmos") && normalizedFile == "1kosmos")\n    }\n    if (appNamedIcon != null) {\n        loadAssetImage(context, "icons/apps", appNamedIcon)?.let { return it }\n    }\n\n    val asset = when {'''
if old_auto not in s:
    raise SystemExit('defaultXpAppIcon anchor missing')
s = s.replace(old_auto, new_auto, 1)

old_cat = '''private fun iconCategory(file: String): String {\n    val name = file.substringAfter("::", file).substringAfterLast("/").lowercase()\n    return when {\n        listOf("folder", "document", "file", "explorer", "briefcase").any { it in name } -> "Files & Folders"\n        listOf("computer", "disk", "drive", "cd", "dvd", "usb", "printer", "camera", "scanner").any { it in name } -> "Hardware"\n        listOf("network", "internet", "connection", "mail", "messenger", "phone").any { it in name } -> "Internet & Communications"\n        listOf("control", "setting", "user", "update", "security", "help", "system").any { it in name } -> "System & Control Panel"\n        listOf("media", "music", "video", "picture", "photo", "paint").any { it in name } -> "Media"\n        else -> "Programs & Other"\n    }\n}'''
new_cat = '''private fun iconCategory(file: String): String {\n    val path = file.substringBefore("::").lowercase()\n    val name = file.substringAfter("::", file).substringAfterLast("/").lowercase()\n    return when {\n        path.startsWith("apps/") -> "App Icons"\n        path.startsWith("custom/") -> "Custom Icons"\n        listOf("folder", "document", "file", "explorer", "briefcase").any { it in name } -> "Windows XP • Files & Folders"\n        listOf("computer", "disk", "drive", "cd", "dvd", "usb", "printer", "camera", "scanner").any { it in name } -> "Windows XP • Hardware"\n        listOf("network", "internet", "connection", "mail", "messenger", "phone").any { it in name } -> "Windows XP • Internet & Communications"\n        listOf("control", "setting", "user", "update", "security", "help", "system").any { it in name } -> "Windows XP • System & Control Panel"\n        listOf("media", "music", "video", "picture", "photo", "paint").any { it in name } -> "Windows XP • Media"\n        else -> "Windows XP • Programs & Other"\n    }\n}'''
if old_cat not in s:
    raise SystemExit('iconCategory anchor missing')
s = s.replace(old_cat, new_cat, 1)

main.write_text(s)
print('Icon library reorganized and MainActivity updated.')
