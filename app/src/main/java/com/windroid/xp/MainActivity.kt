package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun WindroidDesktop(context: Context) {
    var startOpen by remember { mutableStateOf(false) }
    var computerOpen by remember { mutableStateOf(false) }
    val apps = remember { installedApps(context) }
    val xpBlue = Color(0xFF245EDB)

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A92E8), Color(0xFF76C4F4), Color(0xFF58A947))))) {
        Column(Modifier.padding(14.dp).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            DesktopIcon("🖥️", "My Computer") { computerOpen = true }
            DesktopIcon("📁", "My Documents") { }
            DesktopIcon("🌐", "Internet Explorer") {
                val browser = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
                context.startActivity(browser)
            }
            DesktopIcon("🗑️", "Recycle Bin") { }
        }

        if (computerOpen) {
            XPWindow("My Computer", Modifier.align(Alignment.Center)) {
                Text("Files Stored on This Computer", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("📱  Android Device")
                Text("📁  Internal Storage")
                Text("⚙️  Control Panel", Modifier.padding(top = 16.dp).clickable {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                })
                Spacer(Modifier.height(18.dp))
                Text("Close", color = xpBlue, modifier = Modifier.clickable { computerOpen = false })
            }
        }

        if (startOpen) {
            StartMenu(apps, context, Modifier.align(Alignment.BottomStart).padding(bottom = 48.dp))
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(48.dp).background(xpBlue),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.fillMaxHeight().width(105.dp)
                    .background(Color(0xFF3C9D36), RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .clickable { startOpen = !startOpen },
                contentAlignment = Alignment.Center
            ) { Text("⊞  start", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            Clock()
        }
    }
}

@Composable
private fun DesktopIcon(icon: String, label: String, onClick: () -> Unit) {
    Column(Modifier.width(92.dp).clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 36.sp)
        Text(label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun StartMenu(apps: List<LaunchableApp>, context: Context, modifier: Modifier = Modifier) {
    Column(modifier.width(330.dp).heightIn(max = 570.dp).background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(65.dp).background(Color(0xFF1D62C8)), contentAlignment = Alignment.CenterStart) {
            Text("  Windroid XP", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(apps) { app ->
                Text("▣   ${app.label}", Modifier.fillMaxWidth().clickable {
                    context.packageManager.getLaunchIntentForPackage(app.packageName)?.let(context::startActivity)
                }.padding(12.dp), fontSize = 15.sp)
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF2A6ACD)).padding(12.dp), horizontalArrangement = Arrangement.End) {
            Text("Turn Off Computer  ⏻", color = Color.White)
        }
    }
}

@Composable
private fun XPWindow(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.width(310.dp).background(Color(0xFFECE9D8)).padding(3.dp)) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF0A56D8)).padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            now = Date()
        }
    }
    Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(now), color = Color.White, modifier = Modifier.padding(horizontal = 14.dp))
}
