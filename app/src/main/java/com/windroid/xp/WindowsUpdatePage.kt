package com.windroid.xp

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class WindowsUpdateSection { HOME, HISTORY, SETTINGS }

@Composable
fun WindowsUpdatePage(
    status: String,
    progress: Int,
    availableVersion: String?,
    notes: String?,
    downloaded: Boolean,
    onCheckAgain: () -> Unit,
    onDownloadAndInstall: () -> Unit,
    onInstall: () -> Unit,
    onClose: () -> Unit
) {
    var section by remember { mutableStateOf(WindowsUpdateSection.HOME) }
    val blue = Color(0xFF4E7FD0)
    val darkBlue = Color(0xFF3A69B8)
    val paleBlue = Color(0xFFE7EEF9)
    val gold = Color(0xFFF7E7A4)
    val isDownloading = progress in 0..99
    val hasUpdate = availableVersion != null

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 440.dp, max = 610.dp)
            .background(Color.White)
            .border(1.dp, Color(0xFF9AA7B5))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFF7FBFF), Color(0xFFD7E4F8))))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⊞", fontSize = 34.sp, color = Color(0xFF2F67B2), fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("Windroid", fontSize = 27.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Windroid XP Update", fontSize = 11.sp, color = Color(0xFF4B6485))
                Text("Keep your computer up to date", fontSize = 9.sp, color = Color(0xFF71839A))
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(39.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF5B8DDB), darkBlue)))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Windows Update", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(29.dp)
                .background(Color(0xFFF0F0ED))
                .border(1.dp, Color(0xFFD0D0CA))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Windows Update Home", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B4B4B))
            Spacer(Modifier.width(16.dp))
            Text("Windroid XP", fontSize = 10.sp, color = Color(0xFF666666))
            Spacer(Modifier.width(16.dp))
            Text("Automatic Updates", fontSize = 10.sp, color = Color(0xFF666666))
        }

        Row(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .width(154.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFF5F5F5))
                    .border(1.dp, Color(0xFFE1E1E1))
                    .padding(horizontal = 13.dp, vertical = 15.dp)
            ) {
                UpdateNavLink("Windows Update Home", section == WindowsUpdateSection.HOME) { section = WindowsUpdateSection.HOME }
                Spacer(Modifier.height(22.dp))
                Text("Options", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                UpdateNavLink("Review your update history", section == WindowsUpdateSection.HISTORY) { section = WindowsUpdateSection.HISTORY }
                UpdateNavLink("Change settings", section == WindowsUpdateSection.SETTINGS) { section = WindowsUpdateSection.SETTINGS }
                Spacer(Modifier.height(7.dp))
                UpdateNavLink("Check for updates", false) { section = WindowsUpdateSection.HOME; onCheckAgain() }
            }

            Column(Modifier.weight(1f).fillMaxHeight().background(Color.White)) {
                when (section) {
                    WindowsUpdateSection.HOME -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(Brush.verticalGradient(listOf(Color(0xFFFFF8D5), gold)))
                                .padding(horizontal = 15.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                when {
                                    isDownloading -> "Downloading updates"
                                    downloaded -> "Ready to install"
                                    hasUpdate -> "Updates are available"
                                    status.contains("checking", ignoreCase = true) -> "Checking for the latest updates"
                                    status.contains("up to date", ignoreCase = true) -> "Your computer is up to date"
                                    else -> "Windows Update"
                                },
                                fontSize = 24.sp,
                                color = Color(0xFF393939)
                            )
                        }

                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 19.dp)
                        ) {
                            Text(status.ifBlank { "Check for updates to Windroid XP." }, fontSize = 12.sp, color = Color(0xFF333333))
                            availableVersion?.let {
                                Spacer(Modifier.height(7.dp))
                                Text("Available version: $it", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                            }
                            if (progress in 0..100) {
                                Spacer(Modifier.height(14.dp))
                                ClassicSegmentProgress(progress)
                                Spacer(Modifier.height(5.dp))
                                Text("$progress% complete", fontSize = 10.sp, color = Color(0xFF666666))
                            }
                            notes?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(14.dp))
                                Text("Update details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(it.take(600), fontSize = 10.sp, color = Color(0xFF555555), maxLines = 12, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(18.dp))
                            when {
                                downloaded -> ClassicWebButton("Install update") { onInstall() }
                                hasUpdate && !isDownloading -> ClassicWebButton("Download and install") { onDownloadAndInstall() }
                                !isDownloading -> ClassicWebButton("Check for updates") { onCheckAgain() }
                            }
                            Spacer(Modifier.height(18.dp))
                            Text("Windroid XP uses its built-in updater to download and install signed releases.", fontSize = 9.sp, color = Color(0xFF777777))
                        }
                    }

                    WindowsUpdateSection.HISTORY -> {
                        UpdateSubPage("Review your update history") {
                            Text("Current installed version", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(BuildConfig.VERSION_NAME, fontSize = 11.sp, color = Color(0xFF444444))
                            Spacer(Modifier.height(13.dp))
                            if (availableVersion != null) {
                                Text("Latest update found", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(availableVersion, fontSize = 11.sp, color = Color(0xFF444444))
                            } else {
                                Text("No newer update is currently queued for installation.", fontSize = 11.sp, color = Color(0xFF555555))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Full historical release tracking can be added later; this page currently shows the installed and detected release state.", fontSize = 9.sp, color = Color(0xFF777777))
                        }
                    }

                    WindowsUpdateSection.SETTINGS -> {
                        UpdateSubPage("Change settings") {
                            Text("Automatic update checks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Windroid XP currently checks for a newer release automatically after startup. Installation still requires your approval.", fontSize = 11.sp, color = Color(0xFF444444))
                            Spacer(Modifier.height(15.dp))
                            ClassicWebButton("Check now") { section = WindowsUpdateSection.HOME; onCheckAgain() }
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFDDE9FB), Color(0xFFB9D0F4))))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Windroid XP Update", fontSize = 9.sp, color = Color(0xFF446A9D))
            Spacer(Modifier.weight(1f))
            Text("Close", fontSize = 10.sp, color = Color(0xFF204F8A), modifier = Modifier.clickable { onClose() }.padding(8.dp))
        }
    }
}

@Composable
private fun UpdateNavLink(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 10.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) Color(0xFF222222) else Color(0xFF425B7E),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 5.dp)
    )
}

@Composable
private fun UpdateSubPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF8D5), Color(0xFFF7E7A4))))
                .padding(horizontal = 15.dp),
            contentAlignment = Alignment.CenterStart
        ) { Text(title, fontSize = 23.sp, color = Color(0xFF393939)) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), content = content)
    }
}

@Composable
private fun ClassicWebButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD7E5F6))))
            .border(1.dp, Color(0xFF5D718B))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF344A63)) }
}

@Composable
private fun ClassicSegmentProgress(progress: Int) {
    val clamped = progress.coerceIn(0, 100)
    val segments = 24
    val filled = ((clamped / 100f) * segments).toInt()
    Row(
        Modifier.fillMaxWidth().height(18.dp).background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(segments) { index ->
            Box(Modifier.weight(1f).fillMaxHeight().background(if (index < filled) Color(0xFF00A000) else Color.Transparent))
        }
    }
}
