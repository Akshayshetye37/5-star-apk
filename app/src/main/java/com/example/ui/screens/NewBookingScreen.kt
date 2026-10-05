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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Booking
import com.example.data.model.BookingGuest
import com.example.data.model.Payment
import com.example.data.model.Customer
import com.example.ui.components.IdPhotoPicker
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
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GuestForm(
    val name: String = "",
    val idType: String = "Aadhaar Card",
    val idNumber: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val idPhotoUri: String = "",
    val vehicles: List<String> = emptyList()
)

@Composable
fun NewBookingScreen(
    viewModel: HotelViewModel,
    onNavigateBack: () -> Unit,
    onBookingSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val editingBooking by viewModel.editingBooking.collectAsStateWithLifecycle()

    val isEditing = editingBooking != null

    // Form fields
    var bookingId by remember {
        mutableStateOf(
            editingBooking?.bookingId
                ?: "BK-${SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault()).format(Date())}"
        )
    }

    var customerId by remember { mutableStateOf(editingBooking?.customerId ?: "") }
    var guestForms by remember {
        mutableStateOf(
            listOf(
                GuestForm(
                    name = editingBooking?.customerName ?: "",
                    idType = editingBooking?.idType ?: "Aadhaar Card",
                    idNumber = editingBooking?.idNumber ?: "",
                    phone = editingBooking?.contactNumber ?: "",
                    whatsapp = editingBooking?.whatsappNumber ?: "",
                    idPhotoUri = editingBooking?.idPhotoUri ?: ""
                )
            )
        )
    }

    fun updateGuest(index: Int, transform: (GuestForm) -> GuestForm) {
        guestForms = guestForms.mapIndexed { i, g -> if (i == index) transform(g) else g }
    }

    fun addGuest() {
        guestForms = guestForms + GuestForm()
    }

    fun removeGuest(index: Int) {
        if (guestForms.size > 1) guestForms = guestForms.filterIndexed { i, _ -> i != index }
    }

    LaunchedEffect(editingBooking?.bookingId) {
        editingBooking?.bookingId?.let { id ->
            val loaded = viewModel.getGuestsForBookingDirect(id)
            if (loaded.isNotEmpty()) {
                guestForms = loaded.sortedBy { it.sequence }.map { g ->
                    GuestForm(
                        name = g.name,
                        idType = g.idType,
                        idNumber = g.idNumber,
                        phone = g.phone,
                        whatsapp = g.whatsapp,
                        idPhotoUri = g.idPhotoUri,
                        vehicles = g.vehicleNumbers.lines().filter { it.isNotBlank() }
                    )
                }
            }
        }
    }

    val primaryGuest = guestForms.firstOrNull() ?: GuestForm()
    val customerName = primaryGuest.name
    val idType = primaryGuest.idType
    val idNumber = primaryGuest.idNumber
    val contactNumber = primaryGuest.phone
    val whatsappNumber = primaryGuest.whatsapp
    val idPhotoUri = primaryGuest.idPhotoUri
    val numberOfGuests = guestForms.size

    var checkInDate by remember { mutableStateOf(editingBooking?.checkInDate ?: DateUtils.todayDateString()) }
    var checkInTime by remember { mutableStateOf(editingBooking?.checkInTime ?: "12:00 PM") }
    var checkOutDate by remember { mutableStateOf(editingBooking?.checkOutDate ?: DateUtils.tomorrowDateString()) }
    var checkOutTime by remember { mutableStateOf(editingBooking?.checkOutTime ?: "11:00 AM") }

    var selectedRoomNumber by remember {
        mutableStateOf(
            editingBooking?.roomNumber ?: rooms.firstOrNull()?.roomNumber ?: "111"
        )
    }
    var selectedRoomType by remember {
        mutableStateOf(
            editingBooking?.roomType ?: rooms.firstOrNull()?.roomType ?: "Micro Refine Suit"
        )
    }

    var roomChargesText by remember {
        mutableStateOf(
            if (editingBooking != null && editingBooking!!.roomCharges > 0)
                editingBooking!!.roomCharges.toString()
            else {
                val r = rooms.find { it.roomNumber == selectedRoomNumber }
                if (r != null && r.rate > 0) r.rate.toString() else "0"
            }
        )
    }

    LaunchedEffect(selectedRoomNumber, checkInDate, checkOutDate) {
        if (!isEditing) {
            val room = rooms.firstOrNull { it.roomNumber == selectedRoomNumber }
            if (room != null && room.rate > 0.0) {
                val nights = DateUtils.getDaysBetween(
                    DateUtils.parseDateTimeToTimestamp(checkInDate, checkInTime),
                    DateUtils.parseDateTimeToTimestamp(checkOutDate, checkOutTime)
                )
                roomChargesText = (room.rate * nights).toString()
            }
        }
    }

    var advanceText by remember { mutableStateOf(if (editingBooking != null) editingBooking!!.advance.toString() else "0") }
    var onlinePaymentText by remember {
        mutableStateOf(
            if (editingBooking != null) {
                (editingBooking!!.paid - editingBooking!!.advance).coerceAtLeast(0.0).toString()
            } else "0"
        )
    }
    val cashPaid = advanceText.toDoubleOrNull() ?: 0.0
    val onlinePaid = onlinePaymentText.toDoubleOrNull() ?: 0.0
    val paid = cashPaid + onlinePaid
    var discountText by remember { mutableStateOf(if (editingBooking != null) editingBooking!!.discount.toString() else "0") }

    // Initial food items from requirements:
    // Mineral Water, Ghavane Chatney, Tea, Kande Pohe, Other Charges
    var mineralWaterText by remember { mutableStateOf(if (editingBooking != null) editingBooking!!.mineralWater.toString() else "0") }
    var ghavaneChatneyText by remember { mutableStateOf(if (editingBooking != null) editingBooking!!.ghavaneChatney.toString() else "0") }
    var teaText by remember { mutableStateOf(if (editingBooking != null) editingBooking!!.tea.toString() else "0") }
    var kandePoheText by remember { mutableStateOf(if (editingBooking != null) editingBooking!!.kandePohe.toString() else "0") }
    var otherChargesText by remember { mutableStateOf(if (editingBooking != null) editingBooking!!.otherCharges.toString() else "0") }
    var notes by remember { mutableStateOf(editingBooking?.notes ?: "") }

    var showCustomerSearch by remember { mutableStateOf(false) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRoomDropdownExpanded by remember { mutableStateOf(false) }
    var isIdTypeDropdownExpanded by remember { mutableStateOf(false) }
    var idTypeDropdownIndex by remember { mutableIntStateOf(-1) }

    // Numeric calculations
    val roomCharges = roomChargesText.toDoubleOrNull() ?: 0.0
    val advance = advanceText.toDoubleOrNull() ?: 0.0
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val mineralWater = mineralWaterText.toDoubleOrNull() ?: 0.0
    val ghavaneChatney = ghavaneChatneyText.toDoubleOrNull() ?: 0.0
    val tea = teaText.toDoubleOrNull() ?: 0.0
    val kandePohe = kandePoheText.toDoubleOrNull() ?: 0.0
    val otherCharges = otherChargesText.toDoubleOrNull() ?: 0.0

    // Exact formula from requirement 14:
    // Room Charges + Mineral Water + Ghavane Chatney + Tea + Kande Pohe + Other Charges - Discount = Grand Total
    // Grand Total - Advance - Paid = Pending
    val grandTotal = (roomCharges + mineralWater + ghavaneChatney + tea + kandePohe + otherCharges - discount).coerceAtLeast(0.0)
    val pending = (grandTotal - paid).coerceAtLeast(0.0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("new_booking_screen"),
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
                    Text(
                        text = if (isEditing) "Edit Booking" else "New Room Booking",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )
                    Text(
                        text = "Booking ID: $bookingId",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }

                OutlinedButton(
                    onClick = { showCustomerSearch = true },
                    modifier = Modifier.testTag("search_existing_customer_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Search Guest")
                }
            }
        }

        // Error message box
        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFF991B1B),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // --- 1. GUEST DETAILS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Guest Information • ${guestForms.size} guest${if (guestForms.size == 1) "" else "s"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )

                    guestForms.forEachIndexed { index, guest ->
                        if (index > 0) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate200))
                        }

                        Text(
                            "Guest ${index + 1}${if (index == 0) " • Main Guest" else ""}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HotelNavy
                        )

                        OutlinedTextField(
                            value = guest.name,
                            onValueChange = { updateGuest(index) { value -> value.copy(name = it) } },
                            label = { Text("Guest Full Name *") },
                            modifier = Modifier.fillMaxWidth().testTag("guest_name_input_$index"),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = guest.phone,
                                onValueChange = { value ->
                                    updateGuest(index) { guestValue ->
                                        guestValue.copy(
                                            phone = value,
                                            whatsapp = if (guestValue.whatsapp.isBlank()) value else guestValue.whatsapp
                                        )
                                    }
                                },
                                label = { Text("Contact Number *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = guest.vehicles.firstOrNull() ?: "",
                                onValueChange = { value ->
                                    updateGuest(index) { guestValue ->
                                        val vehicles = if (guestValue.vehicles.isEmpty()) listOf(value) else guestValue.vehicles.toMutableList().also { it[0] = value }
                                        guestValue.copy(vehicles = vehicles)
                                    }
                                },
                                label = { Text("Vehicle Registration") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = guest.whatsapp,
                            onValueChange = { value -> updateGuest(index) { it.copy(whatsapp = value) } },
                            label = { Text("WhatsApp Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = guest.idType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("ID Type") },
                                    trailingIcon = {
                                        IconButton(onClick = { idTypeDropdownIndex = index; isIdTypeDropdownExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { isIdTypeDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = isIdTypeDropdownExpanded && idTypeDropdownIndex == index,
                                    onDismissRequest = { isIdTypeDropdownExpanded = false; idTypeDropdownIndex = -1 }
                                ) {
                                    listOf("Aadhaar Card", "Passport", "Driving License", "Voter ID", "PAN Card", "Other").forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                updateGuest(index) { value -> value.copy(idType = type) }
                                                isIdTypeDropdownExpanded = false
                                                idTypeDropdownIndex = -1
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = guest.idNumber,
                                onValueChange = { value -> updateGuest(index) { it.copy(idNumber = value) } },
                                label = { Text("ID Document Number") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        IdPhotoPicker(
                            photoUri = guest.idPhotoUri,
                            onPhotoSelected = { value -> updateGuest(index) { it.copy(idPhotoUri = value) } },
                            onOcrResult = { result ->
                                if (result.name.isNotBlank() || result.idNumber.isNotBlank() || result.idType.isNotBlank()) {
                                    updateGuest(index) { current ->
                                        current.copy(
                                            name = if (result.name.isNotBlank()) result.name else current.name,
                                            idType = if (result.idType.isNotBlank()) result.idType else current.idType,
                                            idNumber = if (result.idNumber.isNotBlank()) result.idNumber else current.idNumber
                                        )
                                    }
                                }
                            }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    if (guest.phone.isNotBlank()) {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(guest.phone)}")))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Tap to Call") }
                            OutlinedButton(
                                onClick = {
                                    val number = guest.whatsapp.ifBlank { guest.phone }.filter { it.isDigit() }
                                    if (number.isNotBlank()) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number")))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Tap WhatsApp") }
                        }

                        if (guest.vehicles.size > 1) {
                            Text("Additional Vehicles", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate700)
                        }
                        guest.vehicles.drop(1).forEachIndexed { extraIndex, vehicle ->
                            val vehicleIndex = extraIndex + 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = vehicle,
                                    onValueChange = { value ->
                                        updateGuest(index) {
                                            it.copy(vehicles = it.vehicles.mapIndexed { vi, v -> if (vi == vehicleIndex) value else v })
                                        }
                                    },
                                    label = { Text("Vehicle ${vehicleIndex + 1}") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                IconButton(onClick = {
                                    updateGuest(index) { it.copy(vehicles = it.vehicles.filterIndexed { vi, _ -> vi != vehicleIndex }) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove vehicle")
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { updateGuest(index) { it.copy(vehicles = it.vehicles + "") } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Vehicle")
                        }

                        if (guestForms.size > 1 && index > 0) {
                            OutlinedButton(
                                onClick = { removeGuest(index) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove Guest ${index + 1}")
                            }
                        }
                    }

                    // Required location: directly below the ID image/guest cards.
                    Button(
                        onClick = { addGuest() },
                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                        modifier = Modifier.fillMaxWidth().testTag("add_guest_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Guest")
                    }
                }
            }
        }

        // --- 2. ROOM & STAY DETAILS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Stay & Room Selection", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate800)

                    // Room Selection Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "Room $selectedRoomNumber — $selectedRoomType",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Room *") },
                            trailingIcon = {
                                IconButton(onClick = { isRoomDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { isRoomDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = isRoomDropdownExpanded,
                            onDismissRequest = { isRoomDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            rooms.forEach { room ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Room ${room.roomNumber} — ${room.roomType}", fontWeight = FontWeight.Bold)
                                            Text("Status: ${room.status} | Rate: ${CurrencyFormatter.formatWhole(room.rate, settings.currencySymbol)}", fontSize = 12.sp, color = Slate500)
                                        }
                                    },
                                    onClick = {
                                        selectedRoomNumber = room.roomNumber
                                        selectedRoomType = room.roomType
                                        if (room.rate > 0) {
                                            roomChargesText = room.rate.toString()
                                        }
                                        isRoomDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Check-in & Check-out Date / Time
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = checkInDate,
                            onValueChange = { checkInDate = it },
                            label = { Text("Check-In Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f).testTag("check_in_date_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = checkInTime,
                            onValueChange = { checkInTime = it },
                            label = { Text("Check-In Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = checkOutDate,
                            onValueChange = { checkOutDate = it },
                            label = { Text("Check-Out Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f).testTag("check_out_date_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = checkOutTime,
                            onValueChange = { checkOutTime = it },
                            label = { Text("Check-Out Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = numberOfGuests.toString(),
                            onValueChange = { },
                            label = { Text("No. of Guests") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("guests_count_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = roomChargesText,
                            onValueChange = { roomChargesText = it },
                            label = { Text("Room Charges (${settings.currencySymbol}) *") },
                            supportingText = { Text("Auto: room rate × nights; editable when needed") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("room_charges_input"),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // --- 3. FOOD & BREAKFAST CHARGES (Required items: Mineral Water, Ghavane Chatney, Tea, Kande Pohe, Other) ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Breakfast & Food Charges", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    Text("Add food items billed to this room:", fontSize = 12.sp, color = Slate500)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = mineralWaterText,
                            onValueChange = { mineralWaterText = it },
                            label = { Text("Mineral Water (${settings.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ghavaneChatneyText,
                            onValueChange = { ghavaneChatneyText = it },
                            label = { Text("Ghavane Chatney (${settings.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = teaText,
                            onValueChange = { teaText = it },
                            label = { Text("Tea (${settings.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = kandePoheText,
                            onValueChange = { kandePoheText = it },
                            label = { Text("Kande Pohe (${settings.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = otherChargesText,
                        onValueChange = { otherChargesText = it },
                        label = { Text("Other Charges (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // --- 4. PAYMENT & CALCULATIONS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Discounts & Payments", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate800)

                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Discount (${settings.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = advanceText,
                            onValueChange = { advanceText = it },
                            label = { Text("Cash Paid (${settings.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("cash_payment_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = onlinePaymentText,
                            onValueChange = { onlinePaymentText = it },
                            label = { Text("Online / UPI Paid (${settings.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("online_payment_input"),
                            singleLine = true
                        )
                    }

                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Paid", fontWeight = FontWeight.Bold, color = Slate700)
                            Text(
                                CurrencyFormatter.format(paid, settings.currencySymbol),
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Special Requests / Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // Grand Total & Pending Calculation summary banner
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Room Charges", fontSize = 13.sp, color = Slate600)
                                Text(CurrencyFormatter.format(roomCharges, settings.currencySymbol), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            val foodSubtotal = mineralWater + ghavaneChatney + tea + kandePohe + otherCharges
                            if (foodSubtotal > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Food & Other Charges", fontSize = 13.sp, color = Slate600)
                                    Text(CurrencyFormatter.format(foodSubtotal, settings.currencySymbol), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Advance", fontSize = 13.sp, color = Slate600)
                                Text(CurrencyFormatter.format(advance, settings.currencySymbol), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Paid", fontSize = 13.sp, color = Slate600)
                                Text(CurrencyFormatter.format(paid, settings.currencySymbol), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                            if (discount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Discount Applied", fontSize = 13.sp, color = Slate600)
                                    Text("- ${CurrencyFormatter.format(discount, settings.currencySymbol)}", fontSize = 13.sp, color = Color(0xFFEF4444))
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate200))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Grand Total", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                                Text(CurrencyFormatter.format(grandTotal, settings.currencySymbol), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Pending Balance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                Text(
                                    CurrencyFormatter.format(pending, settings.currencySymbol),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pending > 0) Color(0xFFDC2626) else SuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. ACTION BUTTONS ---
        item {
            Button(
                onClick = {
                    if (customerName.isBlank()) {
                        errorMessage = "Please enter guest customer name."
                        return@Button
                    }
                    if (contactNumber.isBlank()) {
                        errorMessage = "Please enter guest contact phone number."
                        return@Button
                    }
                    if (selectedRoomNumber.isBlank()) {
                        errorMessage = "Please select a room."
                        return@Button
                    }
                    if (guestForms.any { it.name.isBlank() || it.phone.isBlank() || it.idNumber.isBlank() || it.idPhotoUri.isBlank() }) {
                        errorMessage = "Every guest must have name, contact number, ID number and ID photo."
                        return@Button
                    }

                    val inTs = DateUtils.parseDateTimeToTimestamp(checkInDate, checkInTime)
                    val outTs = DateUtils.parseDateTimeToTimestamp(checkOutDate, checkOutTime)

                    if (outTs <= inTs) {
                        errorMessage = "Check-out date/time must be strictly after Check-in date/time."
                        return@Button
                    }

                    errorMessage = null

                    val bookingToSave = Booking(
                        bookingId = bookingId,
                        customerId = customerId,
                        customerName = customerName.trim(),
                        idType = idType,
                        idNumber = idNumber.trim(),
                        contactNumber = contactNumber.trim(),
                        whatsappNumber = whatsappNumber.trim(),
                        checkInDate = checkInDate,
                        checkInTime = checkInTime,
                        checkOutDate = checkOutDate,
                        checkOutTime = checkOutTime,
                        checkInTimestamp = inTs,
                        checkOutTimestamp = outTs,
                        numberOfGuests = numberOfGuests,
                        roomNumber = selectedRoomNumber,
                        roomType = selectedRoomType,
                        roomCharges = roomCharges,
                        advance = advance,
                        paid = paid,
                        discount = discount,
                        mineralWater = mineralWater,
                        ghavaneChatney = ghavaneChatney,
                        tea = tea,
                        kandePohe = kandePohe,
                        otherCharges = otherCharges,
                        notes = notes.trim(),
                        idPhotoUri = idPhotoUri,
                        status = editingBooking?.status ?: "ACTIVE"
                    )

                    val guestsToSave = guestForms.mapIndexed { index, guest ->
                        BookingGuest(
                            guestId = "GUEST-${bookingId}-${index + 1}",
                            bookingId = bookingId,
                            sequence = index + 1,
                            name = if (index == 0) customerName.trim() else guest.name.trim(),
                            idType = if (index == 0) idType else guest.idType,
                            idNumber = if (index == 0) idNumber.trim() else guest.idNumber.trim(),
                            phone = if (index == 0) contactNumber.trim() else guest.phone.trim(),
                            whatsapp = if (index == 0) whatsappNumber.trim() else guest.whatsapp.trim(),
                            idPhotoUri = if (index == 0) idPhotoUri else guest.idPhotoUri,
                            vehicleNumbers = guest.vehicles.filter { it.isNotBlank() }.joinToString("\n")
                        )
                    }

                    val cashPayment = if (!isEditing && cashPaid > 0) Payment(
                        paymentId = "PAY-${System.currentTimeMillis()}-C",
                        bookingId = bookingId,
                        customerId = customerId,
                        customerName = customerName,
                        amount = cashPaid,
                        paymentMethod = "Cash",
                        notes = "Payment at booking"
                    ) else null

                    val onlinePayment = if (!isEditing && onlinePaid > 0) Payment(
                        paymentId = "PAY-${System.currentTimeMillis()}-U",
                        bookingId = bookingId,
                        customerId = customerId,
                        customerName = customerName,
                        amount = onlinePaid,
                        paymentMethod = "UPI / Online",
                        notes = "Online payment at booking"
                    ) else null

                    viewModel.saveBooking(
                        booking = bookingToSave,
                        isEditing = isEditing,
                        guests = guestsToSave,
                        initialPayment = cashPayment,
                        additionalPayments = listOfNotNull(onlinePayment)
                    ) { success, msg ->
                        if (success) {
                            viewModel.editingBooking.value = null
                            onBookingSaved(bookingId)
                        } else {
                            errorMessage = msg
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_booking_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Update Booking" else "Confirm & Save Booking",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Customer Search Modal Dialog
    if (showCustomerSearch) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomerSearch = false },
            title = { Text("Search Existing Customer", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = customerSearchQuery,
                        onValueChange = { customerSearchQuery = it },
                        label = { Text("Search by Name or Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val filtered = customers.filter {
                        it.name.contains(customerSearchQuery, ignoreCase = true) ||
                                it.phone.contains(customerSearchQuery, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        Text("No matching guest found.", color = Slate500, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(filtered) { c ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            updateGuest(0) {
                                                it.copy(
                                                    name = c.name,
                                                    idType = c.idType,
                                                    idNumber = c.idNumber,
                                                    phone = c.phone,
                                                    whatsapp = c.whatsapp,
                                                    idPhotoUri = c.idPhotoUri
                                                )
                                            }
                                            customerId = c.customerId
                                            showCustomerSearch = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Slate100)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(c.name, fontWeight = FontWeight.Bold, color = Slate800)
                                        Text("Phone: ${c.phone} | Visits: ${c.totalVisits}", fontSize = 12.sp, color = Slate600)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCustomerSearch = false }) {
                    Text("Close")
                }
            }
        )
    }
}
