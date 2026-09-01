package com.windroid.xp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL

object UpdateManager {
    private const val STABLE_RELEASE_API_URL =
        "https://api.github.com/repos/Ghanna1992/Windroid-XP/releases/latest"
    private const val RELEASES_API_URL =
        "https://api.github.com/repos/Ghanna1992/Windroid-XP/releases?per_page=100&page=1"

    data class UpdateInfo(
        val versionName: String,
        val downloadUrl: String,
        val notes: String
    )

    data class UpdateHistoryItem(
        val versionName: String,
        val publishedAt: String,
        val notes: String
    )

    sealed class CheckResult {
        data class UpdateAvailable(val update: UpdateInfo) : CheckResult()
        data object UpToDate : CheckResult()
        data class Failed(val message: String) : CheckResult()
    }

    suspend fun checkForUpdate(): CheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val isDev = BuildConfig.UPDATE_CHANNEL == "dev"
            val endpoint = if (isDev) "$RELEASES_API_URL&_=${System.currentTimeMillis()}" else STABLE_RELEASE_API_URL
            connection = openApiConnection(endpoint)

            val response = connection.responseCode
            if (response !in 200..299) {
                return@withContext CheckResult.Failed("GitHub returned error $response. Try again later.")
            }

            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val release = (if (isDev) findLatestDevRelease(JSONArray(json)) else JSONObject(json))
                ?: return@withContext CheckResult.Failed("No ${BuildConfig.UPDATE_CHANNEL} release is available yet.")

            val tag = release.optString("tag_name")
            val latest = if (isDev) tag.removePrefix("dev-v") else tag.removePrefix("v")
            if (latest.isBlank()) {
                return@withContext CheckResult.Failed("The update server returned an invalid release.")
            }
            if (!isNewer(latest, BuildConfig.VERSION_NAME)) {
                return@withContext CheckResult.UpToDate
            }

            val expectedAsset = if (isDev) "Windroid-XP-Dev.apk" else "Windroid-XP.apk"
            val assets = release.optJSONArray("assets")
                ?: return@withContext CheckResult.Failed("The latest release has no downloadable APK.")
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == expectedAsset) {
                    downloadUrl = asset.optString("browser_download_url")
                    break
                }
            }

            if (downloadUrl.isNullOrBlank()) {
                return@withContext CheckResult.Failed("The latest release is missing $expectedAsset.")
            }
            CheckResult.UpdateAvailable(UpdateInfo(latest, downloadUrl, release.optString("body")))
        } catch (_: UnknownHostException) {
            CheckResult.Failed("No internet connection. Check Wi-Fi or mobile data and try again.")
        } catch (_: SocketTimeoutException) {
            CheckResult.Failed("The update server took too long to respond. Try again.")
        } catch (e: Exception) {
            CheckResult.Failed("Unable to check for updates: ${e.javaClass.simpleName}")
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun loadUpdateHistory(limit: Int = 5): List<UpdateHistoryItem> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val isDev = BuildConfig.UPDATE_CHANNEL == "dev"
            connection = openApiConnection("$RELEASES_API_URL&_=${System.currentTimeMillis()}")
            if (connection.responseCode !in 200..299) return@withContext emptyList()

            val releases = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
            val history = mutableListOf<UpdateHistoryItem>()
            for (i in 0 until releases.length()) {
                val release = releases.optJSONObject(i) ?: continue
                if (release.optBoolean("draft", false)) continue
                val prerelease = release.optBoolean("prerelease", false)
                if (isDev != prerelease) continue

                val tag = release.optString("tag_name")
                val version = if (isDev) {
                    if (!tag.startsWith("dev-v")) continue
                    tag.removePrefix("dev-v")
                } else {
                    if (!tag.startsWith("v") || tag.startsWith("dev-v")) continue
                    tag.removePrefix("v")
                }
                if (version.isBlank()) continue

                history += UpdateHistoryItem(
                    versionName = version,
                    publishedAt = release.optString("published_at"),
                    notes = release.optString("body")
                )
                if (history.size >= limit.coerceAtLeast(1)) break
            }
            history
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun openApiConnection(endpoint: String): HttpURLConnection =
        (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            useCaches = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Windroid-XP-${BuildConfig.UPDATE_CHANNEL}/${BuildConfig.VERSION_NAME}")
        }

    private fun findLatestDevRelease(releases: JSONArray): JSONObject? {
        var bestRelease: JSONObject? = null
        var bestVersion: String? = null

        for (i in 0 until releases.length()) {
            val release = releases.optJSONObject(i) ?: continue
            if (release.optBoolean("draft", false)) continue
            if (!release.optBoolean("prerelease", false)) continue

            val tag = release.optString("tag_name")
            if (!tag.startsWith("dev-v")) continue

            val version = tag.removePrefix("dev-v")
            if (version.isBlank()) continue

            if (bestVersion == null || isNewer(version, bestVersion)) {
                bestVersion = version
                bestRelease = release
            }
        }
        return bestRelease
    }

    suspend fun downloadUpdate(
        context: Context,
        update: UpdateInfo,
        onProgress: (Int) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var apk: File? = null
        try {
            onProgress(0)
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            apk = File(updateDir, "Windroid-XP-${BuildConfig.UPDATE_CHANNEL}-${update.versionName}.apk")
            if (apk.exists()) apk.delete()

            connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "Windroid-XP-${BuildConfig.UPDATE_CHANNEL}/${BuildConfig.VERSION_NAME}")
            }
            if (connection.responseCode !in 200..299) return@withContext null

            val totalBytes = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(apk).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var read: Int
                    var lastProgress = -1
                    while (input.read(buffer).also { read = it } >= 0) {
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            val progress = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            val downloadedBytes = apk.length()
            if (downloadedBytes <= 0L || (totalBytes > 0L && downloadedBytes != totalBytes)) {
                apk.delete()
                return@withContext null
            }

            onProgress(100)
            apk
        } catch (_: Exception) {
            apk?.delete()
            null
        } finally {
            connection?.disconnect()
        }
    }

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
        fun parts(value: String): List<Int> = Regex("\\d+").findAll(value).map { it.value.toInt() }.toList()
        val a = parts(candidate)
        val b = parts(current)
        val count = maxOf(a.size, b.size)
        for (i in 0 until count) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av != bv) return av > bv
        }
        return false
    }
}
