package com.example.ui.screens

import android.content.Intent
import android.net.Uri

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Payment
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.GuestRoomPdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentsScreen(
    viewModel: HotelViewModel
) {
    val context = LocalContext.current
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val activeBookings by viewModel.activeBookings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }
    var showPendingDialog by remember { mutableStateOf(false) }

    val filteredPayments = remember(payments, searchQuery) {
        if (searchQuery.isBlank()) payments
        else {
            payments.filter {
                it.paymentId.contains(searchQuery, ignoreCase = true) ||
                        it.bookingId.contains(searchQuery, ignoreCase = true) ||
                        it.customerName.contains(searchQuery, ignoreCase = true) ||
                        it.paymentMethod.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalCollected = remember(filteredPayments) { filteredPayments.sumOf { it.amount } }
    val pendingBookings = activeBookings.filter { it.pending > 0.0 }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = HotelNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_payment")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Payment")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("payments_screen")
        ) {
            // Search & Export
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by ID, guest, method...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = {
                        val file = CsvExporter.exportPaymentsToCsv(context, filteredPayments)
                        CsvExporter.shareCsvFile(context, file, "Payments Report")
                    }
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = HotelNavy)
                }
            }

            // Total summary bar
            Card(
                colors = CardDefaults.cardColors(containerColor = HotelNavy.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Payments Recorded", fontWeight = FontWeight.Medium, color = Slate700)
                    Text(
                        text = CurrencyFormatter.format(totalCollected, settings.currencySymbol),
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        fontSize = 16.sp
                    )
                }
            }

            OutlinedButton(
                onClick = { showPendingDialog = true },
                enabled = pendingBookings.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (pendingBookings.isEmpty()) "No Pending Balances"
                    else "Pending Balances • ${pendingBookings.size} Guest/Room${if (pendingBookings.size == 1) "" else "s"}"
                )
            }

            if (filteredPayments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No payments found.", color = Slate500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPayments, key = { it.paymentId }) { payment ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth().testTag("payment_card_${payment.paymentId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(payment.paymentId, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HotelNavy)
                                    Text("Booking: ${payment.bookingId.ifBlank { "Direct" }} — ${payment.customerName}", fontSize = 12.sp, color = Slate700)
                                    Text(
                                        "${DateUtils.formatDisplayDateTime(payment.date)} • Method: ${payment.paymentMethod}",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                    if (payment.referenceNumber.isNotBlank()) {
                                        Text("Ref: ${payment.referenceNumber}", fontSize = 11.sp, color = Slate500)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        CurrencyFormatter.format(payment.amount, settings.currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SuccessGreen
                                    )
                                    IconButton(onClick = { paymentToDelete = payment }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Payment Dialog
    if (showAddDialog) {
        RecordPaymentDialog(
            activeBookings = activeBookings,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showAddDialog = false },
            onSave = { p ->
                viewModel.recordPayment(p)
                showAddDialog = false
            }
        )
    }

    if (showPendingDialog) {
        AlertDialog(
            onDismissRequest = { showPendingDialog = false },
            title = { Text("Pending Balances", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(pendingBookings, key = { it.bookingId }) { b ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Room ${b.roomNumber} • ${b.customerName}", fontWeight = FontWeight.Bold, color = HotelNavy)
                                Text("Pending: ${CurrencyFormatter.format(b.pending, settings.currencySymbol)}", color = ErrorRed, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(b.contactNumber)}")))
                                    }, modifier = Modifier.weight(1f)) { Text("Call") }
                                    OutlinedButton(onClick = {
                                        val number = b.whatsappNumber.ifBlank { b.contactNumber }.filter { it.isDigit() }
                                        val text = "Hello ${b.customerName}, your pending hotel balance is ${CurrencyFormatter.format(b.pending, settings.currencySymbol)} for Room ${b.roomNumber}. Please complete the payment. Thank you."
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=${Uri.encode(text)}")))
                                    }, modifier = Modifier.weight(1f)) { Text("WhatsApp") }
                                    OutlinedButton(onClick = {
                                        viewModel.generateGuestRoomPdf(context, b) { file ->
                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                            val number = b.whatsappNumber.ifBlank { b.contactNumber }.filter { it.isDigit() }
                                            val text = "Pending balance reminder for Room ${b.roomNumber}: ${CurrencyFormatter.format(b.pending, settings.currencySymbol)}."
                                            context.startActivity(Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_TEXT, text)
                                                setPackage("com.whatsapp")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            })
                                        }
                                    }, modifier = Modifier.weight(1f)) { Text("PDF + WhatsApp") }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showPendingDialog = false }) { Text("Close") } }
        )
    }

    // Delete confirmation
    paymentToDelete?.let { p ->
        ConfirmDeleteDialog(
            title = "Delete Payment ${p.paymentId}?",
            message = "Are you sure you want to delete this payment of ${p.amount}? Booking paid balance will be adjusted.",
            onConfirm = {
                viewModel.deletePayment(p)
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null }
        )
    }
}

@Composable
fun RecordPaymentDialog(
    activeBookings: List<com.example.data.model.Booking>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Payment) -> Unit
) {
    var selectedBooking by remember { mutableStateOf(activeBookings.firstOrNull()) }
    var amountText by remember { mutableStateOf(if (selectedBooking != null) selectedBooking!!.pending.toString() else "") }
    var paymentMethod by remember { mutableStateOf("UPI") }
    var referenceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var isMethodExpanded by remember { mutableStateOf(false) }
    var isBookingExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Booking selection
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedBooking?.let { "Room ${it.roomNumber} — ${it.customerName} (Due: $currencySymbol ${it.pending})" } ?: "Select Booking",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Booking") },
                        trailingIcon = {
                            IconButton(onClick = { isBookingExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { isBookingExpanded = true }
                    )
                    DropdownMenu(expanded = isBookingExpanded, onDismissRequest = { isBookingExpanded = false }) {
                        activeBookings.forEach { b ->
                            DropdownMenuItem(
                                text = { Text("Room ${b.roomNumber} - ${b.customerName} (Pending: $currencySymbol ${b.pending})") },
                                onClick = {
                                    selectedBooking = b
                                    amountText = b.pending.toString()
                                    isBookingExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Method selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = {
                            IconButton(onClick = { isMethodExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { isMethodExpanded = true }
                    )
                    DropdownMenu(expanded = isMethodExpanded, onDismissRequest = { isMethodExpanded = false }) {
                        listOf("Cash", "UPI", "Google Pay", "Card", "Bank Transfer", "Other").forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    paymentMethod = m
                                    isMethodExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("UPI / Transaction Ref No.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) return@Button
                    val p = Payment(
                        paymentId = "PAY-${SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date())}",
                        bookingId = selectedBooking?.bookingId ?: "",
                        customerId = selectedBooking?.customerId ?: "",
                        customerName = selectedBooking?.customerName ?: "",
                        amount = amount,
                        date = System.currentTimeMillis(),
                        paymentMethod = paymentMethod,
                        referenceNumber = referenceNumber.trim(),
                        notes = notes.trim()
                    )
                    onSave(p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
