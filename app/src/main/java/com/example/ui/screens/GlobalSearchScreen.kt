package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Booking
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.viewmodel.HotelViewModel

@Composable
fun GlobalSearchScreen(viewModel: HotelViewModel) {
    val results by viewModel.globalSearchResults.collectAsStateWithLifecycle()
    val query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Guest / Booking Search", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate800)
        Spacer(Modifier.height(10.dp))
        Text("Searches the local Room database. Partial names and numbers are supported.", color = Slate500, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        if (results.isEmpty()) {
            Text("Enter a guest name, phone, WhatsApp number, booking ID, or room number in the search bar above.", color = Slate500)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results, key = { it.bookingId }) { booking ->
                    SearchResultCard(booking) { viewModel.openGuestDetails(booking) }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(booking: Booking, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Text(booking.customerName, fontWeight = FontWeight.Bold, color = Slate800)
            Text("Phone: ${booking.contactNumber.ifBlank { "—" }}", fontSize = 13.sp)
            Text("Room ${booking.roomNumber} • ${booking.roomType}", fontSize = 13.sp, color = HotelNavy)
            Text("${booking.checkInDate} → ${booking.checkOutDate} • ${booking.status}", fontSize = 12.sp, color = Slate500)
            Text("Booking: ${booking.bookingId}", fontSize = 11.sp, color = Slate500)
        }
    }
}
