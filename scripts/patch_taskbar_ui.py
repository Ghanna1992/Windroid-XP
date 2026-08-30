from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

# Make the custom Start artwork stand alone on the XP taskbar. No generated
# green button is drawn when the asset exists, and the image keeps its shape.
old_start = '''            Box(
                Modifier.fillMaxHeight().width(104.dp)
                    .then(
                        if (startButtonImage == null) Modifier.background(
                            Brush.verticalGradient(listOf(Color(0xFF55B747), Color(0xFF33952E), Color(0xFF257E25))),
                            RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp)
                        ) else Modifier
                    )
                    .clickable {
                        startOpen = !startOpen
                        computerOpen = false
                        profileOpen = false
                        settingsOpen = false
                    },
                contentAlignment = Alignment.Center
            ) {
                if (startButtonImage != null) {
                    Image(
                        bitmap = startButtonImage,
                        contentDescription = "Start",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    Text("⊞  start", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
'''
new_start = '''            Box(
                Modifier.fillMaxHeight().width(108.dp)
                    .then(
                        if (startButtonImage == null) Modifier.background(
                            Brush.verticalGradient(listOf(Color(0xFF55B747), Color(0xFF33952E), Color(0xFF257E25))),
                            RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp)
                        ) else Modifier
                    )
                    .clickable {
                        startOpen = !startOpen
                        computerOpen = false
                        profileOpen = false
                        settingsOpen = false
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                if (startButtonImage != null) {
                    Image(
                        bitmap = startButtonImage,
                        contentDescription = "Start",
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("⊞  start", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            }
'''
if old_start not in text:
    raise SystemExit("Start button block not found; source changed")
text = text.replace(old_start, new_start, 1)

# Desktop shortcuts should fill downward, then begin a fresh column instead of
# being squeezed into the taskbar when the first column runs out of room.
old_desktop = '''        Column(
            Modifier.fillMaxHeight().padding(start = 10.dp, top = 12.dp, bottom = taskbarHeight + 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_my_computer", "🖥️", "My Computer") {
                computerOpen = true; startOpen = false
            }
            DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_my_documents", "📁", "My Documents") { startOpen = false }
            DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_internet", "🌐", "Internet Explorer") {
                startOpen = false
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
            }
            DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_recycle_bin", "🗑️", "Recycle Bin") { startOpen = false }

            apps.filter { it.packageName in desktopPackages }.forEach { app ->
                DesktopAppIcon(context, prefs, customizationVersion, app) { openAndroidApp(app) }
            }
        }
'''
new_desktop = '''        BoxWithConstraints(
            Modifier.fillMaxSize().padding(start = 10.dp, top = 12.dp, end = 8.dp, bottom = taskbarHeight + 8.dp)
        ) {
            val desktopApps = apps.filter { it.packageName in desktopPackages }
            val totalItems = 4 + desktopApps.size
            val slotHeight = 94.dp
            val rowsPerColumn = (maxHeight.value / slotHeight.value).toInt().coerceAtLeast(1)
            val columnCount = ((totalItems + rowsPerColumn - 1) / rowsPerColumn).coerceAtLeast(1)

            Row(
                Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(columnCount) { columnIndex ->
                    Column(
                        Modifier.width(88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(rowsPerColumn) { rowIndex ->
                            val itemIndex = columnIndex * rowsPerColumn + rowIndex
                            if (itemIndex < totalItems) {
                                when (itemIndex) {
                                    0 -> DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_my_computer", "🖥️", "My Computer") {
                                        computerOpen = true; startOpen = false
                                    }
                                    1 -> DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_my_documents", "📁", "My Documents") { startOpen = false }
                                    2 -> DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_internet", "🌐", "Internet Explorer") {
                                        startOpen = false
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
                                    }
                                    3 -> DesktopBuiltInIcon(context, prefs, customizationVersion, "builtin_recycle_bin", "🗑️", "Recycle Bin") { startOpen = false }
                                    else -> {
                                        val app = desktopApps[itemIndex - 4]
                                        DesktopAppIcon(context, prefs, customizationVersion, app) { openAndroidApp(app) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
'''
if old_desktop not in text:
    raise SystemExit("Desktop icon block not found; source changed")
text = text.replace(old_desktop, new_desktop, 1)

# Replace the obvious cyan mini-bar with a compact XP-style notification area.
old_tray = '''            Row(
                Modifier.fillMaxHeight().background(Color(0xFF1595D1)).padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▰  ◉", color = Color.White, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Clock()
            }
'''
new_tray = '''            XPSystemTray(context)
'''
if old_tray not in text:
    raise SystemExit("Tray block not found; source changed")
text = text.replace(old_tray, new_tray, 1)

# XP taskbar buttons are compact icon buttons. Keep the label as accessibility
# content instead of painting Android-style text across the taskbar.
start = text.index("@Composable\nprivate fun TaskButton(")
end = text.index("\n@OptIn(ExperimentalFoundationApi::class)", start)
new_task = '''@Composable
private fun TaskButton(icon: ImageBitmap?, fallback: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(end = 3.dp).size(34.dp)
            .background(Color(0xFF3579D2), RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFF6AA6F1), RoundedCornerShape(2.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(23.dp), contentScale = ContentScale.Fit)
        } else {
            Text(fallback, color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
private fun XPSystemTray(context: Context) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier.fillMaxHeight()
                .background(Brush.verticalGradient(listOf(Color(0xFF2FA6E2), Color(0xFF1685C5))))
                .border(width = 1.dp, color = Color(0xFF57B9E8))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(25.dp).clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Text(if (expanded) "›" else "‹", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(2.dp))
            Text("◉", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            })
            Spacer(Modifier.width(6.dp))
            Text("ᛒ", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            })
            Spacer(Modifier.width(7.dp))
            Clock()
        }

        if (expanded) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(bottom = 45.dp)
                    .width(172.dp).shadow(8.dp)
                    .background(Color(0xFFF5F4EA))
                    .border(1.dp, Color(0xFF7F9DB9))
                    .padding(5.dp)
            ) {
                Text("Notification Area", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                ContextMenuRow("📶  Wi-Fi settings") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
                ContextMenuRow("ᛒ  Bluetooth settings") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }
                ContextMenuRow("🔦  Flashlight / quick controls") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }
}
'''
text = text[:start] + new_task + text[end:]

path.write_text(text, encoding="utf-8")
print("Patched Windroid XP taskbar UI and desktop grid")
