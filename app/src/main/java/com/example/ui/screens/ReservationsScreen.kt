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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.Reservation
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusChip
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReservationsScreen(
    viewModel: HotelViewModel
) {
    val context = LocalContext.current
    val reservations by viewModel.reservations.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    var showAddDialog by remember { mutableStateOf(false) }
    var reservationToDelete by remember { mutableStateOf<Reservation?>(null) }
    var editingReservation by remember { mutableStateOf<Reservation?>(null) }

    val filteredReservations = remember(reservations, searchQuery, selectedFilter) {
        reservations.filter { r ->
            val matchesFilter = when (selectedFilter) {
                "CONFIRMED" -> r.status == "CONFIRMED"
                "PENDING" -> r.status == "PENDING"
                "CHECKED-IN" -> r.status == "CHECKED-IN"
                "CANCELLED" -> r.status == "CANCELLED"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    r.reservationId.contains(searchQuery, ignoreCase = true) ||
                    r.customerName.contains(searchQuery, ignoreCase = true) ||
                    r.phone.contains(searchQuery, ignoreCase = true) ||
                    r.roomNumber.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingReservation = null
                    showAddDialog = true
                },
                containerColor = HotelNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_new_reservation")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Reservation")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("reservations_screen")
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
                    placeholder = { Text("Search reservations...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = {
                        val file = CsvExporter.exportReservationsToCsv(context, filteredReservations)
                        CsvExporter.shareCsvFile(context, file, "Reservations Export")
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
                val filters = listOf("ALL" to "All", "CONFIRMED" to "Confirmed", "PENDING" to "Pending", "CHECKED-IN" to "Checked-In", "CANCELLED" to "Cancelled")
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            if (filteredReservations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching reservations." else "No reservations yet. Tap '+' to create one.",
                        color = Slate500
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredReservations, key = { it.reservationId }) { res ->
                        ReservationItemCard(
                            reservation = res,
                            currencySymbol = settings.currencySymbol,
                            onConvertToBooking = {
                                viewModel.convertReservationToBooking(res)
                            },
                            onCancel = { viewModel.cancelReservation(res.reservationId) },
                            onDelete = { reservationToDelete = res },
                            onEdit = {
                                editingReservation = res
                                showAddDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Reservation Dialog
    if (showAddDialog) {
        ReservationDialog(
            reservation = editingReservation,
            rooms = rooms,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showAddDialog = false },
            onSave = { res, isEditing ->
                viewModel.saveReservation(res, isEditing) { success, msg ->
                    if (success) showAddDialog = false
                    else viewModel.showMessage(msg)
                }
            }
        )
    }

    // Delete Confirmation
    reservationToDelete?.let { r ->
        ConfirmDeleteDialog(
            title = "Delete Reservation?",
            message = "Are you sure you want to delete reservation ${r.reservationId} for ${r.customerName}?",
            onConfirm = {
                viewModel.deleteReservation(r)
                reservationToDelete = null
            },
            onDismiss = { reservationToDelete = null }
        )
    }
}

@Composable
fun ReservationItemCard(
    reservation: Reservation,
    currencySymbol: String,
    onConvertToBooking: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("reservation_card_${reservation.reservationId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(reservation.reservationId, fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 14.sp)
                    Text("Room ${reservation.roomNumber} (${reservation.roomType})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Slate800)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = reservation.status)
                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                            if (reservation.status in listOf("PENDING", "CONFIRMED")) {
                                DropdownMenuItem(
                                    text = { Text("Check-In → Convert to Booking") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onConvertToBooking()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancel Reservation") },
                                    onClick = {
                                        isMenuExpanded = false
                                        onCancel()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    isMenuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = ErrorRed) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Guest: ${reservation.customerName} | Phone: ${reservation.phone}", fontSize = 12.sp, color = Slate700)
            Text("Stay: ${DateUtils.formatDisplayDate(reservation.checkInDate)} → ${DateUtils.formatDisplayDate(reservation.checkOutDate)} (${reservation.guests} guests)", fontSize = 12.sp, color = Slate600)

            if (reservation.advance > 0) {
                Text("Advance Paid: ${CurrencyFormatter.format(reservation.advance, currencySymbol)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
            }

            if (reservation.status in listOf("PENDING", "CONFIRMED")) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onConvertToBooking,
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("Check-In Guest (Convert to Booking)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReservationDialog(
    reservation: Reservation?,
    rooms: List<com.example.data.model.RoomEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Reservation, Boolean) -> Unit
) {
    val isEditing = reservation != null

    var resId by remember {
        mutableStateOf(
            reservation?.reservationId
                ?: "RES-${SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())}"
        )
    }
    var guestName by remember { mutableStateOf(reservation?.customerName ?: "") }
    var phone by remember { mutableStateOf(reservation?.phone ?: "") }
    var selectedRoom by remember { mutableStateOf(reservation?.roomNumber ?: rooms.firstOrNull()?.roomNumber ?: "111") }
    var selectedRoomType by remember { mutableStateOf(reservation?.roomType ?: rooms.firstOrNull()?.roomType ?: "Micro Refine Suit") }
    var checkInDate by remember { mutableStateOf(reservation?.checkInDate ?: DateUtils.todayDateString()) }
    var checkOutDate by remember { mutableStateOf(reservation?.checkOutDate ?: DateUtils.tomorrowDateString()) }
    var guests by remember { mutableIntStateOf(reservation?.guests ?: 1) }
    var advanceText by remember { mutableStateOf(if (reservation != null) reservation.advance.toString() else "0") }
    var notes by remember { mutableStateOf(reservation?.notes ?: "") }
    var status by remember { mutableStateOf(reservation?.status ?: "CONFIRMED") }

    var isRoomExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Reservation" else "New Reservation", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = guestName,
                        onValueChange = { guestName = it },
                        label = { Text("Guest Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "Room $selectedRoom — $selectedRoomType",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Room *") },
                            trailingIcon = {
                                IconButton(onClick = { isRoomExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { isRoomExpanded = true }
                        )
                        if (isRoomExpanded) {
                            AlertDialog(
                                onDismissRequest = { isRoomExpanded = false },
                                title = { Text("Select Room") },
                                text = {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(rooms) { r ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedRoom = r.roomNumber
                                                        selectedRoomType = r.roomType
                                                        isRoomExpanded = false
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = Color.White)
                                            ) {
                                                Column(Modifier.padding(12.dp)) {
                                                    Text("Room ${r.roomNumber}", fontWeight = FontWeight.Bold)
                                                    Text(r.roomType, fontSize = 12.sp, color = Slate500)
                                                    Text(r.status, fontSize = 11.sp, color = Slate500)
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { isRoomExpanded = false }) { Text("Close") }
                                }
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = checkInDate,
                            onValueChange = { checkInDate = it },
                            label = { Text("Check-In") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = checkOutDate,
                            onValueChange = { checkOutDate = it },
                            label = { Text("Check-Out") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = guests.toString(),
                            onValueChange = { guests = it.toIntOrNull() ?: 1 },
                            label = { Text("Guests") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = advanceText,
                            onValueChange = { advanceText = it },
                            label = { Text("Advance ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Special Requests") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (guestName.isBlank() || phone.isBlank()) return@Button
                    val inTs = DateUtils.parseDateTimeToTimestamp(checkInDate, "12:00 PM")
                    val outTs = DateUtils.parseDateTimeToTimestamp(checkOutDate, "11:00 AM")
                    val res = Reservation(
                        reservationId = resId,
                        customerName = guestName.trim(),
                        phone = phone.trim(),
                        roomNumber = selectedRoom,
                        roomType = selectedRoomType,
                        checkInDate = checkInDate,
                        checkOutDate = checkOutDate,
                        checkInTimestamp = inTs,
                        checkOutTimestamp = outTs,
                        guests = guests,
                        advance = advanceText.toDoubleOrNull() ?: 0.0,
                        notes = notes.trim(),
                        status = status
                    )
                    onSave(res, isEditing)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("Save Reservation")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
