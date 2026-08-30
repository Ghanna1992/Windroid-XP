from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

# Desktop long-press menus must be true popups. Keeping the menu inside the
# 88dp desktop icon Box caused Compose to measure it as part of the desktop
# grid, producing the tall cream-colored strips seen on device.
old_menu = '''        if (menuOpen) {
            Column(
                Modifier.padding(start = 64.dp, top = 28.dp).width(190.dp).shadow(10.dp)
                    .background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(5.dp)
            ) {
                Text(app.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp))
                ContextMenuRow("Open") {
                    menuOpen = false
                    onClick()
                }
                ContextMenuRow("Remove from Desktop") {
                    menuOpen = false
                    onRemove()
                }
                ContextMenuRow("App Info") {
                    menuOpen = false
                    openAppInfo(context, app.packageName)
                }
                ContextMenuRow("Cancel") { menuOpen = false }
            }
        }
'''
new_menu = '''        if (menuOpen) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                offset = androidx.compose.ui.unit.IntOffset(58, 28),
                onDismissRequest = { menuOpen = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true)
            ) {
                Column(
                    Modifier.width(190.dp).shadow(10.dp)
                        .background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(5.dp)
                ) {
                    Text(app.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp))
                    ContextMenuRow("Open") {
                        menuOpen = false
                        onClick()
                    }
                    ContextMenuRow("Remove from Desktop") {
                        menuOpen = false
                        onRemove()
                    }
                    ContextMenuRow("App Info") {
                        menuOpen = false
                        openAppInfo(context, app.packageName)
                    }
                    ContextMenuRow("Cancel") { menuOpen = false }
                }
            }
        }
'''
if old_menu not in text:
    raise SystemExit("Desktop popup block not found; source changed")
text = text.replace(old_menu, new_menu, 1)

# Rebuild the tray as only the XP up-chevron and clock. Wi-Fi/Bluetooth/etc.
# belong in the popup menu, not permanently on the taskbar. The popup is a
# real window overlay so it cannot stretch or reflow the taskbar.
tray_start = text.index("@Composable\nprivate fun XPSystemTray(context: Context)")
tray_end = text.index("\n@OptIn(ExperimentalFoundationApi::class)", tray_start)
new_tray = '''@Composable
private fun XPSystemTray(context: Context) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxHeight().width(112.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF2F82D7), Color(0xFF1764B8))))
            .border(width = 1.dp, color = Color(0xFF4C99E2))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(24.dp).clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Text("⌃", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(7.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Clock()
        }
    }

    if (expanded) {
        androidx.compose.ui.window.Popup(
            alignment = Alignment.BottomEnd,
            offset = androidx.compose.ui.unit.IntOffset(0, -44),
            onDismissRequest = { expanded = false },
            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
        ) {
            Column(
                Modifier.width(194.dp).shadow(10.dp)
                    .background(Color(0xFFF5F4EA))
                    .border(1.dp, Color(0xFF7F9DB9))
                    .padding(5.dp)
            ) {
                ContextMenuRow("Wi-Fi settings") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                ContextMenuRow("Bluetooth settings") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
                ContextMenuRow("Flashlight / quick controls") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }
}
'''
text = text[:tray_start] + new_tray + text[tray_end:]

path.write_text(text, encoding="utf-8")
print("Fixed desktop context popups and compact XP tray")
