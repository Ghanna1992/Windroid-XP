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
    history: List<UpdateManager.UpdateHistoryItem> = emptyList(),
    historyLoading: Boolean = false,
    automaticChecks: Boolean = true,
    onAutomaticChecksChanged: (Boolean) -> Unit = {},
    onCheckAgain: () -> Unit,
    onDownloadAndInstall: () -> Unit,
    onInstall: () -> Unit,
    onClose: () -> Unit
) {
    var section by remember { mutableStateOf(WindowsUpdateSection.HOME) }
    val darkBlue = Color(0xFF3A69B8)
    val gold = Color(0xFFF7E7A4)
    val isDownloading = progress in 0..99
    val hasUpdate = availableVersion != null

    XpWindowShell(
        title = "Windows Update",
        modifier = Modifier,
        initiallyMaximized = true,
        showMinimize = true,
        showMaximize = true,
        onMinimize = onClose,
        onClose = onClose
    ) {
        Column(Modifier.fillMaxSize().background(Color.White)) {
            Row(Modifier.fillMaxWidth().height(58.dp).background(Brush.verticalGradient(listOf(Color(0xFFF7FBFF), Color(0xFFD7E4F8)))).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⊞", fontSize = 30.sp, color = Color(0xFF2F67B2), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("Windroid", fontSize = 24.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Windroid XP Update", fontSize = 11.sp, color = Color(0xFF4B6485))
                    Text("Keep your computer up to date", fontSize = 9.sp, color = Color(0xFF71839A))
                }
            }

            Box(Modifier.fillMaxWidth().height(36.dp).background(Brush.verticalGradient(listOf(Color(0xFF5B8DDB), darkBlue))).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                Text("Windows Update", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }

            Row(Modifier.fillMaxWidth().height(27.dp).background(Color(0xFFF0F0ED)).border(1.dp, Color(0xFFD0D0CA)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Windows Update Home", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B4B4B))
                Spacer(Modifier.width(16.dp))
                Text("Windroid XP", fontSize = 10.sp, color = Color(0xFF666666))
                Spacer(Modifier.width(16.dp))
                Text("Automatic Updates", fontSize = 10.sp, color = Color(0xFF666666))
            }

            Row(Modifier.weight(1f).fillMaxWidth()) {
                Column(Modifier.width(154.dp).fillMaxHeight().background(Color(0xFFF5F5F5)).border(1.dp, Color(0xFFE1E1E1)).padding(horizontal = 13.dp, vertical = 15.dp)) {
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
                            UpdatePageHeading(
                                when {
                                    isDownloading -> "Downloading updates"
                                    downloaded -> "Ready to install"
                                    hasUpdate -> "Updates are available"
                                    status.contains("checking", true) -> "Checking for the latest updates"
                                    status.contains("up to date", true) -> "Your computer is up to date"
                                    else -> "Windows Update"
                                }, gold
                            )
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 19.dp)) {
                                Text(status.ifBlank { "Check for updates to Windroid XP." }, fontSize = 12.sp, color = Color(0xFF333333))
                                availableVersion?.let { Spacer(Modifier.height(7.dp)); Text("Available version: $it", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                if (progress in 0..100) {
                                    Spacer(Modifier.height(14.dp)); ClassicSegmentProgress(progress); Spacer(Modifier.height(5.dp)); Text("$progress% complete", fontSize = 10.sp, color = Color(0xFF666666))
                                }
                                if (hasUpdate) {
                                    Spacer(Modifier.height(14.dp)); Text("Patch notes", fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp))
                                    Text(cleanReleaseNotes(notes).ifBlank { "This update contains maintenance and compatibility improvements." }, fontSize = 10.sp, color = Color(0xFF555555), maxLines = 18, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.height(18.dp))
                                when {
                                    downloaded -> ClassicWebButton("Install update") { onInstall() }
                                    hasUpdate && !isDownloading -> ClassicWebButton("Download and install") { onDownloadAndInstall() }
                                    !isDownloading -> ClassicWebButton("Check for updates") { onCheckAgain() }
                                }
                            }
                        }

                        WindowsUpdateSection.HISTORY -> UpdateSubPage("Review your update history") {
                            Text("Current installed version: ${BuildConfig.VERSION_NAME}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            when {
                                historyLoading -> Text("Loading recent updates...", fontSize = 11.sp, color = Color(0xFF555555))
                                history.isEmpty() -> Text("Update history could not be loaded. Check your connection and try again later.", fontSize = 11.sp, color = Color(0xFF555555))
                                else -> {
                                    Text("Recent patches", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(7.dp))
                                    history.take(5).forEachIndexed { index, item ->
                                        UpdateHistoryEntry(item)
                                        if (index != history.take(5).lastIndex) {
                                            Spacer(Modifier.height(9.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE0E0E0))); Spacer(Modifier.height(9.dp))
                                        }
                                    }
                                }
                            }
                        }

                        WindowsUpdateSection.SETTINGS -> UpdateSubPage("Change settings") {
                            Text("Automatic update checks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth().clickable { onAutomaticChecksChanged(!automaticChecks) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                ClassicCheckBox(automaticChecks)
                                Spacer(Modifier.width(9.dp))
                                Text("Automatically check for Windroid XP updates", fontSize = 11.sp, color = Color(0xFF333333))
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                if (automaticChecks) "Windroid XP will check for a newer release when Windows Update starts. You still choose when to download and install it."
                                else "Automatic checks are off. Windroid XP will not look for updates automatically. You can still use Check for updates whenever you want.",
                                fontSize = 10.sp,
                                color = Color(0xFF555555)
                            )
                            Spacer(Modifier.height(18.dp))
                            ClassicWebButton("Check now") { section = WindowsUpdateSection.HOME; onCheckAgain() }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().height(34.dp).background(Brush.verticalGradient(listOf(Color(0xFFDDE9FB), Color(0xFFB9D0F4)))).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Windroid XP Update", fontSize = 9.sp, color = Color(0xFF446A9D))
                Spacer(Modifier.weight(1f))
                Text("Close", fontSize = 10.sp, color = Color(0xFF204F8A), modifier = Modifier.clickable { onClose() }.padding(8.dp))
            }
        }
    }
}

