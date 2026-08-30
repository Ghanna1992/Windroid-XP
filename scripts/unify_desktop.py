from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

# Add a registry key for the XP browser shortcut.
if '    "internet" to "Internet Explorer 6.png",' not in text:
    text = text.replace('    "documents" to "My Documents.png",', '    "documents" to "My Documents.png",\n    "internet" to "Internet Explorer 6.png",', 1)

old_cases = '''                                    0 -> DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_my_computer", "🖥️", "My Computer") {
                                        computerOpen = true; startOpen = false
                                    }
                                    1 -> DesktopResolvedBuiltInIcon(
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
                                    3 -> DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_recycle_bin", "🗑️", "Recycle Bin") { startOpen = false; recycleOpen = true }'''

new_cases = '''                                    0 -> DesktopSystemShortcut(
                                        context, prefs, customizationVersion,
                                        "builtin_my_computer", "computer", "🖥️", "My Computer",
                                        onClick = { computerOpen = true; startOpen = false },
                                        onCustomize = { startOpen = false; settingsOpen = true },
                                        onReset = { setIcon("builtin_my_computer", null) }
                                    )
                                    1 -> DesktopSystemShortcut(
                                        context, prefs, customizationVersion,
                                        "builtin_my_documents", "documents", "📁", "My Documents",
                                        onClick = { startOpen = false; openDocuments(context) },
                                        onCustomize = { startOpen = false; settingsOpen = true },
                                        onReset = { setIcon("builtin_my_documents", null) }
                                    )
                                    2 -> DesktopSystemShortcut(
                                        context, prefs, customizationVersion,
                                        "builtin_internet", "internet", "🌐", "Internet Explorer",
                                        onClick = { startOpen = false; openDefaultBrowser(context) },
                                        onCustomize = { startOpen = false; settingsOpen = true },
                                        onReset = { setIcon("builtin_internet", null) }
                                    )
                                    3 -> DesktopSystemShortcut(
                                        context, prefs, customizationVersion,
                                        "builtin_recycle_bin", "recycle", "🗑️", "Recycle Bin",
                                        onClick = { startOpen = false; recycleOpen = true },
                                        onCustomize = { startOpen = false; settingsOpen = true },
                                        onReset = { setIcon("builtin_recycle_bin", null) }
                                    )'''

if old_cases not in text:
    raise SystemExit("built-in desktop cases not found")
text = text.replace(old_cases, new_cases, 1)

anchor = '@Composable\nprivate fun DesktopBuiltInIcon('
helper = '''@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopSystemShortcut(
    context: Context,
    prefs: android.content.SharedPreferences,
    version: Int,
    id: String,
    registryKey: String,
    fallback: String,
    label: String,
    onClick: () -> Unit,
    onCustomize: () -> Unit,
    onReset: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val customName = remember(id, version) { prefs.getString(iconPrefKey(id), null) }
    val customIcon = remember(id, version, customName) { loadAssetImage(context, "icons", customName) }
    val icon = customIcon ?: xpIcon(context, registryKey)

    Box {
        Column(
            Modifier.width(86.dp).combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true }
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
            } else {
                Text(fallback, fontSize = 38.sp)
            }
            Spacer(Modifier.height(3.dp))
            Text(label, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        if (menuOpen) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(55, 28),
                onDismissRequest = { menuOpen = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier.width(150.dp).background(Color(0xFFFFF8E7)).border(1.dp, Color(0xFF777777)).padding(4.dp)
                ) {
                    Text("Open", modifier = Modifier.fillMaxWidth().clickable { menuOpen = false; onClick() }.padding(7.dp), fontSize = 12.sp)
                    Text("Change Icon...", modifier = Modifier.fillMaxWidth().clickable { menuOpen = false; onCustomize() }.padding(7.dp), fontSize = 12.sp)
                    if (customName != null) {
                        Text("Reset Icon", modifier = Modifier.fillMaxWidth().clickable { menuOpen = false; onReset() }.padding(7.dp), fontSize = 12.sp)
                    }
                    Text("Cancel", modifier = Modifier.fillMaxWidth().clickable { menuOpen = false }.padding(7.dp), fontSize = 12.sp)
                }
            }
        }
    }
}

'''
if anchor not in text:
    raise SystemExit("DesktopBuiltInIcon anchor not found")
text = text.replace(anchor, helper + anchor, 1)

path.write_text(text, encoding="utf-8")
print("Unified built-in desktop shortcuts with XP defaults and long-press menus")
