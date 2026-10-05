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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Booking
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusChip
import com.example.ui.components.UpiQrDialog
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HotelGold
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

@Composable
fun BookingsListScreen(
    viewModel: HotelViewModel,
    onNavigateToNewBooking: () -> Unit,
    onGenerateInvoice: (Booking) -> Unit,
    onOpenDetails: (Booking) -> Unit = {}
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    var bookingToDelete by remember { mutableStateOf<Booking?>(null) }
    var bookingForQr by remember { mutableStateOf<Booking?>(null) }

    val filteredBookings = remember(bookings, searchQuery, selectedFilter) {
        bookings.filter { b ->
            val matchesFilter = when (selectedFilter) {
                "ACTIVE" -> b.status == "ACTIVE"
                "CHECKED_OUT" -> b.status == "CHECKED_OUT"
                "CANCELLED" -> b.status == "CANCELLED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    b.bookingId.contains(searchQuery, ignoreCase = true) ||
                    b.customerName.contains(searchQuery, ignoreCase = true) ||
                    b.contactNumber.contains(searchQuery, ignoreCase = true) ||
                    b.roomNumber.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.editingBooking.value = null
                    onNavigateToNewBooking()
                },
                containerColor = HotelNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_booking")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Booking")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("bookings_screen")
        ) {
            // Top search & Export row
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
                    placeholder = { Text("Search by ID, guest, phone, room...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_bookings_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = {
                        val file = CsvExporter.exportBookingsToCsv(context, filteredBookings)
                        CsvExporter.shareCsvFile(context, file, "Hotel Bookings Export")
                    },
                    modifier = Modifier.testTag("export_bookings_csv_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = HotelNavy)
                }
            }

            // Live booking totals. These are calculated from the Room-backed booking
            // stream, so creating/checking-out/cancelling a booking updates the counts
            // without a manual refresh.
            val allBookingCount = bookings.size
            val activeBookingCount = bookings.count { it.status == "ACTIVE" }
            val checkedOutBookingCount = bookings.count { it.status == "CHECKED_OUT" }
            val cancelledBookingCount = bookings.count { it.status == "CANCELLED" }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookingCountCard("All", allBookingCount, Modifier.weight(1f))
                BookingCountCard("Active", activeBookingCount, Modifier.weight(1f))
                BookingCountCard("Checked Out", checkedOutBookingCount, Modifier.weight(1f))
                BookingCountCard("Cancelled", cancelledBookingCount, Modifier.weight(1f))
            }

            // Status filter chips with live counts.
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All $allBookingCount",
                    "ACTIVE" to "Active $activeBookingCount",
                    "CHECKED_OUT" to "Checked Out $checkedOutBookingCount",
                    "CANCELLED" to "Cancelled $cancelledBookingCount"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bookings List
            if (filteredBookings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching bookings found." else "No bookings recorded yet.",
                            color = Slate500,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.editingBooking.value = null
                                onNavigateToNewBooking()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                        ) {
                            Text("Create First Booking")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBookings, key = { it.bookingId }) { booking ->
                        BookingItemCard(
                            booking = booking,
                            currencySymbol = settings.currencySymbol,
                            onEdit = {
                                viewModel.editingBooking.value = booking
                                onNavigateToNewBooking()
                            },
                            onCheckout = { viewModel.checkoutBooking(booking.bookingId) },
                            onCancel = { viewModel.cancelBooking(booking.bookingId) },
                            onDelete = { bookingToDelete = booking },
                            onGenerateInvoice = { onGenerateInvoice(booking) },
                            onOpenDetails = { onOpenDetails(booking) },
                            onShowQr = { bookingForQr = booking }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    bookingToDelete?.let { b ->
        ConfirmDeleteDialog(
            title = "Delete Booking?",
            message = "Are you sure you want to delete Booking ${b.bookingId} for ${b.customerName}? Room ${b.roomNumber} will become available.",
            onConfirm = {
                viewModel.deleteBooking(b)
                bookingToDelete = null
            },
            onDismiss = { bookingToDelete = null }
        )
    }

    // UPI QR Dialog
    bookingForQr?.let { b ->
        UpiQrDialog(
            settings = settings,
            initialAmount = b.pending,
            title = "Pay for Booking ${b.bookingId}",
            onDismiss = { bookingForQr = null }
        )
    }
}

@Composable
private fun BookingCountCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HotelNavy
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Slate600,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BookingItemCard(
    booking: Booking,
    currencySymbol: String,
    onEdit: () -> Unit,
    onCheckout: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onGenerateInvoice: () -> Unit,
    onShowQr: () -> Unit,
    onOpenDetails: () -> Unit = {}
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
            .testTag("booking_card_${booking.bookingId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Booking ID, Room, Status, Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.bookingId,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = HotelNavy
                    )
                    Text(
                        text = "Room ${booking.roomNumber} — ${booking.roomType}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Slate800
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = booking.status)
                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Generate / View Invoice") },
                                onClick = {
                                    isMenuExpanded = false
                                    onGenerateInvoice()
                                },
                                leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("UPI / GPay QR Code") },
                                onClick = {
                                    isMenuExpanded = false
                                    onShowQr()
                                },
                                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Booking") },
                                onClick = {
                                    isMenuExpanded = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            if (booking.status == "ACTIVE") {
                                DropdownMenuItem(
                                    text = { Text("Check Out") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onCheckout()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancel Booking") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onCancel()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete Booking", color = ErrorRed) },
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

            Spacer(modifier = Modifier.height(10.dp))

            // Guest info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Guest: ${booking.customerName}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Slate800
                    )
                    Text(
                        text = "Phone: ${booking.contactNumber.ifBlank { "Not Provided" }}",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Guests: ${booking.numberOfGuests}",
                        fontSize = 12.sp,
                        color = Slate600
                    )
                    Text(
                        text = "⭐ ${DateUtils.formatDisplayDate(booking.checkInDate)}  →  ⭐ ${DateUtils.formatDisplayDate(booking.checkOutDate)}",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate200))
            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total: ${CurrencyFormatter.formatWhole(booking.grandTotal, currencySymbol)}",
                        fontSize = 13.sp,
                        color = Slate700
                    )
                    Text(
                        text = "Paid: ${CurrencyFormatter.formatWhole(booking.paid, currencySymbol)}",
                        fontSize = 12.sp,
                        color = SuccessGreen
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Pending",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                    Text(
                        text = CurrencyFormatter.format(booking.pending, currencySymbol),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (booking.pending > 0) Color(0xFFDC2626) else SuccessGreen
                    )
                }
            }

            // Quick action buttons under card
            if (booking.status == "ACTIVE") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCheckout,
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("Check-Out", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onGenerateInvoice,
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("Invoice", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
