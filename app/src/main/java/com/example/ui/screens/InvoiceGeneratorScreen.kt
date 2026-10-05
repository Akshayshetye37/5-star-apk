package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.Booking
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.ui.components.UpiQrDialog
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.AppPreferences
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.PdfInvoiceGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoiceGeneratorScreen(
    viewModel: HotelViewModel,
    onViewInvoices: () -> Unit
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedBookingFromState by viewModel.selectedBookingForInvoice.collectAsStateWithLifecycle()
    var invoiceStyle by remember { mutableStateOf(AppPreferences.invoiceStyle(context)) }

    var selectedBooking by remember { mutableStateOf<Booking?>(selectedBookingFromState ?: bookings.firstOrNull()) }
    var isBookingDropdownExpanded by remember { mutableStateOf(false) }

    var invoiceNumber by remember {
        mutableStateOf("${settings.invoicePrefix}${SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date())}")
    }

    // Editable Items list
    val invoiceItems = remember { mutableStateListOf<InvoiceItem>() }

    // Discount, Tax, Payment method
    var discountText by remember { mutableStateOf("0") }
    var taxPercentageText by remember { mutableStateOf(settings.taxPercentage.toString()) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paymentMethodExpanded by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(settings.invoiceFooter) }

    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }

    // When booking selection changes, auto-load all booking details and items
    fun loadBooking(b: Booking) {
        selectedBooking = b
        invoiceItems.clear()

        // 1. Room Charges
        if (b.roomCharges > 0) {
            val nights = DateUtils.getDaysBetween(b.checkInTimestamp, b.checkOutTimestamp)
            invoiceItems.add(
                InvoiceItem(
                    invoiceNumber = invoiceNumber,
                    description = "Room Charges (${b.roomNumber} - ${b.roomType})",
                    quantity = nights,
                    unitPrice = b.roomCharges / nights.coerceAtLeast(1),
                    totalPrice = b.roomCharges
                )
            )
        }

        // 2. Initial food items from requirements:
        // Mineral Water, Ghavane Chatney, Tea, Kande Pohe, Other
        if (b.mineralWater > 0) {
            invoiceItems.add(InvoiceItem(invoiceNumber = invoiceNumber, description = "Mineral Water", quantity = 1, unitPrice = b.mineralWater, totalPrice = b.mineralWater))
        }
        if (b.ghavaneChatney > 0) {
            invoiceItems.add(InvoiceItem(invoiceNumber = invoiceNumber, description = "Ghavane Chatney", quantity = 1, unitPrice = b.ghavaneChatney, totalPrice = b.ghavaneChatney))
        }
        if (b.tea > 0) {
            invoiceItems.add(InvoiceItem(invoiceNumber = invoiceNumber, description = "Tea", quantity = 1, unitPrice = b.tea, totalPrice = b.tea))
        }
        if (b.kandePohe > 0) {
            invoiceItems.add(InvoiceItem(invoiceNumber = invoiceNumber, description = "Kande Pohe", quantity = 1, unitPrice = b.kandePohe, totalPrice = b.kandePohe))
        }
        if (b.otherCharges > 0) {
            invoiceItems.add(InvoiceItem(invoiceNumber = invoiceNumber, description = "Other Charges", quantity = 1, unitPrice = b.otherCharges, totalPrice = b.otherCharges))
        }

        discountText = b.discount.toString()
    }

    LaunchedEffect(selectedBookingFromState, bookings) {
        if (selectedBookingFromState != null) {
            loadBooking(selectedBookingFromState!!)
            viewModel.selectedBookingForInvoice.value = null
        } else if (selectedBooking == null && bookings.isNotEmpty()) {
            loadBooking(bookings.first())
        }
    }

    // Calculations
    val subtotal = invoiceItems.sumOf { it.totalPrice }
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val taxPercent = taxPercentageText.toDoubleOrNull() ?: 0.0
    val taxAmount = ((subtotal - discount).coerceAtLeast(0.0) * (taxPercent / 100.0))
    val grandTotal = (subtotal - discount + taxAmount).coerceAtLeast(0.0)
    val advance = selectedBooking?.advance ?: 0.0
    val paid = selectedBooking?.paid ?: 0.0
    val pending = (grandTotal - paid).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("invoice_generator_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Invoice Generator", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                    Text("Auto-populated from booking", fontSize = 12.sp, color = Slate500)
                }

                Button(
                    onClick = onViewInvoices,
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("View History", fontSize = 12.sp)
                }
            }
        }

        // --- 1. SELECT BOOKING ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Booking", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedBooking?.let { "${it.bookingId} — ${it.customerName} (Room ${it.roomNumber})" } ?: "Select Booking",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Booking") },
                            trailingIcon = {
                                IconButton(onClick = { isBookingDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { isBookingDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = isBookingDropdownExpanded,
                            onDismissRequest = { isBookingDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            bookings.forEach { b ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${b.bookingId} — ${b.customerName}", fontWeight = FontWeight.Bold)
                                            Text("Room ${b.roomNumber} (${b.roomType}) | Status: ${b.status}", fontSize = 12.sp, color = Slate500)
                                        }
                                    },
                                    onClick = {
                                        loadBooking(b)
                                        isBookingDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    selectedBooking?.let { b ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Guest: ${b.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Contact: ${b.contactNumber}", fontSize = 12.sp, color = Slate600)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Room ${b.roomNumber} (${b.roomType})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${b.checkInDate} → ${b.checkOutDate}", fontSize = 12.sp, color = Slate600)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. INVOICE META & ITEMS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Invoice Details & Items", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                        OutlinedButton(
                            onClick = {
                                invoiceItems.add(
                                    InvoiceItem(
                                        invoiceNumber = invoiceNumber,
                                        description = "Additional Service",
                                        quantity = 1,
                                        unitPrice = 0.0,
                                        totalPrice = 0.0
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Invoice Number *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Document Type", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Slate700)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
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

                    // Items list
                    invoiceItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = item.description,
                                onValueChange = { desc ->
                                    invoiceItems[index] = item.copy(description = desc)
                                },
                                label = { Text("Item") },
                                modifier = Modifier.weight(2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = item.totalPrice.toString(),
                                onValueChange = { priceStr ->
                                    val price = priceStr.toDoubleOrNull() ?: 0.0
                                    invoiceItems[index] = item.copy(unitPrice = price, totalPrice = price)
                                },
                                label = { Text(settings.currencySymbol) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = { invoiceItems.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                            }
                        }
                    }
                }
            }
        }

        // --- 3. TOTALS & PAYMENT SETTINGS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Taxes & Payment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("Discount (${settings.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = taxPercentageText,
                            onValueChange = { taxPercentageText = it },
                            label = { Text("Tax %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Payment Method selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payment Method") },
                            trailingIcon = {
                                IconButton(onClick = { paymentMethodExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { paymentMethodExpanded = true }
                        )
                        DropdownMenu(
                            expanded = paymentMethodExpanded,
                            onDismissRequest = { paymentMethodExpanded = false }
                        ) {
                            listOf("Cash", "UPI", "Google Pay", "Card", "Bank Transfer", "Other").forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        paymentMethod = method
                                        paymentMethodExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Financial calculations display
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", color = Slate600, fontSize = 13.sp)
                                Text(CurrencyFormatter.format(subtotal, settings.currencySymbol), fontWeight = FontWeight.Medium)
                            }
                            if (discount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount", color = Slate600, fontSize = 13.sp)
                                    Text("- ${CurrencyFormatter.format(discount, settings.currencySymbol)}", color = ErrorRed)
                                }
                            }
                            if (taxAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tax ($taxPercent%)", color = Slate600, fontSize = 13.sp)
                                    Text(CurrencyFormatter.format(taxAmount, settings.currencySymbol))
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate200))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Grand Total", fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 15.sp)
                                Text(CurrencyFormatter.format(grandTotal, settings.currencySymbol), fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 16.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Paid Amount", color = Slate600, fontSize = 13.sp)
                                Text(CurrencyFormatter.format(paid, settings.currencySymbol), color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Balance Due (Pending)", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)
                                Text(
                                    CurrencyFormatter.format(pending, settings.currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    color = if (pending > 0) ErrorRed else SuccessGreen,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. ACTION BUTTONS (Save, Generate PDF, Print, Share, UPI QR) ---
        item {
            val currentBooking = selectedBooking
            if (currentBooking != null) {
                val invoiceObject = Invoice(
                    invoiceNumber = invoiceNumber,
                    bookingId = currentBooking.bookingId,
                    invoiceDate = System.currentTimeMillis(),
                    customerId = currentBooking.customerId,
                    customerName = currentBooking.customerName,
                    customerContact = currentBooking.contactNumber,
                    roomNumber = currentBooking.roomNumber,
                    roomType = currentBooking.roomType,
                    checkInDate = currentBooking.checkInDate,
                    checkOutDate = currentBooking.checkOutDate,
                    guests = currentBooking.numberOfGuests,
                    roomCharges = currentBooking.roomCharges,
                    foodCharges = currentBooking.mineralWater + currentBooking.ghavaneChatney + currentBooking.tea + currentBooking.kandePohe,
                    otherCharges = currentBooking.otherCharges,
                    discount = discount,
                    tax = taxAmount,
                    grandTotal = grandTotal,
                    advance = currentBooking.advance,
                    paid = paid,
                    pending = pending,
                    paymentMethod = paymentMethod,
                    paymentStatus = if (pending <= 0.0) "PAID" else if (paid > 0) "PARTIALLY_PAID" else "PENDING",
                    notes = notes
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveInvoice(invoiceObject, invoiceItems.toList())
                            // Generate local PDF
                            val pdf = PdfInvoiceGenerator.generateInvoicePdf(
                                context = context,
                                settings = settings,
                                invoice = invoiceObject,
                                items = invoiceItems.toList()
                            )
                            generatedPdfFile = pdf
                            viewModel.showMessage("Invoice & PDF created successfully!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_and_generate_invoice_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Generate Local PDF", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    // PDF Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val pdf = generatedPdfFile ?: PdfInvoiceGenerator.generateInvoicePdf(
                                    context = context,
                                    settings = settings,
                                    invoice = invoiceObject,
                                    items = invoiceItems.toList()
                                )
                                PdfInvoiceGenerator.openInvoicePdf(context, pdf)
                            },
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open PDF", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val pdf = generatedPdfFile ?: PdfInvoiceGenerator.generateInvoicePdf(
                                    context = context,
                                    settings = settings,
                                    invoice = invoiceObject,
                                    items = invoiceItems.toList()
                                )
                                PdfInvoiceGenerator.shareInvoicePdf(context, pdf)
                            },
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val pdf = generatedPdfFile ?: PdfInvoiceGenerator.generateInvoicePdf(
                                    context = context,
                                    settings = settings,
                                    invoice = invoiceObject,
                                    items = invoiceItems.toList()
                                )
                                PdfInvoiceGenerator.printInvoicePdf(context, pdf)
                            },
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print", fontSize = 12.sp)
                        }
                    }

                    // UPI QR button
                    Button(
                        onClick = { showQrDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Show UPI / Google Pay QR Code", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showQrDialog) {
        UpiQrDialog(
            settings = settings,
            initialAmount = pending,
            title = "Pay Invoice $invoiceNumber",
            onDismiss = { showQrDialog = false }
        )
    }
}
