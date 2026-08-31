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

enum class WindowsUpdateSection { HOME, HISTORY, SETTINGS, HIDDEN, HELP, ADMIN, VALIDATION, DEAD_END, CUSTOM }

@Composable
fun WindowsUpdatePage(status: String, progress: Int, availableVersion: String?, notes: String?, downloaded: Boolean, history: List<UpdateManager.UpdateHistoryItem> = emptyList(), historyLoading: Boolean = false, automaticChecks: Boolean = true, onAutomaticChecksChanged: (Boolean) -> Unit = {}, onCheckAgain: () -> Unit, onDownloadAndInstall: () -> Unit, onInstall: () -> Unit, onClose: () -> Unit) {
    var section by remember { mutableStateOf(WindowsUpdateSection.HOME) }
    var previousSection by remember { mutableStateOf(WindowsUpdateSection.HOME) }
    var deadEndTitle by remember { mutableStateOf("Information") }
    val isDownloading = progress in 0..99
    val hasUpdate = availableVersion != null
    fun navigate(target: WindowsUpdateSection) { if (target != section) { previousSection = section; section = target } }
    fun deadEnd(title: String) { deadEndTitle = title; navigate(WindowsUpdateSection.DEAD_END) }
    val address = when (section) {
        WindowsUpdateSection.HISTORY -> "http://windowsupdate.windroid.local/history.aspx"
        WindowsUpdateSection.SETTINGS -> "http://windowsupdate.windroid.local/settings.aspx"
        WindowsUpdateSection.HIDDEN -> "http://windowsupdate.windroid.local/hiddenupdates.aspx"
        WindowsUpdateSection.HELP -> "http://support.windroid.local/windowsupdate/"
        WindowsUpdateSection.ADMIN -> "http://windowsupdate.windroid.local/admin.aspx"
        WindowsUpdateSection.VALIDATION -> "http://windowsupdate.windroid.local/genuine/validate.aspx"
        WindowsUpdateSection.DEAD_END -> "http://windowsupdate.windroid.local/information.aspx"
        WindowsUpdateSection.CUSTOM -> "http://windowsupdate.windroid.local/custominstall.aspx"
        else -> "http://windowsupdate.windroid.local/v6/default.aspx?ln=en-us"
    }

    XpWindowShell(title = "Microsoft Windows Update - Microsoft Internet Explorer", initiallyMaximized = true, showMinimize = true, showMaximize = true, onMinimize = onClose, onClose = onClose) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF4F1E8))) {
            FakeIeMenu { label -> if (label == "Help") navigate(WindowsUpdateSection.HELP) else deadEnd("Internet Explorer $label") }
            FakeIeToolbar(onBack = { val old = section; section = previousSection; previousSection = old }, onHome = { navigate(WindowsUpdateSection.HOME) }, onSearch = { deadEnd("Search") }, onFavorites = { deadEnd("Favorites") })
            FakeAddressBar(address) { navigate(WindowsUpdateSection.HOME) }
            Column(Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
                SiteHeader(onDeadEnd = ::deadEnd, onValidation = { navigate(WindowsUpdateSection.VALIDATION) })
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    UpdateSidebar(section, onHome = { navigate(WindowsUpdateSection.HOME) }, onHistory = { navigate(WindowsUpdateSection.HISTORY) }, onHidden = { navigate(WindowsUpdateSection.HIDDEN) }, onSettings = { navigate(WindowsUpdateSection.SETTINGS) }, onHelp = { navigate(WindowsUpdateSection.HELP) }, onAdmin = { navigate(WindowsUpdateSection.ADMIN) })
                    Column(Modifier.weight(1f).fillMaxHeight().background(Color.White)) {
                        when (section) {
                            WindowsUpdateSection.HOME -> {
                                UpdatePageHeading(when { isDownloading -> "Downloading Updates"; downloaded -> "Ready to Install"; hasUpdate -> "Choose how to install updates"; status.contains("checking", true) -> "Checking for the latest updates"; status.contains("up to date", true) -> "Express Results"; else -> "Windows Update" })
                                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                                    Text(status.ifBlank { "Keep your computer up to date. Check for the latest Windroid XP updates." }, fontSize = 12.sp)
                                    availableVersion?.let { Spacer(Modifier.height(8.dp)); Text("Windroid XP $it", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF174A8B)) }
                                    if (progress in 0..100) { Spacer(Modifier.height(14.dp)); ClassicSegmentProgress(progress); Spacer(Modifier.height(5.dp)); Text("Installing update $progress%", fontSize = 10.sp, color = Color.Gray) }
                                    if (hasUpdate) {
                                        Spacer(Modifier.height(15.dp)); Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(cleanReleaseNotes(notes).ifBlank { "Detailed information was not recorded for this update." }, fontSize = 10.sp, color = Color(0xFF444444), maxLines = 20, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(20.dp))
                                        Text("Choose an installation method:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            ClassicWebButton("Express") { if (downloaded) onInstall() else onDownloadAndInstall() }
                                            Spacer(Modifier.width(10.dp))
                                            ClassicWebButton("Custom") { navigate(WindowsUpdateSection.CUSTOM) }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("Express installs the recommended update immediately. Custom lets you review additional installation options.", fontSize = 9.sp, color = Color(0xFF666666))
                                    }
                                    Spacer(Modifier.height(20.dp)); WebLink("Why should I keep my computer updated?") { navigate(WindowsUpdateSection.HELP) }; WebLink("Is my copy of Windroid genuine?") { navigate(WindowsUpdateSection.VALIDATION) }
                                }
                            }
                            WindowsUpdateSection.CUSTOM -> CustomUpdateWizard(
                                versionName = availableVersion ?: BuildConfig.VERSION_NAME,
                                onCancel = { navigate(WindowsUpdateSection.HOME) },
                                onInstall = { if (downloaded) onInstall() else onDownloadAndInstall() }
                            )
                            WindowsUpdateSection.HISTORY -> UpdateSubPage("Review your update history") {
                                Text("The updates listed below have been installed or recently published for Windroid XP.", fontSize = 11.sp); Spacer(Modifier.height(12.dp)); Text("Current installed version: ${BuildConfig.VERSION_NAME}", fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
                                when { historyLoading -> Text("Please wait while Windows Update retrieves your history...", fontSize = 11.sp); history.isEmpty() -> Text("No update history is available.", fontSize = 11.sp); else -> history.take(5).forEachIndexed { i, item -> UpdateHistoryEntry(item); if (i < history.take(5).lastIndex) { Spacer(Modifier.height(8.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD6D6D6))); Spacer(Modifier.height(8.dp)) } } }
                            }
                            WindowsUpdateSection.SETTINGS -> UpdateSubPage("Change settings") {
                                Text("Choose how Windows Update works on this computer.", fontSize = 11.sp); Spacer(Modifier.height(15.dp)); Text("Automatic Updates", fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp))
                                Row(Modifier.fillMaxWidth().clickable { onAutomaticChecksChanged(!automaticChecks) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { ClassicCheckBox(automaticChecks); Spacer(Modifier.width(9.dp)); Text("Automatically check for Windroid XP updates", fontSize = 11.sp) }
                                Text(if (automaticChecks) "Recommended. Windroid XP checks for newer releases automatically. You decide when to install them." else "Automatic Updates are turned off. You can still visit Windows Update and check manually.", fontSize = 10.sp, color = Color(0xFF555555)); Spacer(Modifier.height(18.dp)); ClassicWebButton("Check Now") { navigate(WindowsUpdateSection.HOME); onCheckAgain() }
                            }
                            WindowsUpdateSection.HIDDEN -> UpdateSubPage("Restore hidden updates") { Text("There are currently no hidden updates to restore.", fontSize = 12.sp); Spacer(Modifier.height(8.dp)); Text("When you hide an update, it will appear here so you can choose to offer it again later.", fontSize = 10.sp, color = Color(0xFF555555)) }
                            WindowsUpdateSection.HELP -> UpdateSubPage("Help and Support") { Text("Windows Update Troubleshooter", fontSize = 13.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); WebLink("Problems installing an update") { deadEnd("Problems installing an update") }; WebLink("Windows Update cannot connect") { deadEnd("Windows Update cannot connect") }; WebLink("Understanding Automatic Updates") { navigate(WindowsUpdateSection.SETTINGS) }; WebLink("View installation history") { navigate(WindowsUpdateSection.HISTORY) }; Spacer(Modifier.height(18.dp)); Text("If Windows Update continues to experience problems, try again later. This usually fixes computers because computers are afraid of persistence.", fontSize = 10.sp, color = Color(0xFF555555)) }
                            WindowsUpdateSection.ADMIN -> UpdateSubPage("Use administrator options") { Text("Administrator Options", fontSize = 13.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); WebLink("Windows Update Catalog") { deadEnd("Windows Update Catalog") }; WebLink("Update multiple computers") { deadEnd("Update multiple computers") }; WebLink("Automatic Updates policy") { navigate(WindowsUpdateSection.SETTINGS) }; WebLink("Network installation options") { deadEnd("Network installation options") } }
                            WindowsUpdateSection.VALIDATION -> UpdateSubPage("Genuine Windroid Validation") { Text("Validation Complete", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF356A12)); Spacer(Modifier.height(10.dp)); Text("Your copy of Windroid XP is genuine. Probably.", fontSize = 12.sp); Spacer(Modifier.height(7.dp)); Text("Genuine Windroid software offers greater reliability, faster access to questionable nostalgia, and an overall richer 2005 computing experience.", fontSize = 10.sp, color = Color(0xFF555555)); Spacer(Modifier.height(18.dp)); ClassicWebButton("Continue") { navigate(WindowsUpdateSection.HOME) } }
                            WindowsUpdateSection.DEAD_END -> UpdateSubPage(deadEndTitle) { Text("[Error number: 0x8024WXP1]", fontSize = 10.sp, color = Color.Gray); Spacer(Modifier.height(12.dp)); Row(verticalAlignment = Alignment.Top) { Text("●", color = Color.Red, fontSize = 18.sp); Spacer(Modifier.width(8.dp)); Text("The website has encountered a problem and cannot display the page you are trying to view.", fontSize = 11.sp) }; Spacer(Modifier.height(15.dp)); Text("For self-help options:", fontSize = 11.sp); WebLink("Frequently Asked Questions") { navigate(WindowsUpdateSection.HELP) }; WebLink("Find Solutions") { navigate(WindowsUpdateSection.HELP) }; WebLink("Windows Update Newsgroup") { deadEnd("Windows Update Newsgroup") } }
                        }
                    }
                }
                SiteFooter(onPrivacy = { deadEnd("Windows Update Privacy Statement") }, onTerms = { deadEnd("Terms of Use") })
            }
            FakeIeStatus(if (isDownloading) "Downloading update..." else if (status.contains("checking", true)) "Opening page..." else "Done")
        }
    }
}

