from pathlib import Path

src = Path('app/src/main/java/com/windroid/xp/MainActivity.kt')
text = src.read_text()
start = text.index('@Composable\nprivate fun MovableDesktopItem')
end = text.index('@Composable private fun AppearanceWindow', start)
block = text[start:end].rstrip() + '\n'
block = block.replace('@Composable\nprivate fun MovableDesktopItem', '@Composable\ninternal fun MovableDesktopItem', 1)
block = block.replace('@Composable private fun WallpaperLayer', '@Composable internal fun WallpaperLayer', 1)
block = block.replace('@Composable private fun XPWallpaper', '@Composable internal fun XPWallpaper', 1)
block = block.replace('@Composable private fun DesktopSystemShortcut', '@Composable internal fun DesktopSystemShortcut', 1)
block = block.replace('@Composable private fun DesktopAppIcon', '@Composable internal fun DesktopAppIcon', 1)
imports = '''package com.windroid.xp

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

'''
Path('app/src/main/java/com/windroid/xp/XpDesktopIcons.kt').write_text(imports + block)
remaining = text[:start] + text[end:]
remaining = remaining.replace('private const val DEFAULT_DESKTOP_BACKGROUND', 'internal const val DEFAULT_DESKTOP_BACKGROUND', 1)
remaining = remaining.replace('private fun iconPrefKey', 'internal fun iconPrefKey', 1)
remaining = remaining.replace('private fun desktopPositionKey', 'internal fun desktopPositionKey', 1)
remaining = remaining.replace('private val XP_ICON_REGISTRY', 'internal val XP_ICON_REGISTRY', 1)
remaining = remaining.replace('private fun xpIcon', 'internal fun xpIcon', 1)
remaining = remaining.replace('@Composable private fun ContextMenuRow', '@Composable internal fun ContextMenuRow', 1)
src.write_text(remaining)
