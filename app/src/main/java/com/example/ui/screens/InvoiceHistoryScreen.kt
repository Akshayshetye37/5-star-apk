package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.Invoice
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusChip
import com.example.ui.components.UpiQrDialog
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.PdfInvoiceGenerator
import kotlinx.coroutines.launch

@Composable
fun InvoiceHistoryScreen(
    viewModel: HotelViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }
    var invoiceForQr by remember { mutableStateOf<Invoice?>(null) }
    var selectedInvoiceForPreview by remember { mutableStateOf<Invoice?>(null) }

    val filteredInvoices = remember(invoices, searchQuery, selectedFilter) {
        invoices.filter { inv ->
            val matchesFilter = when (selectedFilter) {
                "PAID" -> inv.paymentStatus == "PAID"
                "PARTIALLY_PAID" -> inv.paymentStatus == "PARTIALLY_PAID"
                "PENDING" -> inv.paymentStatus == "PENDING"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    inv.bookingId.contains(searchQuery, ignoreCase = true) ||
                    inv.customerName.contains(searchQuery, ignoreCase = true) ||
                    inv.roomNumber.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("invoice_history_screen")
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
                    placeholder = { Text("Search invoice, guest, room...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = {
                        val file = CsvExporter.exportInvoicesToCsv(context, filteredInvoices)
                        CsvExporter.shareCsvFile(context, file, "Hotel Invoices Export")
                    }
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = HotelNavy)
                }
            }

            // Filters
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("ALL" to "All", "PAID" to "Paid", "PARTIALLY_PAID" to "Partially Paid", "PENDING" to "Pending")
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            if (filteredInvoices.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No invoices found matching '$searchQuery'." else "No invoices recorded yet.",
                        color = Slate500
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredInvoices, key = { it.invoiceNumber }) { inv ->
                        InvoiceItemCard(
                            invoice = inv,
                            currencySymbol = settings.currencySymbol,
                            onOpenPdf = {
                                scope.launch {
                                    val items = viewModel.repository.getItemsForInvoiceDirect(inv.invoiceNumber)
                                    val pdf = PdfInvoiceGenerator.generateInvoicePdf(context, settings, inv, items)
                                    PdfInvoiceGenerator.openInvoicePdf(context, pdf)
                                }
                            },
                            onSharePdf = {
                                scope.launch {
                                    val items = viewModel.repository.getItemsForInvoiceDirect(inv.invoiceNumber)
                                    val pdf = PdfInvoiceGenerator.generateInvoicePdf(context, settings, inv, items)
                                    PdfInvoiceGenerator.shareInvoicePdf(context, pdf)
                                }
                            },
                            onPrint = {
                                scope.launch {
                                    val items = viewModel.repository.getItemsForInvoiceDirect(inv.invoiceNumber)
                                    val pdf = PdfInvoiceGenerator.generateInvoicePdf(context, settings, inv, items)
                                    PdfInvoiceGenerator.printInvoicePdf(context, pdf)
                                }
                            },
                            onShowQr = { invoiceForQr = inv },
                            onDelete = { invoiceToDelete = inv }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation
    invoiceToDelete?.let { inv ->
        ConfirmDeleteDialog(
            title = "Delete Invoice ${inv.invoiceNumber}?",
            message = "Are you sure you want to delete this invoice? The linked booking ${inv.bookingId} will not be deleted.",
            onConfirm = {
                viewModel.deleteInvoice(inv)
                invoiceToDelete = null
            },
            onDismiss = { invoiceToDelete = null }
        )
    }

    // QR Dialog
    invoiceForQr?.let { inv ->
        UpiQrDialog(
            settings = settings,
            initialAmount = inv.pending,
            title = "UPI Payment for ${inv.invoiceNumber}",
            onDismiss = { invoiceForQr = null }
        )
    }
}

@Composable
fun InvoiceItemCard(
    invoice: Invoice,
    currencySymbol: String,
    onOpenPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onPrint: () -> Unit,
    onShowQr: () -> Unit,
    onDelete: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("invoice_card_${invoice.invoiceNumber}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(invoice.invoiceNumber, fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 15.sp)
                    Text("Booking: ${invoice.bookingId}", fontSize = 12.sp, color = Slate500)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = invoice.paymentStatus)
                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Open PDF") },
                                onClick = {
                                    isMenuExpanded = false
                                    onOpenPdf()
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share PDF") },
                                onClick = {
                                    isMenuExpanded = false
                                    onSharePdf()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Print Invoice") },
                                onClick = {
                                    isMenuExpanded = false
                                    onPrint()
                                },
                                leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Show UPI QR") },
                                onClick = {
                                    isMenuExpanded = false
                                    onShowQr()
                                },
                                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Invoice", color = ErrorRed) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Guest: ${invoice.customerName} | Room ${invoice.roomNumber}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate800)
            Text("Date: ${DateUtils.formatDisplayDate(invoice.invoiceDate)}", fontSize = 12.sp, color = Slate600)

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total: ${CurrencyFormatter.format(invoice.grandTotal, currencySymbol)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Paid: ${CurrencyFormatter.format(invoice.paid, currencySymbol)}", fontSize = 12.sp, color = SuccessGreen)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Balance Due", fontSize = 11.sp, color = Slate500)
                    Text(
                        CurrencyFormatter.format(invoice.pending, currencySymbol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (invoice.pending > 0) ErrorRed else SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenPdf,
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onSharePdf,
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 12.sp)
                }
                if (invoice.pending > 0) {
                    Button(
                        onClick = onShowQr,
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QR Pay", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
