package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple store-free updater.
 *
 * The default endpoint is GitHub Releases. It works for public release repositories.
 * If the source repository stays private, point RELEASE_API_URL at a public release-only
 * repository later; no other updater code needs to change.
 */
object UpdateManager {
    private const val RELEASE_API_URL =
        "https://api.github.com/repos/Ghanna1992/Windroid-XP/releases/latest"

    data class UpdateInfo(
        val versionName: String,
        val downloadUrl: String,
        val notes: String
    )

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Windroid-XP/${BuildConfig.VERSION_NAME}")
            }

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return@withContext null
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val release = JSONObject(json)
            val latest = release.optString("tag_name").removePrefix("v")
            if (latest.isBlank() || !isNewer(latest, BuildConfig.VERSION_NAME)) {
                return@withContext null
            }

            val assets = release.optJSONArray("assets") ?: return@withContext null
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == "Windroid-XP.apk") {
                    downloadUrl = asset.optString("browser_download_url")
                    break
                }
            }

            if (downloadUrl.isNullOrBlank()) return@withContext null
            UpdateInfo(latest, downloadUrl, release.optString("body"))
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadUpdate(context: Context, update: UpdateInfo): File? = withContext(Dispatchers.IO) {
        try {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apk = File(updateDir, "Windroid-XP-${update.versionName}.apk")
            val connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "Windroid-XP/${BuildConfig.VERSION_NAME}")
            }
            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return@withContext null
            }
            connection.inputStream.use { input ->
                FileOutputStream(apk).use { output -> input.copyTo(output) }
            }
            connection.disconnect()
            apk
        } catch (_: Exception) {
            null
        }
    }

    /** Returns true when the Android package installer was opened. */
    fun installUpdate(context: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        return true
    }

    private fun isNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val b = current.split('.').map { it.toIntOrNull() ?: 0 }
        val count = maxOf(a.size, b.size)
        for (i in 0 until count) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av > bv
        }
        return false
    }
}
