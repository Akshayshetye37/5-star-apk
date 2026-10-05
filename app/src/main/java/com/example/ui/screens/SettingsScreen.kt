package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.data.model.HotelSettings
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.AppPreferences

@Composable
fun SettingsScreen(
    viewModel: HotelViewModel,
    onNavigateToManageSections: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val currentSettings by viewModel.settings.collectAsStateWithLifecycle()

    var hotelName by remember(currentSettings) { mutableStateOf(currentSettings.hotelName) }
    var slogan by remember(currentSettings) { mutableStateOf(currentSettings.slogan) }
    var logoUri by remember(currentSettings) { mutableStateOf(currentSettings.logoUri) }
    var manualQrUri by remember(currentSettings) { mutableStateOf(currentSettings.manualQrUri) }
    var localBackupEnabled by remember(currentSettings) { mutableStateOf(currentSettings.localBackupEnabled) }
    var driveBackupEnabled by remember(currentSettings) { mutableStateOf(currentSettings.driveBackupEnabled) }
    var driveFolderUri by remember(currentSettings) { mutableStateOf(currentSettings.driveFolderUri) }
    var backupSensitiveData by remember(currentSettings) { mutableStateOf(currentSettings.backupSensitiveData) }
    val context = LocalContext.current
    var address by remember(currentSettings) { mutableStateOf(currentSettings.address) }
    var phone by remember(currentSettings) { mutableStateOf(currentSettings.phone) }
    var whatsapp by remember(currentSettings) { mutableStateOf(currentSettings.whatsapp) }
    var email by remember(currentSettings) { mutableStateOf(currentSettings.email) }
    var gstNumber by remember(currentSettings) { mutableStateOf(currentSettings.gstNumber) }
    var upiId by remember(currentSettings) { mutableStateOf(currentSettings.upiId) }
    var upiPayeeName by remember(currentSettings) { mutableStateOf(currentSettings.upiPayeeName) }
    var upiPaymentAnnouncementEnabled by remember(currentSettings) {
        mutableStateOf(currentSettings.upiPaymentAnnouncementEnabled)
    }
    var currencySymbol by remember(currentSettings) { mutableStateOf(currentSettings.currencySymbol) }
    var invoicePrefix by remember(currentSettings) { mutableStateOf(currentSettings.invoicePrefix) }
    var invoiceFooter by remember(currentSettings) { mutableStateOf(currentSettings.invoiceFooter) }
    var termsAndConditions by remember(currentSettings) { mutableStateOf(currentSettings.termsAndConditions) }
    var taxPercentageText by remember(currentSettings) { mutableStateOf(currentSettings.taxPercentage.toString()) }
    var ocrEnabled by remember { mutableStateOf(AppPreferences.ocrEnabled(context)) }
    var compactUi by remember { mutableStateOf(AppPreferences.compactUi(context)) }
    var darkMode by remember { mutableStateOf(AppPreferences.darkMode(context)) }
    var invoiceStyle by remember { mutableStateOf(AppPreferences.invoiceStyle(context)) }
    var exportFromDate by remember { mutableStateOf("") }
    var exportToDate by remember { mutableStateOf("") }
    var exportBookingIds by remember { mutableStateOf("") }
    var exportCustomerName by remember { mutableStateOf("") }
    var groupMatchingWorkbookRows by remember { mutableStateOf(true) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val copied = viewModel.copyImageToInternalStorage(it)
            logoUri = copied.ifBlank { it.toString() }
        }
    }
    val qrPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val copied = viewModel.copyImageToInternalStorage(it)
            manualQrUri = copied.ifBlank { it.toString() }
        }
    }
    val driveFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) { }
            driveFolderUri = it.toString()
            driveBackupEnabled = true
        }
    }

    val workbookPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            viewModel.importHotelWorkbook(
                context = context,
                uri = it,
                groupMatchingStays = groupMatchingWorkbookRows
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Hotel Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                Text("Configure billing, hotel info, UPI QR, and terms", fontSize = 12.sp, color = Slate500)
            }
        }

        // --- QUICK NAVIGATION TILES ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsActionCard(
                    title = "Manage Sections",
                    subtitle = "Customize tabs & menus",
                    icon = Icons.Default.ViewCarousel,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToManageSections
                )
                SettingsActionCard(
                    title = "Backup & Restore",
                    subtitle = "Database JSON backup",
                    icon = Icons.Default.Backup,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToBackup
                )
            }
        }

        // --- 1. HOTEL IDENTITY ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hotel Identification & Branding", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)

                    OutlinedTextField(
                        value = hotelName,
                        onValueChange = { hotelName = it },
                        label = { Text("Hotel Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("hotel_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = slogan,
                        onValueChange = { slogan = it },
                        label = { Text("Slogan / Tagline (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (logoUri.isNotBlank()) {
                            AsyncImage(
                                model = logoUri,
                                contentDescription = "Hotel Logo",
                                modifier = Modifier.size(72.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(72.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("LOGO", fontSize = 10.sp, color = Slate500) }
                        }
                        OutlinedButton(onClick = { logoPicker.launch("image/*") }) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Hotel Logo")
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Hotel Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Phone") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gstNumber,
                            onValueChange = { gstNumber = it },
                            label = { Text("GST Number (Optional)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // --- 2. UPI PAYMENT & QR CONFIGURATION ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("UPI & Google Pay Configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
                    Text("Required to generate local upi://pay QR codes on invoices", fontSize = 12.sp, color = Slate500)

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI ID (e.g. yourname@okhdfcbank)") },
                        modifier = Modifier.fillMaxWidth().testTag("upi_id_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upiPayeeName,
                        onValueChange = { upiPayeeName = it },
                        label = { Text("Payee Name (as registered with bank/UPI)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "UPI Payment Announcement",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Slate800
                            )
                            Text(
                                "Announce the verified received amount after payment confirmation.",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                        Switch(
                            checked = upiPaymentAnnouncementEnabled,
                            onCheckedChange = { upiPaymentAnnouncementEnabled = it }
                        )
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (manualQrUri.isNotBlank()) {
                            AsyncImage(
                                model = manualQrUri,
                                contentDescription = "Manual payment QR",
                                modifier = Modifier.size(110.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(110.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("QR IMAGE", fontSize = 10.sp, color = Slate500) }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Manual QR image", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                            Text("Used when you want a fixed QR. Amount QR uses the UPI ID above.", fontSize = 11.sp, color = Slate500)
                            OutlinedButton(onClick = { qrPicker.launch("image/*") }) {
                                Icon(Icons.Default.QrCode, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add QR Image")
                            }
                        }
                    }
                }
            }
        }

        // --- 3. BACKUP SETTINGS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Automatic Backup", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
                    Text("Backups are refreshed after database changes. Google Drive uses the folder you select.", fontSize = 11.sp, color = Slate500)

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local device backup", fontWeight = FontWeight.SemiBold, color = Slate700)
                            Text("Keep an offline backup on this phone", fontSize = 11.sp, color = Slate500)
                        }
                        Switch(checked = localBackupEnabled, onCheckedChange = { localBackupEnabled = it })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Drive backup", fontWeight = FontWeight.SemiBold, color = Slate700)
                            Text(if (driveFolderUri.isBlank()) "Select a Drive folder first" else "Drive folder connected", fontSize = 11.sp, color = Slate500)
                        }
                        Switch(checked = driveBackupEnabled, onCheckedChange = { driveBackupEnabled = it })
                    }
                    OutlinedButton(
                        onClick = { driveFolderPicker.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (driveFolderUri.isBlank()) "Choose Google Drive Folder" else "Change Backup Folder")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Back up sensitive ID data", fontWeight = FontWeight.SemiBold, color = Slate700)
                            Text("Includes ID numbers and ID photos when enabled", fontSize = 11.sp, color = Slate500)
                        }
                        Switch(checked = backupSensitiveData, onCheckedChange = { backupSensitiveData = it })
                    }
                }
            }
        }


        // --- DATA IMPORT / EXPORT ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Import / Export Workbook", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
                    Text(
                        "Use the hotel's workbook structure. Import reads the Booking sheet by header name, so column position can change safely.",
                        fontSize = 11.sp, color = Slate500
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = exportFromDate,
                            onValueChange = { exportFromDate = it },
                            label = { Text("From (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = exportToDate,
                            onValueChange = { exportToDate = it },
                            label = { Text("To (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = exportCustomerName,
                        onValueChange = { exportCustomerName = it },
                        label = { Text("Guest Name (optional)") },
                        supportingText = { Text("Case-insensitive; filters exported bookings by guest name.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = exportBookingIds,
                        onValueChange = { exportBookingIds = it },
                        label = { Text("Booking IDs (optional, comma separated)") },
                        supportingText = { Text("Blank = export all matching the date range") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Group matching workbook rows", fontWeight = FontWeight.SemiBold, color = Slate700)
                            Text(
                                "Same room + dates + contact become one booking with multiple guests.",
                                fontSize = 11.sp, color = Slate500
                            )
                        }
                        Switch(
                            checked = groupMatchingWorkbookRows,
                            onCheckedChange = { groupMatchingWorkbookRows = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { workbookPicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/vnd.ms-excel")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Import Excel")
                        }
                        Button(
                            onClick = {
                                val ids = exportBookingIds.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .toSet()
                                viewModel.exportHotelWorkbook(
                                    context,
                                    exportFromDate.trim().ifBlank { null },
                                    exportToDate.trim().ifBlank { null },
                                    ids,
                                    exportCustomerName.trim().ifBlank { null }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Export Excel")
                        }
                    }

                    Text(
                        "Export supports: all records, one booking ID, multiple booking IDs, or a From/To date range. Excel is import-compatible.",
                        fontSize = 11.sp, color = Slate500
                    )
                }
            }
        }

        // --- 3. BILLING & INVOICE SETTINGS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Invoicing & Currency", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { currencySymbol = it },
                            label = { Text("Currency Symbol") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = invoicePrefix,
                            onValueChange = { invoicePrefix = it },
                            label = { Text("Invoice Prefix") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = taxPercentageText,
                            onValueChange = { taxPercentageText = it },
                            label = { Text("Default Tax %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = invoiceFooter,
                        onValueChange = { invoiceFooter = it },
                        label = { Text("Invoice Footer Message") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = termsAndConditions,
                        onValueChange = { termsAndConditions = it },
                        label = { Text("Terms & Conditions") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            }
        }

        // --- 5. APP CUSTOMIZATION ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Application Customization", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
                    Text(
                        "Control the look, speed-sensitive features, OCR and invoice presentation. Changes are saved on this device.",
                        fontSize = 11.sp, color = Slate500
                    )

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Smart ID OCR", fontWeight = FontWeight.SemiBold, color = Slate700)
                            Text("Read name, ID number and document type from a photo on-device.", fontSize = 11.sp, color = Slate500)
                        }
                        Switch(
                            checked = ocrEnabled,
                            onCheckedChange = {
                                ocrEnabled = it
                                AppPreferences.setOcrEnabled(context, it)
                            }
                        )
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Compact / fast UI", fontWeight = FontWeight.SemiBold, color = Slate700)
                            Text("Use tighter cards and spacing for larger datasets.", fontSize = 11.sp, color = Slate500)
                        }
                        Switch(
                            checked = compactUi,
                            onCheckedChange = {
                                compactUi = it
                                AppPreferences.setCompactUi(context, it)
                            }
                        )
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Dark mode", fontWeight = FontWeight.SemiBold, color = Slate700)
                            Text("Applied the next time the application starts.", fontSize = 11.sp, color = Slate500)
                        }
                        Switch(
                            checked = darkMode,
                            onCheckedChange = {
                                darkMode = it
                                AppPreferences.setDarkMode(context, it)
                            }
                        )
                    }

                    Text("Default invoice style", fontWeight = FontWeight.SemiBold, color = Slate700)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CLASSIC", "RECEIPT", "PROFORMA", "ESTIMATE").forEach { style ->
                            OutlinedButton(
                                onClick = {
                                    invoiceStyle = style
                                    AppPreferences.setInvoiceStyle(context, style)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(style.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 4. SAVE BUTTON ---
        item {
            Button(
                onClick = {
                    val updated = currentSettings.copy(
                        hotelName = hotelName.trim(),
                        slogan = slogan.trim(),
                        logoUri = logoUri.trim(),
                        manualQrUri = manualQrUri.trim(),
                        localBackupEnabled = localBackupEnabled,
                        driveBackupEnabled = driveBackupEnabled,
                        driveFolderUri = driveFolderUri,
                        backupSensitiveData = backupSensitiveData,
                        address = address.trim(),
                        phone = phone.trim(),
                        whatsapp = whatsapp.trim(),
                        email = email.trim(),
                        gstNumber = gstNumber.trim(),
                        upiId = upiId.trim(),
                        upiPayeeName = upiPayeeName.trim(),
                        upiPaymentAnnouncementEnabled = upiPaymentAnnouncementEnabled,
                        currencySymbol = currencySymbol.trim(),
                        invoicePrefix = invoicePrefix.trim(),
                        invoiceFooter = invoiceFooter.trim(),
                        termsAndConditions = termsAndConditions.trim(),
                        taxPercentage = taxPercentageText.toDoubleOrNull() ?: 0.0
                    )
                    viewModel.updateSettings(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Hotel Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = HotelNavy, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
            Text(subtitle, fontSize = 11.sp, color = Slate500)
        }
    }
}
