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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import com.example.data.model.RoomEntity
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
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter

@Composable
fun RoomsScreen(
    viewModel: HotelViewModel
) {
    val context = LocalContext.current
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<RoomEntity?>(null) }
    var roomToDelete by remember { mutableStateOf<RoomEntity?>(null) }

    val filteredRooms = remember(rooms, searchQuery, selectedFilter) {
        rooms.filter { r ->
            val matchesFilter = when (selectedFilter) {
                "AVAILABLE" -> r.status == "AVAILABLE"
                "OCCUPIED" -> r.status == "OCCUPIED"
                "RESERVED" -> r.status == "RESERVED"
                "MAINTENANCE" -> r.status == "MAINTENANCE"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    r.roomNumber.contains(searchQuery, ignoreCase = true) ||
                    r.roomType.contains(searchQuery, ignoreCase = true) ||
                    r.floor.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRoom = null
                    showAddEditDialog = true
                },
                containerColor = HotelNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_room")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Room")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("rooms_screen")
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
                    placeholder = { Text("Search room number, type...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = {
                        val file = CsvExporter.exportRoomsToCsv(context, filteredRooms)
                        CsvExporter.shareCsvFile(context, file, "Hotel Rooms Export")
                    }
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = HotelNavy)
                }
            }

            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("ALL" to "All", "AVAILABLE" to "Available", "OCCUPIED" to "Occupied", "RESERVED" to "Reserved", "MAINTENANCE" to "Maintenance")
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            if (filteredRooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching rooms." else "No rooms found.",
                        color = Slate500
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRooms, key = { it.roomNumber }) { room ->
                        RoomItemCard(
                            room = room,
                            currencySymbol = settings.currencySymbol,
                            onEdit = {
                                editingRoom = room
                                showAddEditDialog = true
                            },
                            onDelete = { roomToDelete = room },
                            onStatusChange = { newStatus ->
                                viewModel.updateRoomStatus(room.roomNumber, newStatus)
                            },
                            onSharePdf = {
                                viewModel.getCurrentActiveBookingForRoom(room.roomNumber) { booking ->
                                    if (booking == null) viewModel.showMessage("No active guest found for this room.")
                                    else viewModel.shareGuestRoomPdf(context, booking)
                                }
                            },
                            onViewGuest = {
                                viewModel.getCurrentActiveBookingForRoom(room.roomNumber) { booking ->
                                    if (booking == null) viewModel.showMessage("No active guest found for this room.")
                                    else viewModel.openGuestDetails(booking)
                                }
                            },
                            onInvoice = {
                                viewModel.getCurrentActiveBookingForRoom(room.roomNumber) { booking ->
                                    if (booking == null) viewModel.showMessage("No active guest found for this room.")
                                    else {
                                        viewModel.selectedBookingForInvoice.value = booking
                                        viewModel.navigateTo("INVOICE_GENERATOR")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Room Dialog
    if (showAddEditDialog) {
        RoomDialog(
            room = editingRoom,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showAddEditDialog = false },
            onSave = { room, isEditing ->
                viewModel.saveRoom(room, isEditing)
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog (Requirement 5: Room deletion must require confirmation)
    roomToDelete?.let { r ->
        ConfirmDeleteDialog(
            title = "Delete Room ${r.roomNumber}?",
            message = "Are you sure you want to delete Room ${r.roomNumber} (${r.roomType})? Past bookings and invoices will safely retain their room details.",
            onConfirm = {
                viewModel.deleteRoom(r)
                roomToDelete = null
            },
            onDismiss = { roomToDelete = null }
        )
    }
}

@Composable
fun RoomItemCard(
    room: RoomEntity,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (String) -> Unit,
    onSharePdf: () -> Unit,
    onViewGuest: () -> Unit,
    onInvoice: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("room_card_${room.roomNumber}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(HotelNavy.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = HotelNavy)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Room ${room.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate800)
                        Text(room.roomType, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = HotelNavy)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = room.status)
                    Box {
                        IconButton(onClick = { isMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Set Status: Available") },
                                onClick = {
                                    isMenuExpanded = false
                                    onStatusChange("AVAILABLE")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set Status: Maintenance") },
                                onClick = {
                                    isMenuExpanded = false
                                    onStatusChange("MAINTENANCE")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Set Status: Reserved") },
                                onClick = {
                                    isMenuExpanded = false
                                    onStatusChange("RESERVED")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Guest") },
                                onClick = {
                                    isMenuExpanded = false
                                    onViewGuest()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Invoice") },
                                onClick = {
                                    isMenuExpanded = false
                                    onInvoice()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Guest PDF") },
                                onClick = {
                                    isMenuExpanded = false
                                    onSharePdf()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Room & Rate") },
                                onClick = {
                                    isMenuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Room", color = ErrorRed) },
                                onClick = {
                                    isMenuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Floor: ${room.floor}", fontSize = 12.sp, color = Slate600)
                Text(
                    text = if (room.rate > 0)
                        "Rate: ${CurrencyFormatter.formatWhole(room.rate, currencySymbol)} / night"
                    else
                        "Rate: Not Set",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (room.rate > 0) HotelNavy else Slate500
                )
            }

            if (room.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Notes: ${room.notes}", fontSize = 11.sp, color = Slate500)
            }
        }
    }
}

@Composable
fun RoomDialog(
    room: RoomEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (RoomEntity, Boolean) -> Unit
) {
    val isEditing = room != null

    var roomNumber by remember { mutableStateOf(room?.roomNumber ?: "") }
    var roomType by remember { mutableStateOf(room?.roomType ?: "Micro Refine Suit") }
    var floor by remember { mutableStateOf(room?.floor ?: "1st Floor") }
    var rateText by remember { mutableStateOf(if (room != null && room.rate > 0) room.rate.toString() else "0") }
    var status by remember { mutableStateOf(room?.status ?: "AVAILABLE") }
    var notes by remember { mutableStateOf(room?.notes ?: "") }

    var isRoomTypeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Room ${room?.roomNumber}" else "Add New Room", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room Number *") },
                    enabled = !isEditing,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Room Type dropdown with the two required exact room types:
                // "Micro Refine Suit" and "Compact Private Suite"
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = roomType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Room Type *") },
                        trailingIcon = {
                            IconButton(onClick = { isRoomTypeDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { isRoomTypeDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = isRoomTypeDropdownExpanded,
                        onDismissRequest = { isRoomTypeDropdownExpanded = false }
                    ) {
                        listOf("Micro Refine Suit", "Compact Private Suite").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    roomType = type
                                    isRoomTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Floor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Nightly Rate ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Balcony, AC, Mountain view)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (roomNumber.isBlank()) return@Button
                    val r = RoomEntity(
                        roomNumber = roomNumber.trim(),
                        roomType = roomType,
                        floor = floor.trim(),
                        rate = rateText.toDoubleOrNull() ?: 0.0,
                        status = status,
                        notes = notes.trim()
                    )
                    onSave(r, isEditing)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text(if (isEditing) "Update Room" else "Add Room")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
