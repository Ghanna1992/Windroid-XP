from pathlib import Path

path = Path("app/src/main/java/com/windroid/xp/MainActivity.kt")
text = path.read_text(encoding="utf-8")

old_state = '''    var showAllPrograms by remember { mutableStateOf(false) }
    var contextApp by remember { mutableStateOf<LaunchableApp?>(null) }

    BackHandler(enabled = contextApp != null || showAllPrograms) {
        if (contextApp != null) contextApp = null else showAllPrograms = false
    }
'''
new_state = '''    var showAllPrograms by remember { mutableStateOf(false) }
    var contextApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = contextApp != null || showSearch || showAllPrograms) {
        when {
            contextApp != null -> contextApp = null
            showSearch -> { showSearch = false; searchQuery = "" }
            else -> showAllPrograms = false
        }
    }
'''
if old_state not in text:
    raise SystemExit("StartMenu state block not found; source changed")
text = text.replace(old_state, new_state, 1)

old_search = '''                    RightMenuItem("🔍", "Search") { }
'''
new_search = '''                    RightMenuItem("🔍", "Search") {
                        showSearch = true
                        showAllPrograms = false
                        contextApp = null
                    }
'''
if old_search not in text:
    raise SystemExit("Search menu item not found; source changed")
text = text.replace(old_search, new_search, 1)

anchor = '''        contextApp?.let { app ->
'''
search_overlay = '''        if (showSearch) {
            Column(
                Modifier.align(Alignment.Center).width(290.dp).heightIn(max = 470.dp).shadow(12.dp)
                    .background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(10.dp)
            ) {
                Text("Search Programs", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(60) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .border(1.dp, Color(0xFF7F9DB9)).padding(horizontal = 8.dp, vertical = 7.dp)
                )
                Spacer(Modifier.height(8.dp))
                val results = apps.filter {
                    searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true)
                }
                Column(Modifier.weight(1f, fill = false).heightIn(max = 330.dp).verticalScroll(rememberScrollState())) {
                    if (results.isEmpty()) {
                        Text("No programs found.", color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                    } else {
                        results.take(60).forEach { app ->
                            StartMenuAppItem(
                                context = context,
                                prefs = prefs,
                                version = customizationVersion,
                                app = app,
                                onClick = {
                                    showSearch = false
                                    searchQuery = ""
                                    onLaunchApp(app)
                                },
                                onLongClick = {
                                    showSearch = false
                                    contextApp = app
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                XPActionButton("Close") { showSearch = false; searchQuery = "" }
            }
        }

'''
if anchor not in text:
    raise SystemExit("StartMenu popup anchor not found; source changed")
text = text.replace(anchor, search_overlay + anchor, 1)

path.write_text(text, encoding="utf-8")
print("Added Start menu app search")
