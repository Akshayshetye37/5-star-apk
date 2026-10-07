package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.aurelia.AureliaColors
import com.example.ui.aurelia.AureliaDashboardScreen
import com.example.ui.aurelia.AureliaDimens
import com.example.ui.screens.*
import com.example.ui.viewmodel.HotelViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelApp(viewModel: HotelViewModel) {
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val enabledSections by viewModel.enabledSections.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchText by remember { mutableStateOf("") }

    BackHandler(enabled = currentSection != "DASHBOARD") {
        if (!viewModel.navigateBack()) {
            viewModel.navigateTo("DASHBOARD")
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val bottomItems = listOf(
        Triple("DASHBOARD", "Home", Icons.Default.Dashboard),
        Triple("RESERVATIONS", "Reserve", Icons.Default.EventAvailable),
        Triple("BOOKINGS", "Stays", Icons.Default.BookOnline),
        Triple("PAYMENTS", "Folio", Icons.Default.AccountBalanceWallet)
    )

    Scaffold(
        containerColor = AureliaColors.Background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AureliaColors.Background)
                    .padding(
                        start = AureliaDimens.Page,
                        end = AureliaDimens.Page,
                        top = 12.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.hotelName,
                            color = AureliaColors.Ink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = getSectionTitle(currentSection, enabledSections),
                            color = AureliaColors.Muted,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo("SETTINGS") }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AureliaColors.Ink
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        viewModel.searchGlobally(it)
                        if (it.isNotBlank() && currentSection != "SEARCH") {
                            viewModel.navigateTo("SEARCH")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            "Search guests, bookings, rooms...",
                            color = AureliaColors.Muted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = AureliaColors.Brand
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AureliaColors.Brand,
                        unfocusedBorderColor = AureliaColors.Line,
                        focusedTextColor = AureliaColors.Ink,
                        unfocusedTextColor = AureliaColors.Ink,
                        cursorColor = AureliaColors.Brand,
                        focusedContainerColor = AureliaColors.Surface,
                        unfocusedContainerColor = AureliaColors.Surface
                    )
                )

                Spacer(Modifier.height(8.dp))
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = AureliaColors.Surface,
                tonalElevation = 0.dp
            ) {
                bottomItems.forEach { (id, label, icon) ->
                    NavigationBarItem(
                        selected = currentSection == id,
                        onClick = { viewModel.navigateTo(id) },
                        icon = {
                            Icon(icon, contentDescription = label)
                        },
                        label = {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AureliaColors.Brand,
                            selectedTextColor = AureliaColors.Brand,
                            indicatorColor = AureliaColors.Soft,
                            unselectedIconColor = AureliaColors.Muted,
                            unselectedTextColor = AureliaColors.Muted
                        )
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentSection) {
                "DASHBOARD" -> {
                    AureliaDashboardScreen(
                        viewModel = viewModel,
                        onNavigate = viewModel::navigateTo
                    )
                }

                "RESERVATIONS" -> ReservationsScreen(viewModel)

                "BOOKINGS" -> BookingsListScreen(viewModel)

                "PAYMENTS" -> PaymentsScreen(viewModel)

                "NEW_BOOKING" -> NewBookingScreen(viewModel)

                "ROOMS" -> RoomsScreen(viewModel)

                "CUSTOMERS" -> CustomersScreen(viewModel)

                "GUEST_DETAILS" -> GuestDetailsScreen(viewModel)

                "INVOICES" -> InvoiceHistoryScreen(viewModel)

                "INVOICE_GENERATOR" -> InvoiceGeneratorScreen(viewModel)

                "EXPENSES" -> ExpensesScreen(viewModel)

                "REPORTS" -> ReportsScreen(viewModel)

                "SETTINGS" -> SettingsScreen(viewModel)

                "BACKUP" -> BackupRestoreScreen(viewModel)

                "FOOD" -> FoodBreakfastScreen(viewModel)

                "SEARCH" -> GlobalSearchScreen(viewModel)

                "MANAGE_SECTIONS" -> ManageSectionsScreen(viewModel)

                else -> {
                    AureliaDashboardScreen(
                        viewModel = viewModel,
                        onNavigate = viewModel::navigateTo
                    )
                }
            }
        }
    }
}
