package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun XpStartMenu(
    apps: List<LaunchableApp>,
    recentApps: List<LaunchableApp>,
    context: Context,
    prefs: SharedPreferences,
    customizationVersion: Int,
    desktopPackages: Set<String>,
    userName: String,
    userAvatar: String,
    onEditProfile: () -> Unit,
    onLaunchApp: (LaunchableApp) -> Unit,
    onToggleDesktop: (LaunchableApp) -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenComputer: () -> Unit,
    onOpenControlPanel: () -> Unit,
    onOpenRecycle: () -> Unit,
    onOpenRun: () -> Unit,
    onCheckUpdates: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllPrograms by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showPower by remember { mutableStateOf(false) }
    var showRecentDocs by remember { mutableStateOf(false) }
    var showMusic by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showPrinters by remember { mutableStateOf(false) }
    var contextApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler(enabled = showAllPrograms || showSearch || showPower || showRecentDocs || showMusic || showHelp || showPrinters || contextApp != null) {
        when {
            contextApp != null -> contextApp = null
            showPower -> showPower = false
            showSearch -> { showSearch = false; searchQuery = "" }
            showRecentDocs -> showRecentDocs = false
            showMusic -> showMusic = false
            showHelp -> showHelp = false
            showPrinters -> showPrinters = false
            else -> showAllPrograms = false
        }
    }

    val blueTop = Color(0xFF1F6CD2)
    val blueBottom = Color(0xFF0B4EB3)
    val paleBlue = Color(0xFFDCEBFA)
    val recent = recentApps.take(8)

    Box(modifier.width(404.dp).heightIn(max = 610.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .shadow(10.dp)
                .border(2.dp, Color(0xFF1551A8))
                .background(Color.White)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF3F8BEA), blueTop, blueBottom)))
                    .clickable { onEditProfile() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(54.dp)
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .border(2.dp, Color(0xFFF4F4F4), RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(userAvatar, fontSize = 31.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(userName, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFF49B36)))

            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.weight(1.08f).fillMaxHeight().background(Color.White)) {
                    if (showAllPrograms) {
                        Row(Modifier.fillMaxWidth().clickable { showAllPrograms = false }.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("◀", color = Color(0xFF1E6DB8), fontSize = 13.sp)
                            Spacer(Modifier.width(7.dp))
                            Text("Back", color = Color(0xFF1E4F8A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        XpStartSeparator()
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            apps.forEach { app -> XpProgramRow(app, onClick = { onLaunchApp(app) }, onLongClick = { contextApp = app }) }
                        }
                    } else {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 5.dp)) {
                            if (recent.isEmpty()) Text("Recently used programs will appear here.", color = Color(0xFF666666), fontSize = 10.sp, modifier = Modifier.padding(14.dp))
                            else recent.forEach { app -> XpProgramRow(app, onClick = { onLaunchApp(app) }, onLongClick = { contextApp = app }) }
                            Spacer(Modifier.height(4.dp))
                        }
                        XpStartSeparator()
                        Row(Modifier.fillMaxWidth().clickable { showAllPrograms = true }.padding(horizontal = 13.dp, vertical = 11.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            Text("All Programs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
                            Spacer(Modifier.width(9.dp)); XpGreenArrow()
                        }
                    }
                }

                Column(Modifier.weight(0.92f).fillMaxHeight().background(paleBlue).border(width = 1.dp, color = Color(0xFFB7CDE5)).padding(vertical = 6.dp)) {
                    XpSystemMenuRow(context, "My Documents.png", "My Documents") { openDocumentPicker(context, "*/*") }
                    XpSystemMenuRow(context, "My Pictures.png", "My Pictures") { openDocumentPicker(context, "image/*") }
                    XpSystemMenuRow(context, "My Music.png", "My Music", arrow = true) { showMusic = true }
                    XpSystemMenuRow(context, "My Computer.png", "My Computer", bold = true) { onOpenComputer() }
                    XpSystemMenuRow(context, "Recent Documents.png", "Recent Documents", arrow = true) { showRecentDocs = true }
                    XpStartSeparator(right = true)
                    XpSystemMenuRow(context, "Control Panel.png", "Control Panel", bold = true) { onOpenControlPanel() }
                    XpSystemMenuRow(context, "Printers and Faxes.png", "Printers and Faxes") { showPrinters = true }
                    XpStartSeparator(right = true)
                    XpSystemMenuRow(context, "Search.png", "Search", arrow = true) { showSearch = true; showAllPrograms = false }
                    XpSystemMenuRow(context, "Help and Support.png", "Help and Support") { showHelp = true }
                    XpSystemMenuRow(context, "Run.png", "Run...") { onOpenRun() }
                    Spacer(Modifier.weight(1f))
                }
            }

            Row(Modifier.fillMaxWidth().height(50.dp).background(Brush.verticalGradient(listOf(Color(0xFF2D7CD8), Color(0xFF0B52B7)))).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                XpFooterAction(context, "Logout.png", "Log Off") { onEditProfile() }
                Spacer(Modifier.width(18.dp))
                XpFooterAction(context, "Power.png", "Turn Off Computer") { showPower = true }
            }
        }

        if (showSearch) XpStartPopup(Modifier.align(Alignment.Center), "Search") {
            Text("Search for programs", fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
            BasicTextField(value = searchQuery, onValueChange = { searchQuery = it.take(60) }, singleLine = true, modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(7.dp))
            Spacer(Modifier.height(8.dp))
            val q = searchQuery.trim(); val results = if (q.isBlank()) apps.take(10) else apps.filter { it.label.contains(q, true) || it.packageName.contains(q, true) }.take(20)
            Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                if (results.isEmpty()) Text("No programs found.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                results.forEach { app -> XpProgramRow(app, onClick = { showSearch = false; searchQuery = ""; onLaunchApp(app) }, onLongClick = { contextApp = app }) }
            }
        }

        if (showRecentDocs) XpStartPopup(Modifier.align(Alignment.Center), "Recent Documents") {
            Text("There are no recent documents to display.", fontSize = 11.sp); Spacer(Modifier.height(7.dp)); Text("Windroid XP does not track your private document history yet.", fontSize = 9.sp, color = Color(0xFF666666))
        }
        if (showMusic) XpStartPopup(Modifier.align(Alignment.Center), "My Music") {
            XpPopupLink("Open audio files") { showMusic = false; openDocumentPicker(context, "audio/*") }
            XpPopupLink("Windows Media Player") { showMusic = false; val media = apps.firstOrNull { it.label.contains("music", true) || it.label.contains("media", true) }; if (media != null) onLaunchApp(media) else openDocumentPicker(context, "audio/*") }
        }
        if (showPrinters) XpStartPopup(Modifier.align(Alignment.Center), "Printers and Faxes") {
            Text("No printers are currently installed.", fontSize = 11.sp); Spacer(Modifier.height(8.dp)); XpPopupLink("Open Android printing settings") { showPrinters = false; try { context.startActivity(Intent(Settings.ACTION_PRINT_SETTINGS)) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
        }
        if (showHelp) XpStartPopup(Modifier.align(Alignment.Center), "Help and Support") {
            Text("Pick a task", fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); XpPopupLink("Windows Update") { showHelp = false; onCheckUpdates() }; XpPopupLink("Change the appearance of Windroid XP") { showHelp = false; onOpenAppearance() }; XpPopupLink("About Windroid XP") { showHelp = false; onOpenAbout() }; XpPopupLink("Recycle Bin") { showHelp = false; onOpenRecycle() }
        }
        if (showPower) XpStartPopup(Modifier.align(Alignment.Center), "Turn off computer") {
            Text("What do you want the computer to do?", fontSize = 11.sp); Spacer(Modifier.height(7.dp))
            XpPopupLink("Return to Android / Change Home App") { showPower = false; try { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
            XpPopupLink("Android Settings") { showPower = false; context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            XpPopupLink("Restart Windroid XP") { showPower = false; context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(it) } }
            XpPopupLink("Cancel") { showPower = false }
        }
        contextApp?.let { app ->
            XpStartPopup(Modifier.align(Alignment.Center), app.label) {
                XpPopupLink(if (app.packageName in desktopPackages) "Remove from Desktop" else "Add to Desktop") { onToggleDesktop(app); contextApp = null }
                XpPopupLink("App Info") { contextApp = null; try { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${app.packageName}") }) } catch (_: Exception) { } }
                XpPopupLink("Cancel") { contextApp = null }
            }
        }
    }
}

private fun openDocumentPicker(context: Context, mime: String) { try { context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = mime }) } catch (_: Exception) { context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) } }
private fun startAsset(context: Context, fileName: String): ImageBitmap? = try { context.assets.open("icons/xp/$fileName").use { BitmapFactory.decodeStream(it)?.asImageBitmap() } } catch (_: Exception) { null }

@Composable private fun XpSystemMenuRow(context: Context, iconFile: String, label: String, bold: Boolean = false, arrow: Boolean = false, onClick: () -> Unit) {
    val icon = remember(iconFile) { startAsset(context, iconFile) }
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(29.dp)) else Box(Modifier.size(29.dp)); Spacer(Modifier.width(8.dp)); Text(label, color = Color(0xFF163C73), fontSize = 11.5.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)); if (arrow) Text("▶", fontSize = 8.sp, color = Color.Black)
    }
}
@Composable private fun XpFooterAction(context: Context, iconFile: String, label: String, onClick: () -> Unit) { val icon = remember(iconFile) { startAsset(context, iconFile) }; Row(Modifier.clickable { onClick() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(25.dp)); Spacer(Modifier.width(5.dp)); Text(label, color = Color.White, fontSize = 11.sp) } }
@Composable private fun XpGreenArrow() { Box(Modifier.size(20.dp).background(Color(0xFF34B23D), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun XpStartSeparator(right: Boolean = false) { Box(Modifier.fillMaxWidth().padding(horizontal = if (right) 9.dp else 12.dp).height(1.dp).background(if (right) Color(0xFFB4CCE7) else Color(0xFFD6D6D6))) }
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable private fun XpProgramRow(app: LaunchableApp, onClick: () -> Unit, onLongClick: () -> Unit) { Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 11.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { if (app.icon != null) Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(31.dp)) else Box(Modifier.size(31.dp)); Spacer(Modifier.width(9.dp)); Text(app.label, color = Color(0xFF1F1F1F), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun XpStartPopup(modifier: Modifier, title: String, content: @Composable ColumnScope.() -> Unit) { Column(modifier.width(292.dp).shadow(13.dp).background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(10.dp)) { Text(title, color = Color(0xFF003399), fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(7.dp)); content() } }
@Composable private fun XpPopupLink(label: String, onClick: () -> Unit) { Text(label, fontSize = 11.sp, color = Color(0xFF163C73), modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 6.dp, vertical = 7.dp)) }
