from pathlib import Path
p=Path('app/src/main/java/com/windroid/xp/MainActivity.kt')
s=p.read_text()
old='''private fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {\n    val pkg = packageName.lowercase()\n    val name = label.lowercase()\n\n    val asset = when {'''
new='''private fun defaultXpAppIcon(context: Context, packageName: String, label: String): ImageBitmap? {\n    val pkg = packageName.lowercase()\n    val name = label.lowercase()\n\n    // Custom Windroid app icons are intentionally named after the Android app.\n    // This lets newly-added icons auto-apply without needing another hard-coded mapping.\n    val customIconNames = listAssetImages(context, "icons")\n    val normalizedLabel = name.replace(Regex("[^a-z0-9]"), "")\n    val appNamedIcon = customIconNames.firstOrNull { file ->\n        val base = file.substringAfter("::", file).substringAfterLast('/').substringBeforeLast('.').lowercase()\n        val normalizedFile = base.replace(Regex("[^a-z0-9]"), "")\n        normalizedFile == normalizedLabel ||\n            (name == "google home" && normalizedFile == "home") ||\n            (name.contains("baby plus") && normalizedFile == "babyplus") ||\n            (name.contains("1kosmos") && normalizedFile == "1kosmos")\n    }\n    if (appNamedIcon != null) {\n        loadAssetImage(context, "icons", appNamedIcon)?.let { return it }\n    }\n\n    val asset = when {'''
if old not in s: raise SystemExit('anchor missing')
s=s.replace(old,new,1)
p.write_text(s)
