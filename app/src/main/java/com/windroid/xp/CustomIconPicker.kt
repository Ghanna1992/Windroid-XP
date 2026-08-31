package com.windroid.xp

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomIconPicker(
    context: Context,
    onSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    var revision by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var manageId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val result = CustomIconLibrary.importUris(context, uris)
            status = importMessage(result)
            revision++
        }
    }
    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = CustomIconLibrary.importUris(context, listOf(uri))
            status = importMessage(result)
            revision++
        }
    }

    val files = remember(revision) { CustomIconLibrary.list(context) }

    Column {
        Text("My Icons", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("Import your own images or a ZIP. Icons are copied into Windroid XP and survive app updates.", fontSize = 9.sp, color = Color(0xFF666666))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallXpButton("Import Images") { imagePicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }
            SmallXpButton("Import ZIP") { zipPicker.launch(arrayOf("application/zip", "application/x-zip-compressed")) }
        }
        if (status.isNotBlank()) Text(status, fontSize = 9.sp, color = Color(0xFF555555), modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(7.dp))
        Column(Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
            if (files.isEmpty()) {
                Text("No custom icons yet.", fontSize = 11.sp, color = Color(0xFF666666), modifier = Modifier.padding(10.dp))
            }
            files.forEach { file ->
                val id = CustomIconLibrary.idFor(file)
                val image = remember(revision, file.absolutePath) { CustomIconLibrary.load(file) }
                Row(Modifier.fillMaxWidth().clickable { onSelected(id) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Color.White).border(1.dp, Color(0xFFB7B7B7)), contentAlignment = Alignment.Center) {
                        if (image != null) Image(bitmap = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(file.name, modifier = Modifier.weight(1f), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Manage", color = Color(0xFF003399), fontSize = 9.sp, modifier = Modifier.clickable { manageId = id; renameText = file.nameWithoutExtension }.padding(6.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        SmallXpButton("Back") { onBack() }
    }

    manageId?.let { id ->
        androidx.compose.ui.window.Popup(
            alignment = Alignment.Center,
            onDismissRequest = { manageId = null },
            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
        ) {
            Column(Modifier.width(260.dp).background(Color(0xFFF5F4EA)).border(1.dp, Color(0xFF7F9DB9)).padding(10.dp)) {
                Text("Manage Icon", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(7.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(80) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(7.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallXpButton("Rename") {
                        val renamed = CustomIconLibrary.rename(context, id, renameText)
                        status = if (renamed != null) "Icon renamed." else "Could not rename icon."
                        manageId = null
                        revision++
                    }
                    SmallXpButton("Delete") {
                        status = if (CustomIconLibrary.delete(context, id)) "Icon deleted." else "Could not delete icon."
                        manageId = null
                        revision++
                    }
                    SmallXpButton("Cancel") { manageId = null }
                }
            }
        }
    }
}

private fun importMessage(result: CustomIconLibrary.ImportResult): String = buildString {
    append("Imported ${result.imported}")
    if (result.skipped > 0) append(" • skipped ${result.skipped}")
    result.error?.let { append(" • $it") }
}

@Composable
private fun SmallXpButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.background(Color(0xFFECE9D8)).border(1.dp, Color(0xFF7F9DB9)).clickable { onClick() }.padding(horizontal = 9.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 10.sp, color = Color.Black) }
}
