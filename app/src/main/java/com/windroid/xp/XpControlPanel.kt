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
import androidx.compose.ui.unit.Dp
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

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val sidebarWidth = when {
                    maxWidth < 420.dp -> 118.dp
                    maxWidth < 700.dp -> 145.dp
                    else -> 185.dp
                }

                Row(Modifier.fillMaxSize()) {
                    ControlPanelSidebar(
                        width = sidebarWidth,
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
}

@Composable
private fun ExplorerMenuRow() {
    Row(
        Modifier.fillMaxWidth().height(28.dp).background(Color(0xFFF4F1E8)).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("File", "Edit", "View", "Favorites", "Tools", "Help").forEach { label ->
            Text(label, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun ExplorerToolbarRow() {
    Row(
        Modifier.fillMaxWidth().height(46.dp).background(Color(0xFFF1EEE4)).border(1.dp, Color(0xFFD0CCBF)).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("◀", color = Color(0xFFAAAAAA), fontSize = 22.sp)
        Spacer(Modifier.width(14.dp))
        Text("➜", color = Color(0xFF36A535), fontSize = 24.sp)
        Spacer(Modifier.width(16.dp))
        Text("🔍 Search", fontSize = 11.sp)
        Spacer(Modifier.width(16.dp))
        Text("📁 Folders", fontSize = 11.sp)
        Spacer(Modifier.width(16.dp))
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
    width: Dp,
    classicView: Boolean,
    onToggleClassic: () -> Unit,
    onWindowsUpdate: () -> Unit,
    onAbout: () -> Unit,
    onRestoreDefaults: () -> Unit
) {
    val compact = width <= 120.dp
    Column(
        Modifier.width(width).fillMaxHeight().background(Color(0xFF6F94DD)).padding(if (compact) 6.dp else 9.dp)
    ) {
        SidebarCard("Control Panel", compact) {
            SidebarLink(if (classicView) "Switch to Category View" else "Switch to Classic View", onToggleClassic, compact)
        }
        Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
        SidebarCard("See Also", compact) {
            SidebarLink("Windows Update", onWindowsUpdate, compact)
            SidebarLink("About Windroid XP", onAbout, compact)
            SidebarLink("Help and Support", onAbout, compact)
            SidebarLink("Restore Windroid Defaults", onRestoreDefaults, compact)
        }
    }
}

@Composable
private fun SidebarCard(title: String, compact: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFFF5F7FD)).border(1.dp, Color(0xFF8DA7D8))) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF2E69C7)).padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (compact) 10.sp else 11.sp)
            Spacer(Modifier.weight(1f))
            Text("⌃", color = Color.White, fontSize = 11.sp)
        }
        Column(Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 6.dp), content = content)
    }
}

@Composable
private fun SidebarLink(label: String, onClick: () -> Unit, compact: Boolean) {
    Text(
        label,
        color = Color(0xFF2456A5),
        fontSize = if (compact) 8.5.sp else 10.sp,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = if (compact) 4.dp else 5.dp)
    )
}

@Composable
private fun CategoryControlPanel(
    context: Context,
    onAppearance: () -> Unit,
    onUserAccounts: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 470.dp
        val paddingX = if (compact) 14.dp else 24.dp
        val iconSize = if (compact) 44 else 55
        val titleSize = if (compact) 23.sp else 31.sp

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = paddingX, top = 16.dp, end = paddingX, bottom = 18.dp)
        ) {
            Text("Pick a category", color = Color(0xFFD9E3FF), fontSize = titleSize, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(if (compact) 10.dp else 16.dp))

            if (compact) {
                CategoryColumn(context, onAppearance, onUserAccounts, iconSize, includeRightColumn = true)
            } else {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        CategoryLeftItems(context, onAppearance, iconSize)
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        CategoryRightItems(context, onUserAccounts, iconSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryColumn(context: Context, onAppearance: () -> Unit, onUserAccounts: () -> Unit, iconSize: Int, includeRightColumn: Boolean) {
    CategoryLeftItems(context, onAppearance, iconSize)
    if (includeRightColumn) CategoryRightItems(context, onUserAccounts, iconSize)
}

@Composable
private fun CategoryLeftItems(context: Context, onAppearance: () -> Unit, iconSize: Int) {
    CategoryItem(context, "Appearance.png", "Appearance and Themes", iconSize, onAppearance)
    CategoryItem(context, "Network Connections.png", "Network and Internet Connections", iconSize) {
        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }
    CategoryItem(context, "Change or Remove Programs.png", "Add or Remove Programs", iconSize) {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS))
    }
    CategoryItem(context, "Audio Devices.png", "Sounds, Speech, and Audio Devices", iconSize) {
        context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
    }
    CategoryItem(context, "Whistler - Performance.png", "Performance and Maintenance", iconSize) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

@Composable
private fun CategoryRightItems(context: Context, onUserAccounts: () -> Unit, iconSize: Int) {
    CategoryItem(context, "Printers and Faxes.png", "Printers and Other Hardware", iconSize) {
        context.startActivity(Intent(Settings.ACTION_PRINT_SETTINGS))
    }
    CategoryItem(context, "User Accounts.png", "User Accounts", iconSize, onUserAccounts)
    CategoryItem(context, "Date and Time.png", "Date, Time, Language, and Regional Options", iconSize) {
        context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS))
    }
    CategoryItem(context, "Accessibility.png", "Accessibility Options", iconSize) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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
            Triple("Change or Remove Programs.png", "Add or Remove Programs") { context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS)) },
            Triple("Network Connections.png", "Network Connections") { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) },
            Triple("User Accounts.png", "User Accounts", onUserAccounts),
            Triple("Windows Update.png", "Windows Update", onWindowsUpdate),
            Triple("Windroid logo.png", "About Windroid XP", onAbout)
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
private fun CategoryItem(context: Context, iconName: String, label: String, iconSize: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = if (iconSize <= 44) 7.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        XpControlIcon(context, iconName, iconSize)
        Spacer(Modifier.width(if (iconSize <= 44) 8.dp else 10.dp))
        Text(label, color = Color.White, fontSize = if (iconSize <= 44) 10.5.sp else 12.sp, fontWeight = FontWeight.Bold)
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
