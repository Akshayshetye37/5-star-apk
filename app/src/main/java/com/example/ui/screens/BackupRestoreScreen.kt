package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.FileInputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.BackupRestoreManager
import com.example.util.DateUtils
import java.io.File

@Composable
fun BackupRestoreScreen(
    viewModel: HotelViewModel
) {
    val context = LocalContext.current
    val backups by viewModel.backups.collectAsStateWithLifecycle()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var selectedJsonToRestore by remember { mutableStateOf<String?>(null) }
    var lastBackupCreated by remember { mutableStateOf<File?>(null) }
    val exportTypes = listOf("Bookings", "Guests", "Rooms", "Payments", "Invoices", "Reservations", "Food", "Expenses")
    var selectedExports by remember { mutableStateOf(exportTypes.toSet()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val input = context.contentResolver.openInputStream(it)
                val json = input?.bufferedReader()?.use { reader -> reader.readText() }
                if (!json.isNullOrBlank()) {
                    selectedJsonToRestore = json
                    showRestoreConfirmDialog = true
                }
            } catch (e: Exception) {
                viewModel.showMessage("Failed to read backup file: ${e.message}")
            }
        }
    }

    var pendingBackupFile by remember { mutableStateOf<File?>(null) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val source = pendingBackupFile
        pendingBackupFile = null
        if (uri == null || source == null) return@rememberLauncherForActivityResult

        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(source).use { input -> input.copyTo(output) }
            } ?: throw IllegalStateException("Unable to open destination")
            viewModel.showMessage("Backup saved successfully.")
        } catch (e: Exception) {
            viewModel.showMessage("Failed to save backup: ${e.message}")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("backup_restore_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text("Backup & Restore Data", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                Text(
                    "100% offline local backup of rooms, bookings, customers, invoices, payments, food & settings.",
                    fontSize = 12.sp,
                    color = Slate500
                )
            }
        }

        // --- 1. CREATE BACKUP CARD ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(HotelNavy.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = HotelNavy)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Create Full Database Backup", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
                            Text("Choose a phone folder or Google Drive to save the backup JSON", fontSize = 12.sp, color = Slate500)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.createBackup { file ->
                                lastBackupCreated = file
                                pendingBackupFile = file
                                saveBackupLauncher.launch(file.name)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("create_backup_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate & Save Backup File", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.createBackup { file ->
                                lastBackupCreated = file
                                BackupRestoreManager.shareBackupFile(context, file)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Backup")
                    }

                    if (lastBackupCreated != null) {
                        Text(
                            text = "Last generated: ${lastBackupCreated?.name}",
                            fontSize = 11.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- 2. DATA EXPORT ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Export Data", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
                    Text("Choose which records to export. Excel and Word use one row/column sheet/table.", fontSize = 12.sp, color = Slate500)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { selectedExports = exportTypes.toSet() }, modifier = Modifier.weight(1f)) { Text("Select All") }
                        OutlinedButton(onClick = { selectedExports = emptySet() }, modifier = Modifier.weight(1f)) { Text("Clear") }
                    }
                    exportTypes.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            pair.forEach { type ->
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = type in selectedExports,
                                        onCheckedChange = { checked ->
                                            selectedExports = if (checked) selectedExports + type else selectedExports - type
                                        }
                                    )
                                    Text(type, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.exportData(context, selectedExports, "XLSX") },
                            enabled = selectedExports.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TableView, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Excel")
                        }
                        Button(
                            onClick = { viewModel.exportData(context, selectedExports, "DOCX") },
                            enabled = selectedExports.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Word")
                        }
                        Button(
                            onClick = { viewModel.exportData(context, selectedExports, "PDF") },
                            enabled = selectedExports.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("PDF") }
                    }
                }
            }
        }

        // --- 2. RESTORE CARD ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF0284C7).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFF0284C7))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Restore from Backup File", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
                            Text("Load previously backed-up data into the system", fontSize = 12.sp, color = Slate500)
                        }
                    }

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("application/json") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("restore_backup_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup JSON File to Restore", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // --- 3. RECENT LOCAL BACKUPS ---
        item {
            Text("Backup History", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
            Spacer(modifier = Modifier.height(4.dp))
            if (backups.isEmpty()) {
                Text("No previous backup records on this device.", fontSize = 13.sp, color = Slate500)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    backups.forEach { b ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(b.backupId, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                                    Text(DateUtils.formatDisplayDateTime(b.timestamp), fontSize = 11.sp, color = Slate500)
                                    Text("Records: ${b.recordsCount} items", fontSize = 11.sp, color = Slate600)
                                }
                                Icon(Icons.Default.Backup, contentDescription = null, tint = SuccessGreen)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog before restore (Requirement 27)
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                selectedJsonToRestore = null
            },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Confirm Database Restore?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Restoring this backup may replace current application data. This will reload the database and refresh your dashboard. Continue?",
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = selectedJsonToRestore
                        if (json != null) {
                            viewModel.restoreBackup(json) { success, msg ->
                                showRestoreConfirmDialog = false
                                selectedJsonToRestore = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Yes, Restore Data")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        selectedJsonToRestore = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
