package com.example.ui.aurelia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun AureliaDashboardScreen(
    viewModel: HotelViewModel,
    onNavigate: (String) -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AureliaColors.Background),
        contentPadding = PaddingValues(AureliaDimens.Page),
        verticalArrangement = Arrangement.spacedBy(AureliaDimens.SectionGap)
    ) {
        item {
            Column {
                Text(
                    "Good day",
                    color = AureliaColors.Muted,
                    fontSize = AureliaTypography.Body
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    settings.hotelName,
                    color = AureliaColors.Ink,
                    fontSize = AureliaTypography.Hero,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    DateUtils.formatDisplayDate(System.currentTimeMillis()),
                    color = AureliaColors.Muted,
                    fontSize = AureliaTypography.Caption
                )
            }
        }

        item {
            AureliaCard {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(AureliaColors.Ink)
                        .padding(20.dp)
                ) {
                    Text(
                        "Aurelia Hotel Operations",
                        color = Color.White.copy(alpha = .65f),
                        fontSize = AureliaTypography.Caption
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Everything under control.",
                        color = Color.White,
                        fontSize = AureliaTypography.Title,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Revenue", color = Color.White.copy(alpha = .65f), fontSize = 11.sp)
                            Text(
                                CurrencyFormatter.formatWhole(
                                    stats.todayRevenue,
                                    settings.currencySymbol
                                ),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Outstanding", color = Color.White.copy(alpha = .65f), fontSize = 11.sp)
                            Text(
                                CurrencyFormatter.formatWhole(
                                    stats.pendingPayments,
                                    settings.currencySymbol
                                ),
                                color = AureliaColors.Gold,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AureliaMetricCard(
                    "Revenue",
                    CurrencyFormatter.formatWhole(stats.todayRevenue, settings.currencySymbol),
                    "Today's payments",
                    Icons.Default.Payments,
                    Modifier.weight(1f)
                ) { onNavigate("PAYMENTS") }

                AureliaMetricCard(
                    "Pending",
                    CurrencyFormatter.formatWhole(stats.pendingPayments, settings.currencySymbol),
                    "Outstanding",
                    Icons.Default.Receipt,
                    Modifier.weight(1f),
                    AureliaColors.Danger
                ) { onNavigate("INVOICE_HISTORY") }
            }
        }

        item {
            Text(
                "Hotel operations",
                color = AureliaColors.Ink,
                fontSize = AureliaTypography.Section,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AureliaAction(
                    "New stay",
                    "Create booking",
                    Icons.Default.BookOnline,
                    Modifier.weight(1f)
                ) {
                    viewModel.editingBooking.value = null
                    onNavigate("NEW_BOOKING")
                }

                AureliaAction(
                    "Reservation",
                    "Reserve room",
                    Icons.Default.EventAvailable,
                    Modifier.weight(1f)
                ) {
                    onNavigate("RESERVATIONS")
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AureliaAction(
                    "Rooms",
                    "Availability",
                    Icons.Default.MeetingRoom,
                    Modifier.weight(1f)
                ) { onNavigate("ROOMS") }

                AureliaAction(
                    "Guests",
                    "Guest database",
                    Icons.Default.People,
                    Modifier.weight(1f)
                ) { onNavigate("CUSTOMERS") }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AureliaAction(
                    "Invoice",
                    "Create invoice",
                    Icons.Default.Receipt,
                    Modifier.weight(1f)
                ) { onNavigate("INVOICE_GENERATOR") }

                AureliaAction(
                    "Payment",
                    "Record payment",
                    Icons.Default.AddCircle,
                    Modifier.weight(1f)
                ) { onNavigate("PAYMENTS") }
            }
        }
    }
}
