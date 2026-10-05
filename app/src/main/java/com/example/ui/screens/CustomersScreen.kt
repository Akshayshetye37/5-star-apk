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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Booking
import com.example.data.model.Customer
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.IdPhotoPicker
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.CsvExporter
import com.example.util.DateUtils

@Composable
fun CustomersScreen(
    viewModel: HotelViewModel
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var customerHistoryToShow by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else {
            customers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery, ignoreCase = true) ||
                        it.idNumber.contains(searchQuery, ignoreCase = true) ||
                        it.customerId.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCustomer = null
                    showAddDialog = true
                },
                containerColor = HotelNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_customer")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("customers_screen")
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
                    placeholder = { Text("Search by name, phone, ID number...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = {
                        val file = CsvExporter.exportCustomersToCsv(context, filteredCustomers)
                        CsvExporter.shareCsvFile(context, file, "Hotel Customers Export")
                    }
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = HotelNavy)
                }
            }

            if (filteredCustomers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No customers found matching '$searchQuery'." else "No customers recorded yet.",
                        color = Slate500
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCustomers, key = { it.customerId }) { customer ->
                        CustomerItemCard(
                            customer = customer,
                            onEdit = {
                                editingCustomer = customer
                                showAddDialog = true
                            },
                            onDelete = { customerToDelete = customer },
                            onViewHistory = { customerHistoryToShow = customer }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        CustomerDialog(
            customer = editingCustomer,
            onDismiss = { showAddDialog = false },
            onSave = { c ->
                viewModel.saveCustomer(c)
                showAddDialog = false
            }
        )
    }

    // History Dialog
    customerHistoryToShow?.let { cust ->
        val pastBookings = bookings.filter { it.customerId == cust.customerId || it.contactNumber == cust.phone }
        CustomerHistoryDialog(
            customer = cust,
            pastBookings = pastBookings,
            onDismiss = { customerHistoryToShow = null }
        )
    }

    // Delete Confirmation
    customerToDelete?.let { c ->
        ConfirmDeleteDialog(
            title = "Delete Customer ${c.name}?",
            message = "Are you sure you want to delete this customer? Historical bookings and bills will remain intact.",
            onConfirm = {
                viewModel.deleteCustomer(c)
                customerToDelete = null
            },
            onDismiss = { customerToDelete = null }
        )
    }
}

@Composable
fun CustomerItemCard(
    customer: Customer,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewHistory: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("customer_card_${customer.customerId}")
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo / Avatar
            if (customer.idPhotoUri.isNotBlank()) {
                AsyncImage(
                    model = customer.idPhotoUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Slate100),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HotelNavy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
                if (customer.phone.isNotBlank()) {
                    Text("Phone: ${customer.phone}", fontSize = 12.sp, color = Slate600)
                }
                if (customer.idNumber.isNotBlank()) {
                    Text("${customer.idType}: ${customer.idNumber}", fontSize = 11.sp, color = Slate500)
                }
                Text("Visits: ${customer.totalVisits} | Last: ${DateUtils.formatDisplayDate(customer.lastVisit)}", fontSize = 11.sp, color = HotelNavy)
            }

            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("View Stay History") },
                        onClick = {
                            isMenuExpanded = false
                            onViewHistory()
                        },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Customer") },
                        onClick = {
                            isMenuExpanded = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Customer", color = ErrorRed) },
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
}

@Composable
fun CustomerDialog(
    customer: Customer?,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    val isEditing = customer != null
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var whatsapp by remember { mutableStateOf(customer?.whatsapp ?: "") }
    var idType by remember { mutableStateOf(customer?.idType ?: "Aadhaar Card") }
    var idNumber by remember { mutableStateOf(customer?.idNumber ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var idPhotoUri by remember { mutableStateOf(customer?.idPhotoUri ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Customer" else "Add Customer", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            if (whatsapp.isBlank()) whatsapp = it
                        },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = idType,
                            onValueChange = { idType = it },
                            label = { Text("ID Type") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = idNumber,
                            onValueChange = { idNumber = it },
                            label = { Text("ID Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / City") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Preferences") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
                item {
                    IdPhotoPicker(
                        photoUri = idPhotoUri,
                        onPhotoSelected = { idPhotoUri = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val c = Customer(
                        customerId = customer?.customerId ?: "CUST-${System.currentTimeMillis().toString().takeLast(6)}",
                        name = name.trim(),
                        idType = idType.trim(),
                        idNumber = idNumber.trim(),
                        phone = phone.trim(),
                        whatsapp = whatsapp.trim(),
                        address = address.trim(),
                        notes = notes.trim(),
                        idPhotoUri = idPhotoUri,
                        createdAt = customer?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        lastVisit = customer?.lastVisit ?: System.currentTimeMillis(),
                        totalVisits = customer?.totalVisits ?: 1
                    )
                    onSave(c)
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text(if (isEditing) "Update" else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CustomerHistoryDialog(
    customer: Customer,
    pastBookings: List<Booking>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(customer.name, fontWeight = FontWeight.Bold, color = HotelNavy)
                Text("Total Visits: ${pastBookings.size}", fontSize = 12.sp, color = Slate500)
            }
        },
        text = {
            if (pastBookings.isEmpty()) {
                Text("No previous bookings recorded for this guest.", color = Slate500)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pastBookings) { b ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate100),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(b.bookingId, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HotelNavy)
                                    Text("Room ${b.roomNumber}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Text("Stay: ${b.checkInDate} to ${b.checkOutDate}", fontSize = 11.sp, color = Slate600)
                                Text("Charges: ₹${b.grandTotal} | Status: ${b.status}", fontSize = 11.sp, color = Slate500)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
