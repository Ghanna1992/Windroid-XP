package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun XpControlPanel(
    onClose: () -> Unit,
    onAppearance: () -> Unit,
    onUserAccounts: () -> Unit,
    onWindowsUpdate: () -> Unit,
    onAbout: () -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var classicView by remember { mutableStateOf(false) }

    XpWindowShell(
        title = "Control Panel",
        modifier = modifier,
        initiallyMaximized = true,
        showMinimize = true,
        showMaximize = true,
        onMinimize = onClose,
        onClose = onClose
    ) {
        Column(Modifier.fillMaxSize().background(Color(0xFFECE9D8))) {
            ExplorerMenuRow()
            ExplorerToolbarRow()
            AddressRow()

            Row(Modifier.weight(1f).fillMaxWidth()) {
                ControlPanelSidebar(
                    classicView = classicView,
                    onToggleClassic = { classicView = !classicView },
                    onWindowsUpdate = onWindowsUpdate,
                    onAbout = onAbout,
                    onRestoreDefaults = onRestoreDefaults
                )

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF6375D6))
                ) {
                    if (classicView) {
                        ClassicControlPanel(context, onAppearance, onUserAccounts, onWindowsUpdate, onAbout)
                    } else {
                        CategoryControlPanel(context, onAppearance, onUserAccounts)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorerMenuRow() {
    Row(
        Modifier.fillMaxWidth().height(28.dp).background(Color(0xFFF4F1E8)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("File", "Edit", "View", "Favorites", "Tools", "Help").forEach { label ->
            Text(label, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun ExplorerToolbarRow() {
    Row(
        Modifier.fillMaxWidth().height(46.dp).background(Color(0xFFF1EEE4)).border(1.dp, Color(0xFFD0CCBF)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("◀", color = Color(0xFFAAAAAA), fontSize = 22.sp)
        Spacer(Modifier.width(18.dp))
        Text("➜", color = Color(0xFF36A535), fontSize = 24.sp)
        Spacer(Modifier.width(20.dp))
        Text("🔍 Search", fontSize = 11.sp)
        Spacer(Modifier.width(20.dp))
        Text("📁 Folders", fontSize = 11.sp)
        Spacer(Modifier.width(20.dp))
        Text("▦", fontSize = 19.sp)
    }
}

@Composable
private fun AddressRow() {
    Row(
        Modifier.fillMaxWidth().height(31.dp).background(Color(0xFFF4F1E8)).border(1.dp, Color(0xFFC8C5BA)).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Address", fontSize = 9.sp, color = Color.Gray)
        Spacer(Modifier.width(6.dp))
        Row(
            Modifier.weight(1f).fillMaxHeight().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("▣", color = Color(0xFF2B69C7), fontSize = 13.sp)
            Spacer(Modifier.width(5.dp))
            Text("Control Panel", fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text("⌄", color = Color(0xFF4775B9), fontSize = 12.sp)
        }
        Spacer(Modifier.width(4.dp))
        Text("➜ Go", color = Color(0xFF267629), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ControlPanelSidebar(
    classicView: Boolean,
    onToggleClassic: () -> Unit,
    onWindowsUpdate: () -> Unit,
    onAbout: () -> Unit,
    onRestoreDefaults: () -> Unit
) {
    Column(
        Modifier.width(185.dp).fillMaxHeight().background(Color(0xFF6F94DD)).padding(10.dp)
    ) {
        SidebarCard("Control Panel") {
            SidebarLink(if (classicView) "Switch to Category View" else "Switch to Classic View", onToggleClassic)
        }
        Spacer(Modifier.height(12.dp))
        SidebarCard("See Also") {
            SidebarLink("Windows Update", onWindowsUpdate)
            SidebarLink("About Windroid XP", onAbout)
            SidebarLink("Help and Support", onAbout)
            SidebarLink("Restore Windroid Defaults", onRestoreDefaults)
        }
    }
}

@Composable
private fun SidebarCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFF5F7FD)).border(1.dp, Color(0xFF8DA7D8))) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF2E69C7)).padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("⌃", color = Color.White, fontSize = 12.sp)
        }
        Column(Modifier.padding(9.dp), content = content)
    }
}

@Composable
private fun SidebarLink(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color(0xFF2456A5),
        fontSize = 10.sp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 5.dp)
    )
}

@Composable
private fun CategoryControlPanel(
    context: Context,
    onAppearance: () -> Unit,
    onUserAccounts: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 26.dp, top = 20.dp, end = 22.dp, bottom = 20.dp)
    ) {
        Text("Pick a category", color = Color(0xFFD9E3FF), fontSize = 31.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                CategoryItem(context, "Appearance.png", "Appearance and Themes", onAppearance)
                CategoryItem(context, "Network Connections.png", "Network and Internet Connections") {
                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                }
                CategoryItem(context, "Add or Remove Programs.png", "Add or Remove Programs") {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
                }
                CategoryItem(context, "Sounds and Audio Devices.png", "Sounds, Speech, and Audio Devices") {
                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                }
                CategoryItem(context, "Performance and Maintenance.png", "Performance and Maintenance") {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                CategoryItem(context, "Printers and Faxes.png", "Printers and Other Hardware") {
                    context.startActivity(Intent(Settings.ACTION_PRINT_SETTINGS))
                }
                CategoryItem(context, "User Accounts.png", "User Accounts", onUserAccounts)
                CategoryItem(context, "Date and Time.png", "Date, Time, Language, and Regional Options") {
                    context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
                }
                CategoryItem(context, "Accessibility.png", "Accessibility Options") {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
    }
}

@Composable
private fun ClassicControlPanel(
    context: Context,
    onAppearance: () -> Unit,
    onUserAccounts: () -> Unit,
    onWindowsUpdate: () -> Unit,
    onAbout: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("Control Panel", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF174A8B))
        Spacer(Modifier.height(12.dp))
        val items = listOf(
            Triple("Appearance.png", "Appearance and Themes", onAppearance),
            Triple("Add or Remove Programs.png", "Add or Remove Programs") { context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS)) },
            Triple("Network Connections.png", "Network Connections") { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) },
            Triple("User Accounts.png", "User Accounts", onUserAccounts),
            Triple("Windows Update.png", "Windows Update", onWindowsUpdate),
            Triple("My Computer.png", "About Windroid XP", onAbout)
        )
        items.forEach { (icon, label, action) ->
            Row(Modifier.fillMaxWidth().clickable { action() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                XpControlIcon(context, icon, 32)
                Spacer(Modifier.width(10.dp))
                Text(label, fontSize = 12.sp, color = Color(0xFF174A8B))
            }
        }
    }
}

@Composable
private fun CategoryItem(context: Context, iconName: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XpControlIcon(context, iconName, 55)
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun XpControlIcon(context: Context, name: String, size: Int) {
    val image = remember(name) { loadControlPanelIcon(context, name) }
    if (image != null) {
        Image(image, contentDescription = null, modifier = Modifier.size(size.dp), contentScale = ContentScale.Fit)
    } else {
        Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
            Text("▣", color = Color.White, fontSize = (size / 2).sp)
        }
    }
}

private fun loadControlPanelIcon(context: Context, name: String): ImageBitmap? = try {
    context.assets.open("icons/xp/$name").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
} catch (_: Exception) {
    null
}
