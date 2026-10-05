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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RoomService
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FoodItem
import com.example.data.model.FoodOrder
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.CurrencyFormatter

@Composable
fun FoodBreakfastScreen(
    viewModel: HotelViewModel
) {
    val foodItems by viewModel.foodItems.collectAsStateWithLifecycle()
    val activeBookings by viewModel.activeBookings.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFoodItem by remember { mutableStateOf<FoodItem?>(null) }
    var foodToDelete by remember { mutableStateOf<FoodItem?>(null) }
    var foodForOrder by remember { mutableStateOf<FoodItem?>(null) }

    val filteredItems = remember(foodItems, searchQuery) {
        if (searchQuery.isBlank()) foodItems
        else {
            foodItems.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingFoodItem = null
                    showAddDialog = true
                },
                containerColor = HotelNavy,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_food_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Food Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("food_breakfast_screen")
        ) {
            // Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search food or beverage...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No food items found.", color = Slate500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        FoodItemCard(
                            item = item,
                            currencySymbol = settings.currencySymbol,
                            onEdit = {
                                editingFoodItem = item
                                showAddDialog = true
                            },
                            onDelete = { foodToDelete = item },
                            onToggleActive = {
                                viewModel.saveFoodItem(item.copy(isActive = !item.isActive))
                            },
                            onOrderToRoom = { foodForOrder = item }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Food Dialog
    if (showAddDialog) {
        FoodItemDialog(
            foodItem = editingFoodItem,
            currencySymbol = settings.currencySymbol,
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                viewModel.saveFoodItem(item)
                showAddDialog = false
            }
        )
    }

    // Order to Room Dialog
    foodForOrder?.let { food ->
        OrderToRoomDialog(
            foodItem = food,
            activeBookings = activeBookings,
            currencySymbol = settings.currencySymbol,
            onDismiss = { foodForOrder = null },
            onConfirmOrder = { bookingId, roomNo, qty ->
                val total = food.price * qty
                val order = FoodOrder(
                    bookingId = bookingId,
                    roomNumber = roomNo,
                    foodItemId = food.id,
                    foodName = food.name,
                    quantity = qty,
                    price = food.price,
                    total = total
                )
                viewModel.addFoodOrder(order)
                foodForOrder = null
            }
        )
    }

    // Delete Confirmation
    foodToDelete?.let { f ->
        ConfirmDeleteDialog(
            title = "Delete ${f.name}?",
            message = "Are you sure you want to remove this food item from the menu?",
            onConfirm = {
                viewModel.deleteFoodItem(f)
                foodToDelete = null
            },
            onDismiss = { foodToDelete = null }
        )
    }
}

@Composable
fun FoodItemCard(
    item: FoodItem,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    onOrderToRoom: () -> Unit
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("food_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFEA580C).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color(0xFFEA580C))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)
                Text(
                    text = "${item.category} • ${if (item.price > 0) CurrencyFormatter.format(item.price, currencySymbol) else "Price Not Set"}",
                    fontSize = 12.sp,
                    color = if (item.price > 0) HotelNavy else Slate500,
                    fontWeight = FontWeight.Medium
                )
                if (item.description.isNotBlank()) {
                    Text(item.description, fontSize = 11.sp, color = Slate500, maxLines = 1)
                }
            }

            // Quick order button
            IconButton(onClick = onOrderToRoom) {
                Icon(Icons.Default.RoomService, contentDescription = "Order to Room", tint = HotelNavy)
            }

            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Add to Active Room") },
                        onClick = {
                            isMenuExpanded = false
                            onOrderToRoom()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.isActive) "Disable Item" else "Enable Item") },
                        onClick = {
                            isMenuExpanded = false
                            onToggleActive()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Item") },
                        onClick = {
                            isMenuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Item", color = ErrorRed) },
                        onClick = {
                            isMenuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FoodItemDialog(
    foodItem: FoodItem?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (FoodItem) -> Unit
) {
    val isEditing = foodItem != null
    var name by remember { mutableStateOf(foodItem?.name ?: "") }
    var category by remember { mutableStateOf(foodItem?.category ?: "Breakfast") }
    var priceText by remember { mutableStateOf(if (foodItem != null && foodItem.price > 0) foodItem.price.toString() else "0") }
    var description by remember { mutableStateOf(foodItem?.description ?: "") }
    var isActive by remember { mutableStateOf(foodItem?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Food Item" else "Add Food Item", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Item Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Breakfast, Beverage)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available / Active", fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val item = FoodItem(
                        id = foodItem?.id ?: 0,
                        name = name.trim(),
                        category = category.trim(),
                        price = priceText.toDoubleOrNull() ?: 0.0,
                        description = description.trim(),
                        isActive = isActive
                    )
                    onSave(item)
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
fun OrderToRoomDialog(
    foodItem: FoodItem,
    activeBookings: List<com.example.data.model.Booking>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmOrder: (bookingId: String, roomNo: String, quantity: Int) -> Unit
) {
    var selectedBooking by remember { mutableStateOf(activeBookings.firstOrNull()) }
    var quantity by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bill ${foodItem.name} to Room", fontWeight = FontWeight.Bold) },
        text = {
            if (activeBookings.isEmpty()) {
                Text("No active room stays currently checked in. Check in a guest first.", color = Slate500)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Active Room:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    activeBookings.forEach { b ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedBooking?.bookingId == b.bookingId) HotelNavy.copy(alpha = 0.1f) else Slate100
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBooking = b }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Room ${b.roomNumber} — ${b.customerName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Booking: ${b.bookingId}", fontSize = 11.sp, color = Slate500)
                                }
                                if (selectedBooking?.bookingId == b.bookingId) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = HotelNavy)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quantity:", fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (quantity > 1) quantity-- }) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("$quantity", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            IconButton(onClick = { quantity++ }) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val total = foodItem.price * quantity
                    Text(
                        text = "Total Added to Bill: ${CurrencyFormatter.format(total, currencySymbol)}",
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy,
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            if (activeBookings.isNotEmpty() && selectedBooking != null) {
                Button(
                    onClick = {
                        onConfirmOrder(selectedBooking!!.bookingId, selectedBooking!!.roomNumber, quantity)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("Add to Room Bill")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
