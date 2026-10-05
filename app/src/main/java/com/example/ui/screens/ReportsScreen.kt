package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.ui.components.StatCard
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
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.util.Calendar

@Composable
fun ReportsScreen(
    viewModel: HotelViewModel
) {
    val context = LocalContext.current
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf("THIS_MONTH") }

    // Time window calculation
    val (startTimestamp, endTimestamp) = remember(selectedPeriod) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "TODAY" -> Pair(DateUtils.getStartOfDay(), DateUtils.getEndOfDay())
            "YESTERDAY" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                Pair(DateUtils.getStartOfDay(cal), DateUtils.getEndOfDay(cal))
            }
            "THIS_WEEK" -> Pair(DateUtils.getStartOfWeek(), DateUtils.getEndOfDay())
            "THIS_MONTH" -> Pair(DateUtils.getStartOfMonth(), DateUtils.getEndOfDay())
            else -> Pair(0L, Long.MAX_VALUE)
        }
    }

    val periodPayments = remember(payments, startTimestamp, endTimestamp) {
        payments.filter { it.date in startTimestamp..endTimestamp }
    }
    val periodExpenses = remember(expenses, startTimestamp, endTimestamp) {
        expenses.filter { it.date in startTimestamp..endTimestamp }
    }
    val periodBookings = remember(bookings, startTimestamp, endTimestamp) {
        bookings.filter { it.checkInTimestamp in startTimestamp..endTimestamp || it.createdAt in startTimestamp..endTimestamp }
    }

    val totalRevenue = remember(periodPayments) { periodPayments.sumOf { it.amount } }
    val totalExpenses = remember(periodExpenses) { periodExpenses.sumOf { it.amount } }
    val netProfit = totalRevenue - totalExpenses

    // Food sales calculation from bookings
    val totalFoodSales = remember(periodBookings) {
        periodBookings.sumOf { it.mineralWater + it.ghavaneChatney + it.tea + it.kandePohe }
    }
    val totalPendingBalance = remember(bookings) {
        bookings.filter { it.status == "ACTIVE" }.sumOf { it.pending }
    }

    // Payment methods breakdown
    val paymentMethodsBreakdown = remember(periodPayments) {
        periodPayments.groupBy { it.paymentMethod }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Export
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hotel Financial & Operational Reports", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                    Text("All data computed offline from Room SQLite", fontSize = 12.sp, color = Slate500)
                }

                IconButton(
                    onClick = {
                        val file = CsvExporter.exportPaymentsToCsv(context, periodPayments)
                        CsvExporter.shareCsvFile(context, file, "Financial Revenue Report")
                    }
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export Report", tint = HotelNavy)
                }
            }
        }

        // Time Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(
                    "TODAY" to "Today",
                    "YESTERDAY" to "Yesterday",
                    "THIS_WEEK" to "This Week",
                    "THIS_MONTH" to "This Month",
                    "ALL_TIME" to "All Time"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedPeriod == key,
                        onClick = { selectedPeriod = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        // --- 1. P&L FINANCIAL SUMMARY ---
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HotelNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("PROFIT & LOSS OVERVIEW", color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Revenue (Inflow)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            Text(
                                CurrencyFormatter.format(totalRevenue, settings.currencySymbol),
                                color = Color(0xFF6EE7B7),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Expenses (Outflow)", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                            Text(
                                CurrencyFormatter.format(totalExpenses, settings.currencySymbol),
                                color = Color(0xFFFCA5A5),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.2f)))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NET OPERATING PROFIT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            CurrencyFormatter.format(netProfit, settings.currencySymbol),
                            color = if (netProfit >= 0) Color(0xFF34D399) else Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }

        // --- 2. OPERATIONAL METRICS ---
        item {
            Text("Operational Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate800)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Bookings in Period",
                    value = "${periodBookings.size}",
                    icon = Icons.Default.Assessment,
                    iconColor = HotelNavy,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Breakfast & Food Sales",
                    value = CurrencyFormatter.formatWhole(totalFoodSales, settings.currencySymbol),
                    icon = Icons.Default.Payments,
                    iconColor = HotelGold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Pending Dues (Active)",
                    value = CurrencyFormatter.formatWhole(totalPendingBalance, settings.currencySymbol),
                    icon = Icons.Default.TrendingDown,
                    iconColor = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Registered Guests",
                    value = "${customers.size}",
                    icon = Icons.Default.TrendingUp,
                    iconColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 3. PAYMENT METHOD BREAKDOWN ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Collections by Payment Mode", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)

                    if (paymentMethodsBreakdown.isEmpty()) {
                        Text("No payment transactions in selected period.", color = Slate500, fontSize = 13.sp)
                    } else {
                        paymentMethodsBreakdown.forEach { (method, amount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(method, fontSize = 13.sp, color = Slate700)
                                Text(
                                    CurrencyFormatter.format(amount, settings.currencySymbol),
                                    fontWeight = FontWeight.SemiBold,
                                    color = HotelNavy
                                )
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate100))
                        }
                    }
                }
            }
        }

        // --- 4. ROOM OCCUPANCY REPORT ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Room Occupancy Status", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Slate800)

                    rooms.forEach { room ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Room ${room.roomNumber} (${room.roomType})", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("Floor: ${room.floor}", fontSize = 11.sp, color = Slate500)
                            }
                            com.example.ui.components.StatusChip(status = room.status)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Slate100))
                    }
                }
            }
        }
    }
}
