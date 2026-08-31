package com.windroid.xp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val UPDATE_PREFS = "windroid_update_settings"
private const val AUTO_UPDATE_CHECKS = "automatic_update_checks"

@Composable
fun WindowsUpdateOverlay(
    context: Context,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = remember { context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE) }
    var automaticChecks by remember { mutableStateOf(prefs.getBoolean(AUTO_UPDATE_CHECKS, true)) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var status by remember { mutableStateOf(if (automaticChecks) "Checking for updates..." else "Automatic update checks are turned off.") }
    var progress by remember { mutableIntStateOf(-1) }
    var downloaded by remember { mutableStateOf<java.io.File?>(null) }
    var history by remember { mutableStateOf<List<UpdateManager.UpdateHistoryItem>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun check() {
        progress = -1
        downloaded = null
        status = "Checking for updates..."
        scope.launch {
            when (val result = UpdateManager.checkForUpdate()) {
                is UpdateManager.CheckResult.UpdateAvailable -> {
                    updateInfo = result.update
                    status = "Windroid XP ${result.update.versionName} is ready."
                }
                UpdateManager.CheckResult.UpToDate -> {
                    updateInfo = null
                    status = "Your computer is up to date."
                }
                is UpdateManager.CheckResult.Failed -> {
                    updateInfo = null
                    status = result.message
                }
            }
        }
    }

    fun loadHistory() {
        historyLoading = true
        scope.launch {
            history = UpdateManager.loadUpdateHistory(5)
            historyLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (automaticChecks) check()
        loadHistory()
    }

    Box(
        Modifier.fillMaxSize().background(Color(0x22000000)).clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { }
    )

    Box(modifier.fillMaxSize().padding(bottom = 43.dp)) {
        WindowsUpdatePage(
            status = status,
            progress = progress,
            availableVersion = updateInfo?.versionName,
            notes = updateInfo?.notes,
            downloaded = downloaded != null,
            history = history,
            historyLoading = historyLoading,
            automaticChecks = automaticChecks,
            onAutomaticChecksChanged = { enabled ->
                automaticChecks = enabled
                prefs.edit().putBoolean(AUTO_UPDATE_CHECKS, enabled).apply()
                if (enabled) check() else status = "Automatic update checks are turned off. You can still check manually at any time."
            },
            onCheckAgain = { check(); loadHistory() },
            onDownloadAndInstall = {
                val found = updateInfo ?: return@WindowsUpdatePage
                progress = 0
                status = "Downloading update..."
                scope.launch {
                    val file = UpdateManager.downloadUpdate(context, found) { value -> progress = value }
                    if (file == null) {
                        progress = -1
                        status = "The update could not be downloaded. Try again later."
                    } else {
                        downloaded = file
                        progress = 100
                        status = "Download complete. Opening the Android installer..."
                        val opened = UpdateManager.installUpdate(context, file)
                        if (!opened) status = "Allow installs from Windroid XP, then return and tap Install update."
                    }
                }
            },
            onInstall = {
                downloaded?.let { file ->
                    val opened = UpdateManager.installUpdate(context, file)
                    if (!opened) status = "Allow installs from Windroid XP, then return and tap Install update again."
                }
            },
            onClose = onClose
        )
    }
}
