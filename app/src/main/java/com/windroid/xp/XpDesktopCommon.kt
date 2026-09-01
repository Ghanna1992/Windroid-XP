package com.windroid.xp

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable internal fun XPActionButton(label: String, onClick: () -> Unit) { Box(Modifier.background(Color(0xFFECE9D8), RoundedCornerShape(2.dp)).border(1.dp, Color(0xFF7F9DB9), RoundedCornerShape(2.dp)).clickable { onClick() }.padding(horizontal = 14.dp, vertical = 6.dp)) { Text(label, fontSize = 12.sp, color = Color.Black) } }

@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun TaskButton(icon: ImageBitmap?, fallback: String, label: String, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) { Box(Modifier.padding(end = 3.dp).size(34.dp).background(Color(0xFF3579D2), RoundedCornerShape(2.dp)).border(1.dp, Color(0xFF6AA6F1), RoundedCornerShape(2.dp)).combinedClickable(onClick = onClick, onLongClick = { onLongClick?.invoke() }), contentAlignment = Alignment.Center) { if (icon != null) Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(23.dp), contentScale = ContentScale.Fit) else Text(fallback, color = Color.White, fontSize = 15.sp) } }

@Composable internal fun ContextMenuRow(label: String, onClick: () -> Unit) { Text(label, fontSize = 12.sp, color = Color.Black, modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 10.dp, vertical = 9.dp)) }
@Composable internal fun XPSystemRow(context: Context, iconKey: String, label: String, onClick: () -> Unit) { val icon = remember(iconKey) { xpIcon(context, iconKey) }; Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { if (icon != null) Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(30.dp)) else Box(Modifier.size(30.dp)); Spacer(Modifier.width(9.dp)); Text(label, color = Color(0xFF003399), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }

@Composable internal fun XPWindow(title: String, modifier: Modifier = Modifier, onClose: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val context = LocalContext.current
    var maximized by remember(title) { mutableStateOf(false) }
    val barImage = remember { loadAssetImage(context, "icons", "ui/Window Bar.jpg") }
    val closeImage = remember { loadAssetImage(context, "icons", "ui/Window X.jpg") }
    val minimizeImage = remember { loadAssetImage(context, "icons", "ui/Minimize.jpg") }
    val maximizeImage = remember { loadAssetImage(context, "icons", "ui/Maximize.jpg") }
    val frameModifier = if (maximized) modifier.fillMaxSize().padding(bottom = 43.dp) else modifier.width(316.dp)
    Column(frameModifier.shadow(10.dp).background(Color(0xFFECE9D8)).border(2.dp, Color(0xFF245EDB))) {
        Box(Modifier.fillMaxWidth().height(31.dp)) {
            if (barImage != null) Image(bitmap = barImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) else Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF0A56D8), Color(0xFF3A8AF1)))))
            Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                if (onClose != null) {
                    if (minimizeImage != null) Image(bitmap = minimizeImage, contentDescription = "Minimize", modifier = Modifier.size(22.dp).clickable { onClose() }, contentScale = ContentScale.Fit) else XPTitleFallback("_", onClose)
                    Spacer(Modifier.width(2.dp))
                    if (maximizeImage != null) Image(bitmap = maximizeImage, contentDescription = if (maximized) "Restore" else "Maximize", modifier = Modifier.size(22.dp).clickable { maximized = !maximized }, contentScale = ContentScale.Fit) else XPTitleFallback("□") { maximized = !maximized }
                    Spacer(Modifier.width(2.dp))
                    if (closeImage != null) Image(bitmap = closeImage, contentDescription = "Close", modifier = Modifier.size(22.dp).clickable { onClose() }, contentScale = ContentScale.Fit) else XPTitleFallback("×", onClose)
                }
            }
        }
        Column(Modifier.padding(18.dp).then(if (maximized) Modifier.fillMaxSize() else Modifier), content = content)
    }
}

@Composable private fun XPTitleFallback(label: String, onClick: () -> Unit) { Box(Modifier.size(20.dp).background(Color(0xFF3579D2), RoundedCornerShape(2.dp)).border(1.dp, Color.White, RoundedCornerShape(2.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) { Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) } }

