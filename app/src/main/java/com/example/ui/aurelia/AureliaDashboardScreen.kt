package com.example.ui.aurelia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()

    val today = DateUtils.formatDisplayDate(System.currentTimeMillis())
    val occupancy =
        if (stats.occupiedRooms + stats.availableRooms + stats.reservedRooms > 0)
            "${(stats.occupiedRooms.toDouble() /
                    (stats.occupiedRooms + stats.availableRooms + stats.reservedRooms) * 100).toInt()}%"
        else "0%"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AureliaColors.Background),
        contentPadding = PaddingValues(AureliaDimens.Page),
        verticalArrangement = Arrangement.spacedBy(AureliaDimens.SectionGap)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AureliaHeroTitle(
                    title = "Hotel command center",
                    subtitle = "${settings.hotelName} · Live operational overview · $today"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AureliaButton(
                        "＋ New booking",
                        primary = true,
                        onClick = {
                            viewModel.editingBooking.value = null
                            onNavigate("NEW_BOOKING")
                        }
                    )
                    AureliaButton(
                        "Reservation",
                        onClick = { onNavigate("RESERVATIONS") }
                    )
                    AureliaButton(
                        "POS",
                        onClick = { onNavigate("FOOD") }
                    )
                }
            }
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 145.dp),
                modifier = Modifier.heightIn(min = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                item {
                    AureliaKpiCard(
                        "Occupancy",
                        occupancy,
                        "${stats.occupiedRooms} occupied · ${stats.availableRooms} available"
                    )
                }
                item {
                    AureliaKpiCard(
                        "Arrivals",
                        stats.todayCheckIns.toString(),
                        "Today"
                    )
                }
                item {
                    AureliaKpiCard(
                        "Departures",
                        stats.todayCheckOuts.toString(),
                        "Today"
                    )
                }
                item {
                    AureliaKpiCard(
                        "Reserved",
                        stats.reservedRooms.toString(),
                        "Confirmed"
                    )
                }
                item {
                    AureliaKpiCard(
                        "Guests in house",
                        stats.currentGuests.toString(),
                        "Active stays"
                    )
                }
                item {
                    AureliaKpiCard(
                        "Outstanding",
                        CurrencyFormatter.formatWhole(
                            stats.pendingPayments,
                            settings.currencySymbol
                        ),
                        "All active folios"
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AureliaCard(Modifier.weight(1.4f)) {
                    AureliaSectionHeader(
                        "Attention",
                        "Reports"
                    ) {
                        onNavigate("REPORTS")
                    }

                    Spacer(Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        AureliaAttentionRow(
                            "Pending dues across active folios",
                            CurrencyFormatter.formatWhole(
                                stats.pendingPayments,
                                settings.currencySymbol
                            ),
                            danger = stats.pendingPayments > 0
                        )
                        AureliaAttentionRow(
                            "Inventory below reorder level",
                            "—",
                            warning = true
                        )
                        AureliaAttentionRow(
                            "Rooms requiring housekeeping",
                            "—"
                        )
                        AureliaAttentionRow(
                            "Payment announcement",
                            "ON"
                        )
                    }
                }

                AureliaCard(Modifier.weight(1f)) {
                    AureliaSectionHeader(
                        "Recent activity",
                        "Audit"
                    ) {
                        onNavigate("REPORTS")
                    }

                    Spacer(Modifier.height(12.dp))

                    if (stats.recentBookings.isEmpty()) {
                        Text(
                            "No activity yet",
                            color = AureliaColors.Muted
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            stats.recentBookings.take(5).forEach { booking ->
                                Column {
                                    androidx.compose.material3.Text(
                                        booking.bookingId,
                                        color = AureliaColors.Ink,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                    androidx.compose.material3.Text(
                                        booking.customerName,
                                        color = AureliaColors.Muted,
                                        fontSize = AureliaTypography.Caption
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            AureliaCard {
                AureliaSectionHeader(
                    "Room command board",
                    "Manage rooms"
                ) {
                    onNavigate("ROOMS")
                }

                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rooms.forEach { room ->
                        AureliaRoomCard(
                            roomNumber = room.roomNumber,
                            roomType = room.roomType,
                            rate = CurrencyFormatter.formatWhole(
                                room.rate,
                                settings.currencySymbol
                            ),
                            status = room.status,
                            guest = null,
                            checkout = null,
                            onFolio = { onNavigate("PAYMENTS") },
                            onManage = {
                                viewModel.editingRoom.value = room
                                onNavigate("ROOMS")
                            }
                        )
                    }

                    if (rooms.isEmpty()) {
                        androidx.compose.material3.Text(
                            "No rooms configured.",
                            color = AureliaColors.Muted
                        )
                    }
                }
            }
        }
    }
}
