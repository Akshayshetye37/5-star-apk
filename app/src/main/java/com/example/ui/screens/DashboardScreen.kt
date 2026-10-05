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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.StatCard
import com.example.ui.components.StatusChip
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun DashboardScreen(
    viewModel: HotelViewModel,
    onNavigate: (String) -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. WELCOME BANNER ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = HotelNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = settings.hotelName,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Today: ${DateUtils.formatDisplayDate(System.currentTimeMillis())}",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = HotelGold
                        ) {
                            Text(
                                text = "OFFLINE NATIVE",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Revenue & Pending Highlight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable { onNavigate("PAYMENTS") }
                                .padding(12.dp)
                                .testTag("today_revenue_card")
                        ) {
                            Text("Today's Revenue", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                            Text(
                                text = CurrencyFormatter.formatWhole(stats.todayRevenue, settings.currencySymbol),
                                color = Color(0xFF6EE7B7),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tap to view payments",
                                color = Color(0xFFCBD5E1),
                                fontSize = 9.sp
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable { onNavigate("INVOICE_HISTORY") }
                                .padding(12.dp)
                                .testTag("pending_balance_card")
                        ) {
                            Text("Pending Balance", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                            Text(
                                text = CurrencyFormatter.formatWhole(stats.pendingPayments, settings.currencySymbol),
                                color = Color(0xFFFCA5A5),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tap to view invoices",
                                color = Color(0xFFCBD5E1),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 2. QUICK ACTIONS ---
        item {
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    QuickActionButton(
                        label = "+ New Booking",
                        icon = Icons.Default.BookOnline,
                        color = HotelNavy,
                        onClick = {
                            viewModel.editingBooking.value = null
                            onNavigate("NEW_BOOKING")
                        }
                    )
                }
                item {
                    QuickActionButton(
                        label = "+ Reservation",
                        icon = Icons.Default.EventAvailable,
                        color = Color(0xFF0284C7),
                        onClick = { onNavigate("RESERVATIONS") }
                    )
                }
                item {
                    QuickActionButton(
                        label = "+ New Invoice",
                        icon = Icons.Default.Receipt,
                        color = HotelGold,
                        onClick = { onNavigate("INVOICE_GENERATOR") }
                    )
                }
                item {
                    QuickActionButton(
                        label = "+ Customer",
                        icon = Icons.Default.PersonAdd,
                        color = Color(0xFF059669),
                        onClick = { onNavigate("CUSTOMERS") }
                    )
                }
                item {
                    QuickActionButton(
                        label = "+ Payment",
                        icon = Icons.Default.Payments,
                        color = Color(0xFF7C3AED),
                        onClick = { onNavigate("PAYMENTS") }
                    )
                }
                item {
                    QuickActionButton(
                        label = "+ Food",
                        icon = Icons.Default.Restaurant,
                        color = Color(0xFFEA580C),
                        onClick = { onNavigate("FOOD") }
                    )
                }
            }
        }

        // --- 3. ACTIONABLE PENDING BALANCES ---
        if (stats.pendingBookings.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Pending Payments", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Slate800)
                                Text("Guests with an outstanding balance", fontSize = 11.sp, color = Slate500)
                            }
                            TextButton(onClick = { onNavigate("INVOICE_HISTORY") }) { Text("View all") }
                        }
                        stats.pendingBookings.forEach { booking ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate("INVOICE_HISTORY") }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(booking.customerName, fontWeight = FontWeight.SemiBold, color = Slate800)
                                    Text("Room ${booking.roomNumber}", fontSize = 11.sp, color = Slate500)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        CurrencyFormatter.formatWhole(booking.pending, settings.currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        color = ErrorRed
                                    )
                                    Text("Pending", fontSize = 10.sp, color = ErrorRed)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. KEY METRICS GRID ---
        item {
            Text(
                text = "Room & Stay Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Today's Check-ins",
                    value = "${stats.todayCheckIns}",
                    icon = Icons.Default.BookOnline,
                    iconColor = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("BOOKINGS") }
                )
                StatCard(
                    title = "Today's Check-outs",
                    value = "${stats.todayCheckOuts}",
                    icon = Icons.Default.Hotel,
                    iconColor = WarningAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("BOOKINGS") }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Occupied Rooms",
                    value = "${stats.occupiedRooms}",
                    icon = Icons.Default.MeetingRoom,
                    iconColor = ErrorRed,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("ROOMS") }
                )
                StatCard(
                    title = "Available Rooms",
                    value = "${stats.availableRooms}",
                    icon = Icons.Default.MeetingRoom,
                    iconColor = SuccessGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("ROOMS") }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Reserved Rooms",
                    value = "${stats.reservedRooms}",
                    icon = Icons.Default.EventAvailable,
                    iconColor = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("RESERVATIONS") }
                )
                StatCard(
                    title = "Current Guests",
                    value = "${stats.currentGuests}",
                    icon = Icons.Default.People,
                    iconColor = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("BOOKINGS") }
                )
            }
        }

        // --- 4. RECENT BOOKINGS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Bookings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                TextButton(onClick = { onNavigate("BOOKINGS") }) {
                    Text("View All", color = HotelNavy, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (stats.recentBookings.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No bookings yet. Tap '+ New Booking' to start.", color = Slate500, fontSize = 14.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.recentBookings.forEach { booking ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("BOOKINGS") }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = booking.customerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Slate800
                                    )
                                    Text(
                                        text = "Room ${booking.roomNumber} (${booking.roomType})",
                                        fontSize = 12.sp,
                                        color = Slate600
                                    )
                                    Text(
                                        text = "${booking.checkInDate} to ${booking.checkOutDate}",
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusChip(status = booking.status)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CurrencyFormatter.formatWhole(booking.grandTotal, settings.currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = HotelNavy
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. RECENT INVOICES ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Invoices",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                TextButton(onClick = { onNavigate("INVOICE_HISTORY") }) {
                    Text("View All", color = HotelNavy, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (stats.recentInvoices.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No invoices generated yet.", color = Slate500, fontSize = 14.sp)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.recentInvoices.forEach { invoice ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("INVOICE_HISTORY") }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = invoice.invoiceNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = HotelNavy
                                    )
                                    Text(
                                        text = "${invoice.customerName} (Room ${invoice.roomNumber})",
                                        fontSize = 12.sp,
                                        color = Slate600
                                    )
                                    Text(
                                        text = DateUtils.formatDisplayDate(invoice.invoiceDate),
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StatusChip(status = invoice.paymentStatus)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CurrencyFormatter.formatWhole(invoice.grandTotal, settings.currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Slate800
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        modifier = Modifier.testTag("quick_action_${label.lowercase().replace(" ", "_")}")
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
