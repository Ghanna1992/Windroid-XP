package com.windroid.xp

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
internal fun MovableDesktopItem(prefs: android.content.SharedPreferences, positionId: String, defaultX: Float, defaultY: Float, maxWidth: androidx.compose.ui.unit.Dp, maxHeight: androidx.compose.ui.unit.Dp, dragEnabled: Boolean = true, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val maxXPx = with(density) { (maxWidth - 88.dp).coerceAtLeast(0.dp).toPx() }
    val maxYPx = with(density) { (maxHeight - 82.dp).coerceAtLeast(0.dp).toPx() }
    val defaultXPx = with(density) { defaultX.dp.toPx() }
    val defaultYPx = with(density) { defaultY.dp.toPx() }
    val keyX = desktopPositionKey(positionId, "x")
    val keyY = desktopPositionKey(positionId, "y")
    var position by remember(positionId, maxXPx, maxYPx) {
        mutableStateOf(Offset(prefs.getFloat(keyX, defaultXPx).coerceIn(0f, maxXPx), prefs.getFloat(keyY, defaultYPx).coerceIn(0f, maxYPx)))
    }
    var dragging by remember(positionId) { mutableStateOf(false) }
    fun save() { prefs.edit().putFloat(keyX, position.x).putFloat(keyY, position.y).apply() }

    Box(
        Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .zIndex(if (dragging) 100f else 0f)
            .pointerInput(positionId, maxXPx, maxYPx, dragEnabled) {
                if (!dragEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val pointerId = down.id
                    val startParent = position + down.position
                    var lastParent = startParent
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed || !dragEnabled) break
                        val pointerParent = position + change.position
                        val total = pointerParent - startParent
                        if (!moved && total.getDistance() > viewConfiguration.touchSlop) {
                            moved = true
                            dragging = true
                        }
                        if (moved) {
                            val delta = pointerParent - lastParent
                            position = Offset(
                                (position.x + delta.x).coerceIn(0f, maxXPx),
                                (position.y + delta.y).coerceIn(0f, maxYPx)
                            )
                            change.consume()
                        }
                        lastParent = pointerParent
                    }
                    if (moved) {
                        dragging = false
                        save()
                    }
                }
            }
    ) { content() }
}

@Composable internal fun WallpaperLayer(context: Context, selectedBackground: String?) { val image = remember(selectedBackground) { loadAssetImage(context, "backgrounds", selectedBackground ?: DEFAULT_DESKTOP_BACKGROUND) }; if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else XPWallpaper() }
@Composable internal fun XPWallpaper() { Canvas(Modifier.fillMaxSize()) { drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF2087D5), Color(0xFF55B6ED), Color(0xFFBDEAFF)), endY = size.height * 0.68f)); val farHill = Path().apply { moveTo(0f, size.height * 0.70f); cubicTo(size.width * 0.18f, size.height * 0.58f, size.width * 0.44f, size.height * 0.64f, size.width * 0.64f, size.height * 0.72f); cubicTo(size.width * 0.78f, size.height * 0.78f, size.width * 0.92f, size.height * 0.70f, size.width, size.height * 0.67f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }; drawPath(farHill, brush = Brush.verticalGradient(listOf(Color(0xFF79C752), Color(0xFF4DAA35)))); val nearHill = Path().apply { moveTo(0f, size.height * 0.82f); cubicTo(size.width * 0.20f, size.height * 0.69f, size.width * 0.40f, size.height * 0.70f, size.width * 0.58f, size.height * 0.82f); cubicTo(size.width * 0.75f, size.height * 0.93f, size.width * 0.90f, size.height * 0.85f, size.width, size.height * 0.80f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }; drawPath(nearHill, brush = Brush.verticalGradient(listOf(Color(0xFF56B33D), Color(0xFF248C2A)))) } }

