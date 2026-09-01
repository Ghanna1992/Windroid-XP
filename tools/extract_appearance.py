from pathlib import Path

src = Path('app/src/main/java/com/windroid/xp/MainActivity.kt')
text = src.read_text()
start = text.index('@Composable private fun AppearanceWindow')
end = text.index('@Composable private fun XPProgressBar', start)
block = text[start:end].rstrip() + '\n'
replacements = {
    '@Composable private fun AppearanceWindow': '@Composable internal fun AppearanceWindow',
    '@Composable private fun SettingsChoice': '@Composable internal fun SettingsChoice',
    '@Composable private fun PickerRow': '@Composable internal fun PickerRow',
    '@Composable private fun AssignmentRow': '@Composable internal fun AssignmentRow',
    '@Composable private fun AppCustomizationRow': '@Composable internal fun AppCustomizationRow',
    '@Composable private fun XPActionButton': '@Composable internal fun XPActionButton',
}
for old, new in replacements.items():
    block = block.replace(old, new, 1)
imports = '''package com.windroid.xp

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

'''
Path('app/src/main/java/com/windroid/xp/XpAppearance.kt').write_text(imports + block)
remaining = text[:start] + text[end:]
remaining = remaining.replace('@Composable private fun XPWindow', '@Composable internal fun XPWindow', 1)
remaining = remaining.replace('@Composable\nprivate fun XPWindow', '@Composable\ninternal fun XPWindow', 1)
remaining = remaining.replace('private fun iconCategory', 'internal fun iconCategory', 1)
src.write_text(remaining)
