package com.windroid.xp

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun XpWindowShell(
    title: String,
    modifier: Modifier = Modifier,
    initiallyMaximized: Boolean = false,
    showMinimize: Boolean = true,
    showMaximize: Boolean = true,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var maximized by remember { mutableStateOf(initiallyMaximized) }
    val windowModifier = if (maximized) {
        modifier.fillMaxSize()
    } else {
        modifier.fillMaxWidth(0.94f).fillMaxHeight(0.90f)
    }

    Column(windowModifier.background(Color(0xFFECE9D8)).border(2.dp, Color(0xFF0A3AA8))) {
        XpTitleBar(
            title = title,
            showMinimize = showMinimize,
            showMaximize = showMaximize,
            maximized = maximized,
            onMinimize = onMinimize,
            onMaximize = { if (showMaximize) maximized = !maximized },
            onClose = onClose
        )
        Box(Modifier.weight(1f).fillMaxWidth().background(Color.White), content = content)
    }
}

@Composable
private fun XpTitleBar(
    title: String,
    showMinimize: Boolean,
    showMaximize: Boolean,
    maximized: Boolean,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val bar = remember { loadWindowAsset(context, "Window Bar.jpg") }
    val minimize = remember { loadWindowAsset(context, "Minimize.jpg") }
    val maximize = remember { loadWindowAsset(context, "Maximize.jpg") }
    val close = remember { loadWindowAsset(context, "Window X.jpg") }

    Box(Modifier.fillMaxWidth().height(31.dp)) {
        if (bar != null) {
            Image(bar, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFF0755C7)))
        }

        Row(
            Modifier.fillMaxSize().padding(start = 8.dp, end = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (showMinimize) WindowControlButton(minimize, "_", "Minimize", onMinimize)
            if (showMaximize) WindowControlButton(maximize, if (maximized) "❐" else "□", if (maximized) "Restore" else "Maximize", onMaximize)
            WindowControlButton(close, "×", "Close", onClose)
        }
    }
}

@Composable
private fun WindowControlButton(
    image: ImageBitmap?,
    fallback: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .padding(start = 2.dp)
            .size(26.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(image, description, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        } else {
            Box(Modifier.fillMaxSize().background(Color(0xFF2B74D7)).border(1.dp, Color.White), contentAlignment = Alignment.Center) {
                Text(fallback, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun loadWindowAsset(context: android.content.Context, name: String): ImageBitmap? = try {
    context.assets.open("icons/ui/$name").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
} catch (_: Exception) {
    null
}
