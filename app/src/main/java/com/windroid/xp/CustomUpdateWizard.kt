package com.windroid.xp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomUpdateWizard(
    versionName: String,
    onCancel: () -> Unit,
    onInstall: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var critical by remember { mutableStateOf(true) }
    var recommended by remember { mutableStateOf(true) }
    var drivers by remember { mutableStateOf(false) }
    var backupFiles by remember { mutableStateOf(true) }
    var createRestorePoint by remember { mutableStateOf(true) }
    var deliveryMode by remember { mutableStateOf("Background Intelligent Transfer Service") }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(
            Modifier.fillMaxWidth().height(55.dp)
                .background(Brush.horizontalGradient(listOf(Color(0xFFFFF7CB), Color(0xFFFFE99C), Color(0xFFF3B35C))))
                .padding(horizontal = 17.dp)
        ) {
            Text(
                when (step) {
                    0 -> "Custom Installation"
                    1 -> "Select update categories"
                    2 -> "Installation preferences"
                    3 -> "Download options"
                    else -> "Review and install updates"
                },
                fontSize = 22.sp,
                color = Color(0xFF3A3A3A),
                modifier = Modifier.align(Alignment.CenterStart)
            )
            WindroidBrandAsset(
                wordmark = true,
                modifier = Modifier.align(Alignment.CenterEnd).width(118.dp).height(34.dp)
            )
        }

        Column(Modifier.weight(1f).fillMaxWidth().padding(20.dp)) {
            when (step) {
                0 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WindroidBrandAsset(wordmark = false, modifier = Modifier.size(54.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Windows Update is preparing a custom installation for Windroid XP $versionName.", fontSize = 12.sp, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Custom installation lets you review completely unnecessary options before installing the same update package.", fontSize = 10.sp, color = Color(0xFF555555))
                    Spacer(Modifier.height(18.dp))
                    Text("Estimated download size: whatever GitHub says it is", fontSize = 10.sp)
                    Text("Estimated installation time: optimistic", fontSize = 10.sp)
                }
                1 -> {
                    Text("Choose the categories of updates you want Windows Update to consider.", fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    WizardCheck("Critical Updates and Service Packs", critical) { critical = it }
                    WizardCheck("Recommended Updates", recommended) { recommended = it }
                    WizardCheck("Hardware and Driver Updates", drivers) { drivers = it }
                    Spacer(Modifier.height(10.dp))
                    Text("These selections do not change the update package. They are here because 2005 demanded paperwork.", fontSize = 9.sp, color = Color(0xFF777777))
                }
                2 -> {
                    Text("Choose installation preferences.", fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    WizardCheck("Back up files before installation", backupFiles) { backupFiles = it }
                    WizardCheck("Create a System Restore point", createRestorePoint) { createRestorePoint = it }
                    WizardCheck("Notify me before restarting", true) { }
                    Spacer(Modifier.height(12.dp))
                    Text("Windroid XP will not modify Android restore settings or create a real restore point.", fontSize = 9.sp, color = Color(0xFF777777))
                }
                3 -> {
                    Text("Select how Windows Update should obtain the update files.", fontSize = 11.sp)
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "Background Intelligent Transfer Service",
                        "Download updates immediately",
                        "Use idle network bandwidth",
                        "Ask the nearest Dell Dimension"
                    ).forEach { option ->
                        WizardRadio(option, deliveryMode == option) { deliveryMode = option }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("All options ultimately use the same signed Windroid XP release download.", fontSize = 9.sp, color = Color(0xFF777777))
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WindroidBrandAsset(wordmark = false, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Windows Update is ready to install the following update:", fontSize = 11.sp)
                            Text("Windroid XP $versionName", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF174A8B))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Selected classifications:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(listOfNotNull(if (critical) "Critical" else null, if (recommended) "Recommended" else null, if (drivers) "Drivers" else null).ifEmpty { listOf("None, somehow") }.joinToString(", "), fontSize = 10.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("Transfer method: $deliveryMode", fontSize = 10.sp)
                    Text("Back up files: ${if (backupFiles) "Yes" else "No"}", fontSize = 10.sp)
                    Text("Create restore point: ${if (createRestorePoint) "Yes" else "No"}", fontSize = 10.sp)
                    Spacer(Modifier.height(15.dp))
                    Text("Click Install Updates to begin. Your carefully chosen options will now be respectfully ignored.", fontSize = 10.sp, color = Color(0xFF555555))
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color(0xFFF3F3F1)).border(1.dp, Color(0xFFD0D0D0)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClassicWizardButton("Cancel", onCancel)
            Spacer(Modifier.weight(1f))
            if (step > 0) {
                ClassicWizardButton("< Back") { step-- }
                Spacer(Modifier.width(8.dp))
            }
            if (step < 4) ClassicWizardButton("Next >") { step++ }
            else ClassicWizardButton("Install Updates", onInstall)
        }
    }
}

@Composable
private fun WizardCheck(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChanged(!checked) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(17.dp).background(Color.White).border(1.dp, Color(0xFF777777)), contentAlignment = Alignment.Center) {
            if (checked) Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3F80))
        }
        Spacer(Modifier.width(9.dp))
        Text(label, fontSize = 10.sp)
    }
}

@Composable
private fun WizardRadio(label: String, selected: Boolean, onSelected: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onSelected() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(17.dp).background(Color.White).border(1.dp, Color(0xFF777777)), contentAlignment = Alignment.Center) {
            if (selected) Text("●", fontSize = 11.sp, color = Color(0xFF1A3F80))
        }
        Spacer(Modifier.width(9.dp))
        Text(label, fontSize = 10.sp)
    }
}

@Composable
private fun ClassicWizardButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.background(Brush.verticalGradient(listOf(Color.White, Color(0xFFD7E5F6))))
            .border(1.dp, Color(0xFF5D718B)).clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF344A63))
    }
}
