package com.windroid.xp

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

object CustomIconLibrary {
    private const val DIRECTORY = "custom_icons"
    private const val MAX_SINGLE_IMAGE_BYTES = 12L * 1024L * 1024L
    private const val MAX_ZIP_BYTES = 100L * 1024L * 1024L
    private const val MAX_EXTRACTED_BYTES = 150L * 1024L * 1024L
    private const val MAX_ZIP_IMAGES = 1000

    private val supportedExtensions = setOf("png", "jpg", "jpeg", "webp")

    data class ImportResult(
        val imported: Int,
        val skipped: Int,
        val error: String? = null
    )

    fun directory(context: Context): File =
        File(context.filesDir, DIRECTORY).apply { mkdirs() }

    fun list(context: Context): List<File> =
        directory(context)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && extensionOf(it.name) in supportedExtensions }
            .sortedBy { it.name.lowercase(Locale.ROOT) }

    fun load(file: File): ImageBitmap? = try {
        if (!file.exists() || !file.isFile) return null
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }

    fun fileForId(context: Context, id: String?): File? {
        if (id.isNullOrBlank() || !id.startsWith("custom::")) return null
        val name = id.removePrefix("custom::")
        if (name.isBlank() || name.contains('/') || name.contains('\\')) return null
        val file = File(directory(context), name)
        return file.takeIf { it.exists() && it.isFile }
    }

    fun idFor(file: File): String = "custom::${file.name}"

    fun delete(context: Context, id: String): Boolean =
        fileForId(context, id)?.delete() == true

    fun rename(context: Context, id: String, requestedName: String): String? {
        val source = fileForId(context, id) ?: return null
        val ext = extensionOf(source.name)
        val base = sanitizeBaseName(requestedName.substringBeforeLast('.', requestedName))
        if (base.isBlank()) return null
        val target = uniqueFile(directory(context), "$base.$ext")
        return if (source.renameTo(target)) idFor(target) else null
    }

    fun clear(context: Context): Int {
        var deleted = 0
        for (file in list(context)) {
            if (file.delete()) deleted++
        }
        return deleted
    }

    fun importUris(context: Context, uris: List<Uri>): ImportResult {
        var imported = 0
        var skipped = 0
        var firstError: String? = null

        for (uri in uris) {
            val name = displayName(context, uri) ?: "icon"
            val extension = extensionOf(name)
            val result = if (extension == "zip") {
                importZip(context, uri)
            } else {
                importImage(context, uri, name)
            }
            imported += result.imported
            skipped += result.skipped
            if (firstError == null) firstError = result.error
        }
        return ImportResult(imported, skipped, firstError)
    }

    private fun importImage(context: Context, uri: Uri, displayName: String): ImportResult {
        val extension = extensionOf(displayName)
        if (extension !in supportedExtensions) return ImportResult(0, 1)

        return try {
            val destination = uniqueFile(directory(context), sanitizeFileName(displayName))
            var written = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        written += count
                        if (written > MAX_SINGLE_IMAGE_BYTES) {
                            destination.delete()
                            return ImportResult(0, 1, "Image is larger than 12 MB.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: return ImportResult(0, 1, "Unable to open selected image.")

            if (!isDecodableImage(destination)) {
                destination.delete()
                ImportResult(0, 1, "Selected file is not a valid image.")
            } else {
                ImportResult(1, 0)
            }
        } catch (e: Exception) {
            ImportResult(0, 1, e.message ?: "Unable to import image.")
        }
    }

    private fun importZip(context: Context, uri: Uri): ImportResult {
        val temp = File.createTempFile("icon-import-", ".zip", context.cacheDir)
        try {
            var zipBytes = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        zipBytes += count
                        if (zipBytes > MAX_ZIP_BYTES) {
                            return ImportResult(0, 1, "ZIP is larger than 100 MB.")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: return ImportResult(0, 1, "Unable to open selected ZIP.")

            var imported = 0
            var skipped = 0
            var extractedBytes = 0L
            ZipInputStream(temp.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val leafName = entry.name.substringAfterLast('/').substringAfterLast('\\')
                        val extension = extensionOf(leafName)
                        if (extension in supportedExtensions && imported < MAX_ZIP_IMAGES) {
                            val destination = uniqueFile(directory(context), sanitizeFileName(leafName))
                            var entryBytes = 0L
                            FileOutputStream(destination).use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                while (true) {
                                    val count = zip.read(buffer)
                                    if (count < 0) break
                                    entryBytes += count
                                    extractedBytes += count
                                    if (entryBytes > MAX_SINGLE_IMAGE_BYTES || extractedBytes > MAX_EXTRACTED_BYTES) {
                                        break
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }

                            if (
                                entryBytes > MAX_SINGLE_IMAGE_BYTES ||
                                extractedBytes > MAX_EXTRACTED_BYTES ||
                                !isDecodableImage(destination)
                            ) {
                                destination.delete()
                                skipped++
                            } else {
                                imported++
                            }

                            if (extractedBytes > MAX_EXTRACTED_BYTES) {
                                return ImportResult(imported, skipped + 1, "ZIP expands beyond the 150 MB safety limit.")
                            }
                        } else if (extension in supportedExtensions) {
                            skipped++
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            ImportResult(imported, skipped)
        } catch (e: Exception) {
            ImportResult(0, 1, e.message ?: "Unable to import ZIP.")
        } finally {
            temp.delete()
        }
    }

    private fun displayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) null
                    else cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun isDecodableImage(file: File): Boolean = try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        options.outWidth > 0 && options.outHeight > 0
    } catch (_: Exception) {
        false
    }

    private fun sanitizeFileName(name: String): String {
        val ext = extensionOf(name).takeIf { it in supportedExtensions } ?: "png"
        val base = sanitizeBaseName(name.substringBeforeLast('.', name)).ifBlank { "icon" }
        return "$base.$ext"
    }

    private fun sanitizeBaseName(value: String): String =
        value.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .take(80)

    private fun extensionOf(name: String): String =
        name.substringAfterLast('.', "").lowercase(Locale.ROOT)

    private fun uniqueFile(parent: File, requestedName: String): File {
        var candidate = File(parent, requestedName)
        if (!candidate.exists()) return candidate

        val ext = requestedName.substringAfterLast('.', "")
        val base = requestedName.substringBeforeLast('.', requestedName)
        var index = 2
        while (candidate.exists()) {
            val nextName = if (ext.isBlank()) "$base ($index)" else "$base ($index).$ext"
            candidate = File(parent, nextName)
            index++
        }
        return candidate
    }
}
