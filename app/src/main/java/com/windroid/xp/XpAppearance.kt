package com.windroid.xp

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable internal fun AppearanceWindow(context: Context, apps: List<LaunchableApp>, backgrounds: List<String>, iconFiles: List<String>, selectedBackground: String?, prefs: android.content.SharedPreferences, desktopPackages: Set<String>, hiddenBuiltinShortcuts: Set<String>, onBackgroundSelected: (String?) -> Unit, onIconSelected: (String, String?) -> Unit, onDesktopToggle: (String, Boolean) -> Unit, onBuiltinToggle: (String, Boolean) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var page by remember { mutableStateOf("home") }; var iconTarget by remember { mutableStateOf<String?>(null) }; var iconTargetLabel by remember { mutableStateOf("") }; var iconSearch by remember { mutableStateOf("") }; var myIconsOpen by remember { mutableStateOf(false) }
    XPWindow("Display Properties", modifier.width(350.dp), onClose = onClose) { when {
        myIconsOpen && iconTarget != null -> {
            CustomIconPicker(
                context = context,
                onSelected = { customId ->
                    onIconSelected(iconTarget!!, customId)
                    iconTarget = null
                    iconSearch = ""
                    myIconsOpen = false
                },
                onBack = { myIconsOpen = false }
            )
        }
        iconTarget != null -> { Text("Choose icon for $iconTargetLabel", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); BasicTextField(value = iconSearch, onValueChange = { iconSearch = it.take(50) }, singleLine = true, modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(7.dp)); Text("Search the XP icon library by name. Showing up to 120 matches.", fontSize = 9.sp, color = Color(0xFF666666), modifier = Modifier.padding(vertical = 5.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { XPActionButton("My Icons...") { myIconsOpen = true }; XPActionButton("Use default") { onIconSelected(iconTarget!!, null); iconTarget = null; iconSearch = "" } }; Spacer(Modifier.height(7.dp)); val visibleIcons = remember(iconFiles, iconSearch) { val q = iconSearch.trim(); (if (q.isBlank()) iconFiles else iconFiles.filter { it.substringAfter("::", it).substringAfterLast("/").contains(q, ignoreCase = true) }).take(120) }; val groupedIcons = remember(visibleIcons) { visibleIcons.groupBy { iconCategory(it) }.toSortedMap() }; Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) { groupedIcons.forEach { (category, files) -> Text(category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003399), modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp)); files.forEach { file -> val image = remember(file) { loadAssetImage(context, "icons", file) }; PickerRow(file.substringAfter("::", file).substringAfterLast("/"), image) { onIconSelected(iconTarget!!, file); iconTarget = null; iconSearch = "" } } } }; Spacer(Modifier.height(8.dp)); XPActionButton("Back") { iconTarget = null } }
        page == "myIcons" -> { CustomIconPicker(context = context, onBack = { page = "home" }) }
        page == "background" -> { Text("Desktop Background", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); Column(Modifier.height(330.dp).verticalScroll(rememberScrollState())) { val bliss = remember { loadAssetImage(context, "backgrounds", DEFAULT_DESKTOP_BACKGROUND) }; PickerRow("Default (Bliss)", bliss, selectedBackground == DEFAULT_DESKTOP_BACKGROUND) { onBackgroundSelected(DEFAULT_DESKTOP_BACKGROUND) }; backgrounds.filterNot { it == DEFAULT_DESKTOP_BACKGROUND }.forEach { file -> val image = remember(file) { loadAssetImage(context, "backgrounds", file) }; PickerRow(file, image, selectedBackground == file) { onBackgroundSelected(file) } } }; Spacer(Modifier.height(8.dp)); XPActionButton("Back") { page = "home" } }
        page == "desktopIcons" -> { Text("Desktop Icons", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(8.dp)); Column(Modifier.height(330.dp).verticalScroll(rememberScrollState())) { listOf("builtin_my_computer" to "My Computer", "builtin_my_documents" to "My Documents", "builtin_internet" to "Internet Explorer", "builtin_recycle_bin" to "Recycle Bin").forEach { (id, label) -> AssignmentRow(label, prefs.getString(iconPrefKey(id), null), id !in hiddenBuiltinShortcuts, { onBuiltinToggle(id, id in hiddenBuiltinShortcuts) }, { iconTarget = id; iconTargetLabel = label }) } }; Spacer(Modifier.height(8.dp)); XPActionButton("Back") { page = "home" } }
        page == "apps" -> { Text("Applications", fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("Assign custom icons or pin apps to the XP desktop.", fontSize = 10.sp, color = Color(0xFF666666)); Spacer(Modifier.height(8.dp)); Column(Modifier.height(350.dp).verticalScroll(rememberScrollState())) { apps.forEach { app -> AppCustomizationRow(app, prefs.getString(iconPrefKey("app_${app.packageName}"), null), { iconTarget = "app_${app.packageName}"; iconTargetLabel = app.label }, { onDesktopToggle(app.packageName, app.packageName !in desktopPackages) }, app.packageName in desktopPackages) } }; Spacer(Modifier.height(8.dp)); XPActionButton("Back") { page = "home" } }
        else -> { Text("Customize Windroid XP", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.height(12.dp)); SettingsChoice("🖼️", "Desktop Background", "Choose a wallpaper; Bliss is the install default") { page = "background" }; SettingsChoice("🖥️", "Desktop Icons", "Assign or hide Windroid shortcuts") { page = "desktopIcons" }; SettingsChoice("📦", "Applications", "Custom app icons and desktop shortcuts") { page = "apps" }; SettingsChoice("🗂️", "My Icons", "Import and manage your personal icon library") { page = "myIcons" }; Spacer(Modifier.height(14.dp)); Text("Desktop icons can be dragged anywhere and may overlap, just like old-school Windows.", fontSize = 10.sp, color = Color(0xFF666666), modifier = Modifier.padding(8.dp)); Spacer(Modifier.height(12.dp)); XPActionButton("Close") { onClose() } }
    } }
}

