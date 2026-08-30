from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

# Give the Start asset real edge transparency even if the source PNG was saved
# on a white/light-gray canvas. We flood-fill only neutral pixels connected to
# the image edge, so white lettering/logo inside the green button is preserved.
old_loader = '''private fun loadAssetImage(context: Context, folder: String, fileName: String?): ImageBitmap? {
    if (fileName.isNullOrBlank()) return null
    return try {
        context.assets.open("$folder/$fileName").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    } catch (_: Exception) {
        null
    }
}
'''
new_loader = '''private fun loadAssetImage(context: Context, folder: String, fileName: String?): ImageBitmap? {
    if (fileName.isNullOrBlank()) return null
    return try {
        context.assets.open("$folder/$fileName").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    } catch (_: Exception) {
        null
    }
}

private fun loadStartButtonImage(context: Context): ImageBitmap? {
    return try {
        val decoded = context.assets.open("icons/start_button.png").use { BitmapFactory.decodeStream(it) } ?: return null
        val bitmap = decoded.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap.asImageBitmap()

        fun removableBackground(pixel: Int): Boolean {
            val a = android.graphics.Color.alpha(pixel)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val average = (r + g + b) / 3
            return a == 0 || (max - min <= 30 && average >= 135)
        }

        val seen = BooleanArray(width * height)
        val queue = java.util.ArrayDeque<Int>()
        fun seed(x: Int, y: Int) {
            val index = y * width + x
            if (!seen[index] && removableBackground(bitmap.getPixel(x, y))) {
                seen[index] = true
                queue.add(index)
            }
        }
        for (x in 0 until width) {
            seed(x, 0)
            seed(x, height - 1)
        }
        for (y in 0 until height) {
            seed(0, y)
            seed(width - 1, y)
        }
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val x = index % width
            val y = index / width
            bitmap.setPixel(x, y, android.graphics.Color.TRANSPARENT)
            if (x > 0) seed(x - 1, y)
            if (x + 1 < width) seed(x + 1, y)
            if (y > 0) seed(x, y - 1)
            if (y + 1 < height) seed(x, y + 1)
        }
        bitmap.asImageBitmap()
    } catch (_: Exception) {
        loadAssetImage(context, "icons", "start_button.png")
    }
}
'''
if old_loader not in text:
    raise SystemExit("Asset loader block not found; source changed")
text = text.replace(old_loader, new_loader, 1)
text = text.replace(
    'val startButtonImage = remember { loadAssetImage(context, "icons", "start_button.png") }',
    'val startButtonImage = remember { loadStartButtonImage(context) }',
    1
)

# Make the custom Start artwork stand alone on the XP taskbar.
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

# Desktop shortcuts fill down, then begin a new column. App shortcuts also get
# an XP-style long-press menu so they can be opened, removed, or inspected.
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
                                        DesktopAppIcon(
                                            context = context,
                                            prefs = prefs,
                                            version = customizationVersion,
                                            app = app,
                                            onClick = { openAndroidApp(app) },
                                            onRemove = { setDesktopApp(app.packageName, false) }
                                        )
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

old_desktop_app = '''@Composable
private fun DesktopAppIcon(
    context: Context,
    prefs: android.content.SharedPreferences,
    version: Int,
    app: LaunchableApp,
    onClick: () -> Unit
) {
    val image = remember(version, app.packageName) {
        loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null))
    }
    DesktopIcon(image ?: app.icon, "▣", app.label, onClick)
}

@Composable
private fun DesktopIcon(image: ImageBitmap?, fallback: String, label: String, onClick: () -> Unit) {
    Column(Modifier.width(88.dp).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.size(42.dp), contentScale = ContentScale.Fit)
        else Text(fallback, fontSize = 37.sp)
        Text(label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp, maxLines = 2)
    }
}
'''
new_desktop_app = '''@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopAppIcon(
    context: Context,
    prefs: android.content.SharedPreferences,
    version: Int,
    app: LaunchableApp,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val image = remember(version, app.packageName) {
        loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null))
    }
    var menuOpen by remember { mutableStateOf(false) }
    Box(Modifier.width(88.dp)) {
        Column(
            Modifier.width(88.dp).combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true }
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val icon = image ?: app.icon
            if (icon != null) Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(42.dp), contentScale = ContentScale.Fit)
            else Text("▣", fontSize = 37.sp)
            Text(app.label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp, maxLines = 2)
        }

        if (menuOpen) {
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
    }
}

@Composable
private fun DesktopIcon(image: ImageBitmap?, fallback: String, label: String, onClick: () -> Unit) {
    Column(Modifier.width(88.dp).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.size(42.dp), contentScale = ContentScale.Fit)
        else Text(fallback, fontSize = 37.sp)
        Text(label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp, maxLines = 2)
    }
}
'''
if old_desktop_app not in text:
    raise SystemExit("Desktop app icon function not found; source changed")
text = text.replace(old_desktop_app, new_desktop_app, 1)

# Replace the old mini-bar with a fixed-width XP-style notification area.
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

# XP taskbar buttons are compact icon-only buttons. The tray has a fixed up
# chevron and popup menu; opening it never changes the clock position.
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

    Box(Modifier.fillMaxHeight().width(146.dp)) {
        Row(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF2F82D7), Color(0xFF1764B8))))
                .border(width = 1.dp, color = Color(0xFF4C99E2))
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(24.dp).clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Text("⌃", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(3.dp))
            Text("◉", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            })
            Spacer(Modifier.width(7.dp))
            Text("ᛒ", color = Color.White, fontSize = 12.sp, modifier = Modifier.clickable {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            })
            Spacer(Modifier.weight(1f))
            Clock()
        }

        if (expanded) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(bottom = 45.dp)
                    .width(205.dp).shadow(10.dp)
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
                ContextMenuRow("Android Settings") {
                    expanded = false
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                ContextMenuRow("Cancel") { expanded = false }
            }
        }
    }
}
'''
text = text[:start] + new_task + text[end:]

# Give the clock the compact XP tray look and a fixed width so neighboring tray
# icons never shove it around.
old_clock = '''@Composable
private fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = Date()
        }
    }
    Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(now), color = Color.White, fontSize = 12.sp)
}
'''
new_clock = '''@Composable
private fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = Date()
        }
    }
    Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) {
        Text(
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(now),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}
'''
if old_clock not in text:
    raise SystemExit("Clock function not found; source changed")
text = text.replace(old_clock, new_clock, 1)

path.write_text(text, encoding="utf-8")
print("Patched Windroid XP desktop menus, tray, clock, grid, and Start transparency")
