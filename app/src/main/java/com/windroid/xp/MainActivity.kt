package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WindroidDesktop(this) }
    }
}

data class LaunchableApp(val label: String, val packageName: String)

private fun installedApps(context: Context): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return context.packageManager.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map { LaunchableApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun launchApp(context: Context, app: LaunchableApp) {
    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(intent)
    }
}

@Composable
fun WindroidDesktop(context: Context) {
    var startOpen by remember { mutableStateOf(false) }
    var computerOpen by remember { mutableStateOf(false) }
    val apps = remember { installedApps(context) }
    val launchedApps = remember { mutableStateListOf<LaunchableApp>() }

    var updateWindowOpen by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var updateStatus by remember { mutableStateOf("") }
    var downloadedUpdate by remember { mutableStateOf<File?>(null) }
    val updateScope = rememberCoroutineScope()

    fun openAndroidApp(app: LaunchableApp) {
        launchedApps.removeAll { it.packageName == app.packageName }
        launchedApps.add(0, app)
        startOpen = false
        computerOpen = false
        launchApp(context, app)
    }

    fun checkForUpdates(manual: Boolean) {
        startOpen = false
        updateStatus = "Checking for updates..."
        if (manual) updateWindowOpen = true
        updateScope.launch {
            val found = UpdateManager.checkForUpdate()
            updateInfo = found
            downloadedUpdate = null
            if (found != null) {
                updateStatus = "Windroid XP ${found.versionName} is ready."
                updateWindowOpen = true
            } else if (manual) {
                updateStatus = "Your computer is up to date."
                updateWindowOpen = true
            } else {
                updateWindowOpen = false
            }
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        val found = UpdateManager.checkForUpdate()
        if (found != null) {
            updateInfo = found
            updateStatus = "Windroid XP ${found.versionName} is ready."
            updateWindowOpen = true
        }
    }

    val xpBlue = Color(0xFF245EDB)
    val taskbarHeight = 43.dp

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF2B83D5), Color(0xFF6AB7E9), Color(0xFF8FD0F2), Color(0xFF58A947)))
        ).navigationBarsPadding()
    ) {
        Column(
            Modifier.fillMaxHeight().padding(start = 10.dp, top = 12.dp, bottom = taskbarHeight + 8.dp),
            verticalArrangement = Arrangement.spacedBy(17.dp)
        ) {
            DesktopIcon("🖥️", "My Computer") { computerOpen = true; startOpen = false }
            DesktopIcon("📁", "My Documents") { startOpen = false }
            DesktopIcon("🌐", "Internet Explorer") {
                startOpen = false
                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")))
            }
            DesktopIcon("🗑️", "Recycle Bin") { startOpen = false }
        }

        if (computerOpen) {
            XPWindow("My Computer", Modifier.align(Alignment.Center)) {
                Text("Files Stored on This Computer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text("📱  Android Device", fontSize = 14.sp)
                Spacer(Modifier.height(5.dp))
                Text("📁  Internal Storage", fontSize = 14.sp)
                Spacer(Modifier.height(14.dp))
                Text("⚙️  Control Panel", color = Color(0xFF003399), fontSize = 14.sp, modifier = Modifier.clickable {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                })
                Spacer(Modifier.height(18.dp))
                Text("Close", color = Color(0xFF003399), modifier = Modifier.clickable { computerOpen = false })
            }
        }

        if (updateWindowOpen) {
            XPWindow("Windows Update", Modifier.align(Alignment.Center)) {
                Text("🛡️  Automatic Updates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                Text(updateStatus, fontSize = 13.sp)
                updateInfo?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Spacer(Modifier.height(8.dp))
                    Text(notes.take(280), fontSize = 11.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(15.dp))

                val readyFile = downloadedUpdate
                val found = updateInfo
                if (readyFile != null) {
                    XPActionButton("Install update") {
                        val opened = UpdateManager.installUpdate(context, readyFile)
                        if (!opened) {
                            updateStatus = "Allow installs from Windroid XP, then return and tap Install update again."
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else if (found != null) {
                    XPActionButton("Download and install") {
                        updateStatus = "Downloading update..."
                        updateScope.launch {
                            val file = UpdateManager.downloadUpdate(context, found)
                            if (file == null) {
                                updateStatus = "The update could not be downloaded. Try again later."
                            } else {
                                downloadedUpdate = file
                                updateStatus = "Download complete. Opening the Android installer..."
                                val opened = UpdateManager.installUpdate(context, file)
                                if (!opened) {
                                    updateStatus = "Allow installs from Windroid XP, then return and tap Install update."
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    XPActionButton("Check again") { checkForUpdates(true) }
                    Spacer(Modifier.height(8.dp))
                }

                Text("Remind me later", color = Color(0xFF003399), fontSize = 12.sp,
                    modifier = Modifier.clickable { updateWindowOpen = false })
            }
        }

        if (startOpen) {
            StartMenu(
                apps,
                context,
                { openAndroidApp(it) },
                { checkForUpdates(true) },
                Modifier.align(Alignment.BottomStart).padding(bottom = taskbarHeight)
            )
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(taskbarHeight).background(
                Brush.verticalGradient(listOf(Color(0xFF3886E8), xpBlue, Color(0xFF1748AF)))
            ), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.fillMaxHeight().width(104.dp).background(
                    Brush.verticalGradient(listOf(Color(0xFF55B747), Color(0xFF33952E), Color(0xFF257E25))),
                    RoundedCornerShape(topEnd = 13.dp, bottomEnd = 13.dp)
                ).clickable { startOpen = !startOpen; computerOpen = false },
                contentAlignment = Alignment.Center
            ) { Text("⊞  start", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.width(5.dp))

            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (computerOpen) {
                    TaskButton("🖥️", "My Computer") { computerOpen = true; startOpen = false }
                }
                launchedApps.forEach { app ->
                    TaskButton("▣", app.label) {
                        startOpen = false
                        launchApp(context, app)
                    }
                }
            }

            Row(
                Modifier.fillMaxHeight().background(Color(0xFF1595D1)).padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("▰  ◉", color = Color.White, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Clock()
            }
        }
    }
}

@Composable
private fun XPActionButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.background(Color(0xFFECE9D8), RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFF7F9DB9), RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = Color.Black)
    }
}

@Composable
private fun TaskButton(icon: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.padding(end = 3.dp).height(31.dp).widthIn(min = 90.dp, max = 150.dp)
            .background(Color(0xFF3579D2), RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFF6AA6F1), RoundedCornerShape(2.dp))
            .clickable { onClick() }.padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = Color.White, fontSize = 11.sp)
        Spacer(Modifier.width(5.dp))
        Text(label, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DesktopIcon(icon: String, label: String, onClick: () -> Unit) {
    Column(Modifier.width(88.dp).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 37.sp)
        Text(label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp, maxLines = 2)
    }
}

@Composable
private fun StartMenu(
    apps: List<LaunchableApp>,
    context: Context,
    onLaunchApp: (LaunchableApp) -> Unit,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xpBlue = Color(0xFF1D62C8)
    Column(modifier.width(350.dp).heightIn(max = 590.dp).shadow(8.dp).border(2.dp, Color(0xFF174EA6)).background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().height(68.dp).background(Brush.verticalGradient(listOf(Color(0xFF2F7BDC), Color(0xFF1855B6)))).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).background(Color.White, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) { Text("🙂", fontSize = 28.sp) }
            Spacer(Modifier.width(10.dp))
            Text("Windroid XP", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.weight(1f)) {
            LazyColumn(Modifier.weight(1.35f).fillMaxHeight().background(Color.White)) {
                item { StartMenuItem("🌐", "Internet") { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))) } }
                item { StartMenuItem("📧", "E-mail") { } }
                item { Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFD6D6D6))) }
                items(apps.take(8)) { app -> StartMenuItem("▣", app.label) { onLaunchApp(app) } }
            }
            Column(Modifier.weight(0.95f).fillMaxHeight().background(Color(0xFFDCEBFA)).padding(vertical = 7.dp)) {
                RightMenuItem("📄", "My Documents") { }
                RightMenuItem("🖼️", "My Pictures") { }
                RightMenuItem("🎵", "My Music") { }
                Spacer(Modifier.height(5.dp)); Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFB4CCE7))); Spacer(Modifier.height(5.dp))
                RightMenuItem("🖥️", "My Computer") { }
                RightMenuItem("⚙️", "Control Panel") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                RightMenuItem("🛡️", "Windows Update") { onCheckUpdates() }
                RightMenuItem("🔍", "Search") { }
                RightMenuItem("❓", "Help and Support") { }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(47.dp).background(xpBlue).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔑 Log Off", color = Color.White, fontSize = 12.sp); Spacer(Modifier.width(16.dp)); Text("⏻ Turn Off Computer", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StartMenuItem(icon: String, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 22.sp); Spacer(Modifier.width(9.dp)); Text(label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RightMenuItem(icon: String, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp); Spacer(Modifier.width(7.dp)); Text(label, color = Color(0xFF163C73), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun XPWindow(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.width(316.dp).shadow(10.dp).background(Color(0xFFECE9D8)).border(2.dp, Color(0xFF245EDB))) {
        Row(
            Modifier.fillMaxWidth().height(31.dp).background(Brush.horizontalGradient(listOf(Color(0xFF0A56D8), Color(0xFF3A8AF1)))).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.weight(1f))
            Box(Modifier.size(20.dp).background(Color(0xFFE95B45), RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) { Text("×", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
        }
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); now = Date() } }
    Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(now), color = Color.White, fontSize = 12.sp)
}