@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun DesktopSystemShortcut(context: Context, prefs: android.content.SharedPreferences, version: Int, id: String, registryKey: String, fallback: String, label: String, onClick: () -> Unit, onCustomize: () -> Unit, onReset: () -> Unit, onRemove: () -> Unit, onMenuStateChange: (Boolean) -> Unit = {}) {
    var menuOpen by remember { mutableStateOf(false) }; val customName = remember(id, version) { prefs.getString(iconPrefKey(id), null) }; val customIcon = remember(id, version, customName) { loadAssetImage(context, "icons", customName) }; val icon = customIcon ?: xpIcon(context, registryKey)
    fun closeMenu() { menuOpen = false; onMenuStateChange(false) }
    Box { Column(Modifier.width(88.dp).combinedClickable(onClick = onClick, onLongClick = { menuOpen = true; onMenuStateChange(true) }), horizontalAlignment = Alignment.CenterHorizontally) { if (icon != null) Image(bitmap = icon, contentDescription = label, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit) else Text(fallback, fontSize = 38.sp); Spacer(Modifier.height(3.dp)); Text(label, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }; if (menuOpen) androidx.compose.ui.window.Popup(alignment = Alignment.TopStart, offset = IntOffset(55, 28), onDismissRequest = { closeMenu() }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) { Column(Modifier.width(170.dp).background(Color(0xFFFFF8E7)).border(1.dp, Color(0xFF777777)).padding(4.dp)) { Text("Open", modifier = Modifier.fillMaxWidth().clickable { closeMenu(); onClick() }.padding(7.dp), fontSize = 12.sp); Text("Change Icon...", modifier = Modifier.fillMaxWidth().clickable { closeMenu(); onCustomize() }.padding(7.dp), fontSize = 12.sp); if (customName != null) Text("Reset Icon", modifier = Modifier.fillMaxWidth().clickable { closeMenu(); onReset() }.padding(7.dp), fontSize = 12.sp); Text("Remove from Desktop", modifier = Modifier.fillMaxWidth().clickable { closeMenu(); onRemove() }.padding(7.dp), fontSize = 12.sp); Text("Cancel", modifier = Modifier.fillMaxWidth().clickable { closeMenu() }.padding(7.dp), fontSize = 12.sp) } } }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable internal fun DesktopAppIcon(context: Context, prefs: android.content.SharedPreferences, version: Int, app: LaunchableApp, onClick: () -> Unit, onRemove: () -> Unit, onMenuStateChange: (Boolean) -> Unit = {}) {
    val image = remember(version, app.packageName) { loadAssetImage(context, "icons", prefs.getString(iconPrefKey("app_${app.packageName}"), null)) }; var menuOpen by remember { mutableStateOf(false) }
    fun closeMenu() { menuOpen = false; onMenuStateChange(false) }
    Box(Modifier.width(88.dp)) { Column(Modifier.width(88.dp).combinedClickable(onClick = onClick, onLongClick = { menuOpen = true; onMenuStateChange(true) }), horizontalAlignment = Alignment.CenterHorizontally) { val icon = image ?: app.icon; if (icon != null) Image(bitmap = icon, contentDescription = app.label, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit) else Text("▣", fontSize = 38.sp); Spacer(Modifier.height(3.dp)); Text(app.label, color = Color.White, fontSize = 12.sp, lineHeight = 13.sp, maxLines = 2) }; if (menuOpen) androidx.compose.ui.window.Popup(alignment = Alignment.TopStart, offset = IntOffset(58, 28), onDismissRequest = { closeMenu() }, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) { Column(Modifier.width(190.dp).shadow(10.dp).background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(5.dp)) { Text(app.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp)); ContextMenuRow("Open") { closeMenu(); onClick() }; ContextMenuRow("Remove from Desktop") { closeMenu(); onRemove() }; ContextMenuRow("App Info") { closeMenu(); openAppInfo(context, app.packageName) }; ContextMenuRow("Cancel") { closeMenu() } } } }
}
