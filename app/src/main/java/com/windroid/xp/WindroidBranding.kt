package com.windroid.xp

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@Composable
fun WindroidBrandAsset(
    wordmark: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val fileName = if (wordmark) "Windroid Text.png" else "Windroid logo.png"
    val bitmap = remember(fileName) {
        try {
            context.assets.open("icons/xp/$fileName").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        } catch (_: Exception) {
            null
        }
    }
    bitmap?.let { Image(it, contentDescription = if (wordmark) "Windroid XP" else "Windroid", modifier = modifier, contentScale = contentScale) }
}