@Composable private fun FakeIeMenu(onClick: (String) -> Unit) { Row(Modifier.fillMaxWidth().height(27.dp).background(Color(0xFFF4F1E8)).border(1.dp, Color(0xFFD0CDBF)).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) { listOf("File", "Edit", "View", "Favorites", "Tools", "Help").forEach { Text(it, fontSize = 11.sp, modifier = Modifier.clickable { onClick(it) }.padding(horizontal = 8.dp, vertical = 5.dp)) } } }
@Composable private fun FakeIeToolbar(onBack: () -> Unit, onHome: () -> Unit, onSearch: () -> Unit, onFavorites: () -> Unit) { Row(Modifier.fillMaxWidth().height(43.dp).background(Color(0xFFF1EEE4)).border(1.dp, Color(0xFFD1CCBE)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { ToolbarItem("◀", "Back", onBack); Spacer(Modifier.width(10.dp)); Text("▶", color = Color(0xFFAAAAAA), fontSize = 20.sp); Spacer(Modifier.width(13.dp)); ToolbarItem("⌂", "Home", onHome); Spacer(Modifier.width(14.dp)); ToolbarItem("⌕", "Search", onSearch); Spacer(Modifier.width(14.dp)); ToolbarItem("★", "Favorites", onFavorites) } }
@Composable private fun ToolbarItem(icon: String, label: String, action: () -> Unit) { Row(Modifier.clickable { action() }.padding(3.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 20.sp, color = Color(0xFF278A32)); Spacer(Modifier.width(3.dp)); Text(label, fontSize = 10.sp) } }
@Composable private fun FakeAddressBar(address: String, onGo: () -> Unit) { Row(Modifier.fillMaxWidth().height(30.dp).background(Color(0xFFF4F1E8)).border(1.dp, Color(0xFFC8C5BA)).padding(3.dp), verticalAlignment = Alignment.CenterVertically) { Text("Address", fontSize = 9.sp, color = Color.Gray); Spacer(Modifier.width(5.dp)); Box(Modifier.weight(1f).fillMaxHeight().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(horizontal = 5.dp), contentAlignment = Alignment.CenterStart) { Text(address, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Spacer(Modifier.width(4.dp)); Box(Modifier.background(Color(0xFF3BA53B)).border(1.dp, Color(0xFF247424)).clickable { onGo() }.padding(horizontal = 7.dp, vertical = 3.dp)) { Text("➜ Go", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun SiteHeader(onDeadEnd: (String) -> Unit, onValidation: () -> Unit) { Column { Row(Modifier.fillMaxWidth().height(54.dp).background(Brush.horizontalGradient(listOf(Color.White, Color(0xFFC5D6F3), Color(0xFF7297DB)))).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text("▣", fontSize = 27.sp, color = Color(0xFF1D63B7)); Spacer(Modifier.width(5.dp)); Text("Windows", fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Column(horizontalAlignment = Alignment.End) { Row { WebLink("Microsoft.com Home") { onDeadEnd("Microsoft.com Home") }; Text("  |  ", fontSize = 9.sp); WebLink("Site Map") { onDeadEnd("Site Map") } }; Text("Search Microsoft.com for:", fontSize = 8.sp) } }; Box(Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF4E78C8)).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) { Text("Windows Update", fontSize = 20.sp, color = Color.White) }; Row(Modifier.fillMaxWidth().height(29.dp).background(Color(0xFFF3F3F1)).border(1.dp, Color(0xFFD1D1CF)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) { listOf("Windows Family", "Windows Marketplace", "Office Family", "Microsoft Update").forEachIndexed { i, s -> if (i > 0) Text(" | ", color = Color.Gray); Text(s, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF444444), modifier = Modifier.clickable { if (s == "Microsoft Update") onValidation() else onDeadEnd(s) }.padding(4.dp)) } } } }
@Composable private fun UpdateSidebar(section: WindowsUpdateSection, onHome: () -> Unit, onHistory: () -> Unit, onHidden: () -> Unit, onSettings: () -> Unit, onHelp: () -> Unit, onAdmin: () -> Unit) { Column(Modifier.width(178.dp).fillMaxHeight().background(Color(0xFFF2F2F2)).border(1.dp, Color(0xFFD0D0D0)).padding(15.dp)) { UpdateNavLink("Windows Update Home", section == WindowsUpdateSection.HOME, onHome); Spacer(Modifier.height(25.dp)); Text("Options", fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); UpdateNavLink("Review your update history", section == WindowsUpdateSection.HISTORY, onHistory); UpdateNavLink("Restore hidden updates", section == WindowsUpdateSection.HIDDEN, onHidden); UpdateNavLink("Change settings", section == WindowsUpdateSection.SETTINGS, onSettings); UpdateNavLink("Get help and support", section == WindowsUpdateSection.HELP, onHelp); UpdateNavLink("Use administrator options", section == WindowsUpdateSection.ADMIN, onAdmin) } }
@Composable private fun SiteFooter(onPrivacy: () -> Unit, onTerms: () -> Unit) { Row(Modifier.fillMaxWidth().height(48.dp).background(Brush.horizontalGradient(listOf(Color.White, Color(0xFFB6CBF0), Color(0xFF7194D5)))).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column { WebLink("Windows Update Privacy Statement", onPrivacy); Row { Text("©2005 Windroid Corporation. All rights reserved.  ", fontSize = 8.sp); WebLink("Terms of Use", onTerms) } }; Spacer(Modifier.weight(1f)); Text("Windroid", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White) } }
@Composable private fun FakeIeStatus(text: String) { Row(Modifier.fillMaxWidth().height(24.dp).background(Color(0xFFF1EFE7)).border(1.dp, Color(0xFFC8C5BA)).padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) { Text("▣  $text", fontSize = 9.sp); Spacer(Modifier.weight(1f)); Text("🌐  Internet", fontSize = 9.sp) } }
@Composable private fun WebLink(label: String, onClick: () -> Unit) { Text(label, color = Color(0xFF174A8B), fontSize = 10.sp, modifier = Modifier.clickable { onClick() }.padding(vertical = 2.dp)) }
@Composable private fun ClassicCheckBox(checked: Boolean) { Box(Modifier.size(17.dp).background(Color.White).border(1.dp, Color(0xFF777777)), contentAlignment = Alignment.Center) { if (checked) Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3F80)) } }
@Composable private fun UpdatePageHeading(title: String) { Box(Modifier.fillMaxWidth().height(55.dp).background(Brush.horizontalGradient(listOf(Color(0xFFFFF7CB), Color(0xFFFFE99C), Color(0xFFF3B35C)))).padding(horizontal = 17.dp), contentAlignment = Alignment.CenterStart) { Text(title, fontSize = 22.sp, color = Color(0xFF3A3A3A)) } }
@Composable private fun UpdateHistoryEntry(item: UpdateManager.UpdateHistoryItem) { Text("Windroid XP ${item.versionName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF174A8B)); Text(formatReleaseDate(item.publishedAt), fontSize = 9.sp, color = Color.Gray); Spacer(Modifier.height(4.dp)); Text(cleanReleaseNotes(item.notes).ifBlank { "Detailed patch notes were not recorded for this build." }, fontSize = 10.sp, color = Color(0xFF444444), maxLines = 15, overflow = TextOverflow.Ellipsis) }
private fun formatReleaseDate(value: String): String { if (value.length < 10) return "Date unavailable"; val p = value.take(10).split('-'); if (p.size != 3) return value.take(10); val m = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec").getOrElse(p[1].toIntOrNull() ?: 0) { p[1] }; return "$m ${p[2].trimStart('0').ifBlank { "0" }}, ${p[0]}" }
private fun cleanReleaseNotes(raw: String?): String { if (raw.isNullOrBlank()) return ""; return raw.lineSequence().map { it.trim().removePrefix("### ").removePrefix("## ").removePrefix("# ").replace("**", "").replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1") }.filter { it.isNotBlank() && !it.startsWith("Full Changelog", true) }.take(22).joinToString("\n").take(1800) }
@Composable private fun UpdateNavLink(label: String, selected: Boolean, onClick: () -> Unit) { Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Color(0xFF222222) else Color(0xFF174A8B), modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 5.dp)) }
@Composable private fun UpdateSubPage(title: String, content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize()) { UpdatePageHeading(title); Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), content = content) } }
@Composable private fun ClassicWebButton(label: String, onClick: () -> Unit) { Box(Modifier.background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD7E5F6)))).border(1.dp, Color(0xFF5D718B)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 5.dp)) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF344A63)) } }
@Composable private fun ClassicSegmentProgress(progress: Int) { val filled = ((progress.coerceIn(0, 100) / 100f) * 24).toInt(); Row(Modifier.fillMaxWidth().height(18.dp).background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(24) { i -> Box(Modifier.weight(1f).fillMaxHeight().background(if (i < filled) Color(0xFF00A000) else Color.Transparent)) } } }
