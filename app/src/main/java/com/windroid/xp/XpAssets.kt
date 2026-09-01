package com.windroid.xp

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal fun listAssetImages(context: Context, folder: String): List<String> {
    val allowed = setOf("png", "jpg", "jpeg", "webp")
    val results = mutableListOf<String>()
    fun scan(assetFolder: String, relativePrefix: String = "") {
        val children = try { context.assets.list(assetFolder)?.toList().orEmpty() } catch (_: Exception) { emptyList() }
        for (name in children) {
            val full = "$assetFolder/$name"
            val relative = if (relativePrefix.isBlank()) name else "$relativePrefix/$name"
            val ext = name.substringAfterLast('.', "").lowercase()
            when {
                ext in allowed -> results.add(relative)
                ext == "zip" -> try {
                    context.assets.open(full).use { raw ->
                        java.util.zip.ZipInputStream(raw).use { zip ->
                            var entry = zip.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory && entry.name.substringAfterLast('.', "").lowercase() in allowed) results.add("$relative::${entry.name}")
                                zip.closeEntry()
                                entry = zip.nextEntry
                            }
                        }
                    }
                } catch (_: Exception) { }
                ext.isBlank() -> scan(full, relative)
            }
        }
    }
    return try { scan(folder); results.sortedBy { it.substringAfter("::", it).lowercase() } } catch (_: Exception) { emptyList() }
}

private val assetImageCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap?>()

internal fun loadAssetImage(context: Context, folder: String, fileName: String?): ImageBitmap? {
    if (fileName.isNullOrBlank()) return null
    if (fileName.startsWith("custom::")) {
        return CustomIconLibrary.fileForId(context, fileName)?.let { CustomIconLibrary.load(it) }
    }
    val key = "$folder::$fileName"
    if (assetImageCache.containsKey(key)) return assetImageCache[key]
    fun candidatePaths(name: String): List<String> {
        if (folder != "icons" || name.contains('/')) return listOf("$folder/$name")
        return listOf("icons/$name", "icons/xp/$name", "icons/apps/$name", "icons/ui/$name")
    }
    val image = try {
        if (fileName.contains("::")) {
            val zipName = fileName.substringBefore("::")
            val entryName = fileName.substringAfter("::")
            var found: ImageBitmap? = null
            for (path in candidatePaths(zipName)) {
                try {
                    context.assets.open(path).use { raw ->
                        java.util.zip.ZipInputStream(raw).use { zip ->
                            var entry = zip.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory && entry.name == entryName) {
                                    val bytes = java.io.ByteArrayOutputStream()
                                    zip.copyTo(bytes)
                                    found = BitmapFactory.decodeByteArray(bytes.toByteArray(), 0, bytes.size())?.asImageBitmap()
                                    break
                                }
                                zip.closeEntry(); entry = zip.nextEntry
                            }
                        }
                    }
                } catch (_: Exception) { }
                if (found != null) break
            }
            found
        } else {
            var found: ImageBitmap? = null
            for (path in candidatePaths(fileName)) {
                try { found = context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } } catch (_: Exception) { }
                if (found != null) break
            }
            found
        }
    } catch (_: Exception) { null }
    if (image != null) assetImageCache[key] = image
    return image
}

internal fun loadStartButtonImage(context: Context): ImageBitmap? {
    return try {
        val decoded = context.assets.open("icons/ui/start_button.png").use { BitmapFactory.decodeStream(it) } ?: return null
        val bitmap = decoded.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap.asImageBitmap()
        fun removableBackground(pixel: Int): Boolean {
            val a = android.graphics.Color.alpha(pixel)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val average = (r + g + b) / 3
            return a == 0 || (max - min <= 30 && (average >= 135 || average <= 45))
        }
        val seen = BooleanArray(width * height)
        val queue = java.util.ArrayDeque<Int>()
        fun seed(x: Int, y: Int) {
            val index = y * width + x
            if (!seen[index] && removableBackground(bitmap.getPixel(x, y))) { seen[index] = true; queue.add(index) }
        }
        for (x in 0 until width) { seed(x, 0); seed(x, height - 1) }
        for (y in 0 until height) { seed(0, y); seed(width - 1, y) }
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            val x = index % width
            val y = index / width
            bitmap.setPixel(x, y, android.graphics.Color.TRANSPARENT)
            if (x > 0) seed(x - 1, y)
            if (x + 1 < width) seed(x + 1, y)
            if (y > 0) seed(x, y - 1)
            if (y + 1 < height) seed(x, y + 1)
        }
        bitmap.asImageBitmap()
    } catch (_: Exception) { loadAssetImage(context, "icons", "ui/start_button.png") }
}
