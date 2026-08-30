from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

bad = '''                    RightMenuItem("🔍", "Search") {
                    RightMenuAssetItem(context, "run", "Run...") { onOpenRun() }
                        showSearch = true
                        showAllPrograms = false
                        contextApp = null
                    }
'''

good = '''                    RightMenuItem("🔍", "Search") {
                        showSearch = true
                        showAllPrograms = false
                        contextApp = null
                    }
                    RightMenuAssetItem(context, "run", "Run...") { onOpenRun() }
'''

if bad not in text:
    raise SystemExit("Known Search/Run nesting error not found; transformed source changed")

text = text.replace(bad, good, 1)
path.write_text(text, encoding="utf-8")
print("Fixed Search/Run composable nesting")