@Composable internal fun SettingsChoice(icon: String, title: String, subtitle: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 25.sp); Spacer(Modifier.width(10.dp)); Column { Text(title, color = Color(0xFF003399), fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(subtitle, fontSize = 10.sp, color = Color(0xFF555555)) } } }
@Composable internal fun PickerRow(label: String, image: ImageBitmap?, selected: Boolean = false, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable { onClick() }.background(if (selected) Color(0xFFDCEBFA) else Color.Transparent).padding(horizontal = 6.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Color.White).border(1.dp, Color(0xFFB7B7B7)), contentAlignment = Alignment.Center) { if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit) else Text("Default", fontSize = 8.sp) }; Spacer(Modifier.width(9.dp)); Text(label, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
@Composable internal fun AssignmentRow(label: String, assignedFile: String?, visible: Boolean, onToggleVisibility: () -> Unit, onChange: () -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(if (visible) (assignedFile ?: "Default icon") else "Hidden from desktop", fontSize = 9.sp, color = Color(0xFF666666), maxLines = 1, overflow = TextOverflow.Ellipsis) }; Text(if (visible) "Remove" else "Restore", color = Color(0xFF003399), fontSize = 10.sp, modifier = Modifier.clickable { onToggleVisibility() }.padding(5.dp)); Spacer(Modifier.width(4.dp)); XPActionButton("Change") { onChange() } } }
@Composable internal fun AppCustomizationRow(app: LaunchableApp, assignedFile: String?, onChangeIcon: () -> Unit, onToggleDesktop: () -> Unit, onDesktop: Boolean) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { if (app.icon != null) Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(28.dp)) else Text("▣", fontSize = 20.sp); Spacer(Modifier.width(7.dp)); Column(Modifier.weight(1f)) { Text(app.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(assignedFile ?: "Android icon", fontSize = 8.sp, color = Color(0xFF666666), maxLines = 1, overflow = TextOverflow.Ellipsis) }; Text("Icon", color = Color(0xFF003399), fontSize = 10.sp, modifier = Modifier.clickable { onChangeIcon() }.padding(5.dp)); Spacer(Modifier.width(4.dp)); Text(if (onDesktop) "Remove" else "Desktop", color = Color(0xFF003399), fontSize = 10.sp, modifier = Modifier.clickable { onToggleDesktop() }.padding(5.dp)) } }
@Composable internal fun XPActionButton(label: String, onClick: () -> Unit) { Box(Modifier.background(Color(0xFFECE9D8), RoundedCornerShape(2.dp)).border(1.dp, Color(0xFF7F9DB9), RoundedCornerShape(2.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 6.dp)) { Text(label, fontSize = 12.sp, color = Color.Black) } }
