from pathlib import Path

main = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = main.read_text()

start_marker = '        if (controlPanelOpen) XPWindow("Control Panel", Modifier.align(Alignment.Center), onClose = { controlPanelOpen = false }) {'
end_marker = '\n\n        if (recycleOpen) XPWindow("Recycle Bin"'

count = text.count(start_marker)
if count != 1:
    raise SystemExit(f"Refusing Control Panel swap: expected exactly one old Control Panel block, found {count}")

start = text.index(start_marker)
end = text.index(end_marker, start)
old = text[start:end]

required = [
    'XPSystemRow(context, "update", "Automatic Updates")',
    'XPSystemRow(context, "computer", "About Windroid XP")',
    'XPSystemRow(context, "back", "Restore Windroid Defaults")',
]
for token in required:
    if token not in old:
        raise SystemExit(f"Refusing Control Panel swap: safety route missing: {token}")

new = '''        if (controlPanelOpen) XpControlPanel(
            onClose = { controlPanelOpen = false },
            onAppearance = { controlPanelOpen = false; settingsOpen = true },
            onUserAccounts = { controlPanelOpen = false; profileOpen = true },
            onWindowsUpdate = { controlPanelOpen = false; checkForUpdates(true) },
            onAbout = { controlPanelOpen = false; aboutOpen = true },
            onRestoreDefaults = { resetConfirmOpen = true },
            modifier = Modifier.align(Alignment.Center)
        )'''

main.write_text(text[:start] + new + text[end:])

notes = Path("release-notes/current.md")
body = notes.read_text()
line = "- Control Panel redesigned around the classic Windows XP category view while preserving Windows Update and About access."
if line not in body:
    notes.write_text(body.rstrip() + "\n" + line + "\n")