@Composable private fun ClassicCheckBox(checked: Boolean) {
    Box(Modifier.size(17.dp).background(Color.White).border(1.dp, Color(0xFF777777)), contentAlignment = Alignment.Center) {
        if (checked) Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3F80))
    }
}

@Composable private fun UpdatePageHeading(title: String, gold: Color) {
    Box(Modifier.fillMaxWidth().height(52.dp).background(Brush.verticalGradient(listOf(Color(0xFFFFF8D5), gold))).padding(horizontal = 15.dp), contentAlignment = Alignment.CenterStart) {
        Text(title, fontSize = 23.sp, color = Color(0xFF393939))
    }
}

@Composable private fun UpdateHistoryEntry(item: UpdateManager.UpdateHistoryItem) {
    Text("Windroid XP ${item.versionName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF244C80))
    Text(formatReleaseDate(item.publishedAt), fontSize = 9.sp, color = Color(0xFF777777))
    Spacer(Modifier.height(4.dp))
    Text(cleanReleaseNotes(item.notes).ifBlank { "Maintenance and compatibility improvements. Detailed patch notes were not recorded for this build." }, fontSize = 10.sp, color = Color(0xFF4E4E4E), maxLines = 12, overflow = TextOverflow.Ellipsis)
}

private fun formatReleaseDate(value: String): String {
    if (value.length < 10) return "Date unavailable"
    val parts = value.take(10).split('-')
    if (parts.size != 3) return value.take(10)
    val month = when (parts[1]) {
        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"; "05" -> "May"; "06" -> "Jun"
        "07" -> "Jul"; "08" -> "Aug"; "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"; else -> parts[1]
    }
    return "$month ${parts[2].trimStart('0').ifBlank { "0" }}, ${parts[0]}"
}

private fun cleanReleaseNotes(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return raw.lineSequence()
        .map { line -> line.trim().removePrefix("### ").removePrefix("## ").removePrefix("# ").replace("**", "").replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1") }
        .filter { it.isNotBlank() && !it.startsWith("Full Changelog", true) }
        .take(18)
        .joinToString("\n")
        .take(1400)
}

@Composable private fun UpdateNavLink(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Color(0xFF222222) else Color(0xFF425B7E), modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 5.dp))
}

@Composable private fun UpdateSubPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        UpdatePageHeading(title, Color(0xFFF7E7A4))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), content = content)
    }
}

@Composable private fun ClassicWebButton(label: String, onClick: () -> Unit) {
    Box(Modifier.background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD7E5F6)))).border(1.dp, Color(0xFF5D718B)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF344A63))
    }
}

@Composable private fun ClassicSegmentProgress(progress: Int) {
    val clamped = progress.coerceIn(0, 100)
    val segments = 24
    val filled = ((clamped / 100f) * segments).toInt()
    Row(Modifier.fillMaxWidth().height(18.dp).background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(segments) { index ->
            Box(Modifier.weight(1f).fillMaxHeight().background(if (index < filled) Color(0xFF00A000) else Color.Transparent))
        }
    }
}
