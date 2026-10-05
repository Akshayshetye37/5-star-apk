package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.BookingsListScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.FoodBreakfastScreen
import com.example.ui.screens.GlobalSearchScreen
import com.example.ui.screens.GuestDetailsScreen
import com.example.ui.screens.InvoiceGeneratorScreen
import com.example.ui.screens.InvoiceHistoryScreen
import com.example.ui.screens.ManageSectionsScreen
import com.example.ui.screens.NewBookingScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ReservationsScreen
import com.example.ui.screens.RoomsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.HotelGold
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.viewmodel.HotelViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotelApp(viewModel: HotelViewModel) {
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val enabledSections by viewModel.enabledSections.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var globalSearchText by remember { mutableStateOf("") }

    // Intercept back button to navigate to Dashboard or pop custom state stack
    BackHandler(enabled = currentSection != "DASHBOARD") {
        if (!viewModel.navigateBack()) {
            viewModel.navigateTo("DASHBOARD")
        }
    }

    // Snackbar notifications
    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight(),
                drawerContainerColor = Color.White
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HotelNavy)
                        .padding(20.dp)
                ) {
                    Text(
                        text = settings.hotelName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Native Offline Management",
                        color = HotelGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Enabled sections list in Drawer
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    items(enabledSections, key = { it.id }) { section ->
                        val isSelected = currentSection == section.id

                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = getSectionIcon(section.id),
                                    contentDescription = null,
                                    tint = if (isSelected) HotelNavy else Slate700
                                )
                            },
                            label = {
                                Text(
                                    text = section.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.navigateTo(section.id)
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = HotelNavy.copy(alpha = 0.12f),
                                selectedTextColor = HotelNavy,
                                unselectedTextColor = Slate800
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .testTag("nav_drawer_item_${section.id.lowercase()}")
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.fillMaxWidth().background(HotelNavy)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = getSectionTitle(currentSection, enabledSections),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = settings.hotelName,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    },
                    navigationIcon = {
                        if (currentSection == "DASHBOARD") {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = {
                                if (!viewModel.navigateBack()) {
                                    viewModel.navigateTo("DASHBOARD")
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.navigateTo("SETTINGS") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HotelNavy,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = globalSearchText,
                    onValueChange = {
                        globalSearchText = it
                        viewModel.searchGlobally(it)
                        if (it.isNotBlank() && currentSection != "SEARCH") viewModel.navigateTo("SEARCH")
                    },
                    placeholder = { Text("Search guest name or phone number", color = Color.White.copy(alpha = 0.65f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = HotelGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = HotelGold
                    )
                )
                }
            },
            bottomBar = {
                // Bottom Bar showing key quick tabs
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val bottomItems = listOf(
                        Triple("DASHBOARD", "Dashboard", Icons.Default.Dashboard),
                        Triple("NEW_BOOKING", "New Stay", Icons.Default.AddCircle),
                        Triple("BOOKINGS", "Bookings", Icons.Default.BookOnline),
                        Triple("ROOMS", "Rooms", Icons.Default.MeetingRoom),
                        Triple("INVOICE_GENERATOR", "Invoice", Icons.Default.Receipt)
                    )

                    bottomItems.forEach { (id, label, icon) ->
                        val isSelected = currentSection == id
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(id) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("bottom_nav_${id.lowercase()}")
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = HotelNavy,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentSection) {
                    "SEARCH" -> GlobalSearchScreen(viewModel = viewModel)
                    "GUEST_DETAILS" -> viewModel.selectedBookingForInvoice.value?.let { booking ->
                        GuestDetailsScreen(
                            viewModel = viewModel,
                            booking = booking,
                            onEdit = {
                                viewModel.editingBooking.value = booking
                                viewModel.navigateTo("NEW_BOOKING")
                            },
                            onAddPayment = { viewModel.navigateTo("PAYMENTS") },
                            onGenerateInvoice = {
                                viewModel.selectedBookingForInvoice.value = booking
                                viewModel.navigateTo("INVOICE_GENERATOR")
                            }
                        )
                    } ?: DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { sec -> viewModel.navigateTo(sec) }
                    )
                    "DASHBOARD" -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { sec -> viewModel.navigateTo(sec) }
                    )
                    "NEW_BOOKING" -> NewBookingScreen(
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateTo("BOOKINGS") },
                        onBookingSaved = { viewModel.navigateTo("BOOKINGS") }
                    )
                    "BOOKINGS" -> BookingsListScreen(
                        viewModel = viewModel,
                        onNavigateToNewBooking = { viewModel.navigateTo("NEW_BOOKING") },
                        onGenerateInvoice = { booking ->
                            viewModel.selectedBookingForInvoice.value = booking
                            viewModel.navigateTo("INVOICE_GENERATOR")
                        },
                        onOpenDetails = { booking ->
                            viewModel.openGuestDetails(booking)
                        }
                    )
                    "RESERVATIONS" -> ReservationsScreen(viewModel = viewModel)
                    "ROOMS" -> RoomsScreen(viewModel = viewModel)
                    "CUSTOMERS" -> CustomersScreen(viewModel = viewModel)
                    "INVOICE_GENERATOR" -> InvoiceGeneratorScreen(
                        viewModel = viewModel,
                        onViewInvoices = { viewModel.navigateTo("INVOICE_HISTORY") }
                    )
                    "INVOICE_HISTORY" -> InvoiceHistoryScreen(viewModel = viewModel)
                    "FOOD" -> FoodBreakfastScreen(viewModel = viewModel)
                    "PAYMENTS" -> PaymentsScreen(viewModel = viewModel)
                    "EXPENSES" -> ExpensesScreen(viewModel = viewModel)
                    "REPORTS" -> ReportsScreen(viewModel = viewModel)
                    "BACKUP_RESTORE" -> BackupRestoreScreen(viewModel = viewModel)
                    "SETTINGS" -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToManageSections = { viewModel.navigateTo("MANAGE_SECTIONS") },
                        onNavigateToBackup = { viewModel.navigateTo("BACKUP_RESTORE") }
                    )
                    "MANAGE_SECTIONS" -> ManageSectionsScreen(viewModel = viewModel)
                    else -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { sec -> viewModel.navigateTo(sec) }
                    )
                }
            }
        }
    }
}

fun getSectionTitle(sectionId: String, sections: List<com.example.data.model.AppSection>): String {
    val sec = sections.find { it.id == sectionId }
    if (sec != null) return sec.title
    return when (sectionId) {
        "SEARCH" -> "Global Search"
        "GUEST_DETAILS" -> "Guest / Person Details"
        "DASHBOARD" -> "Dashboard"
        "NEW_BOOKING" -> "New Booking"
        "BOOKINGS" -> "Bookings & Guests"
        "RESERVATIONS" -> "Room Reservations"
        "ROOMS" -> "Rooms & Rates"
        "CUSTOMERS" -> "Guest Database"
        "INVOICE_GENERATOR" -> "Generate Invoice"
        "INVOICE_HISTORY" -> "Invoice History"
        "FOOD" -> "Breakfast & Food Menu"
        "PAYMENTS" -> "Payment Ledger"
        "EXPENSES" -> "Hotel Expenses"
        "REPORTS" -> "Financial Reports"
        "BACKUP_RESTORE" -> "Backup & Restore"
        "SETTINGS" -> "Settings"
        "MANAGE_SECTIONS" -> "Manage Sections"
        else -> "Hotel Billing System"
    }
}

fun getSectionIcon(sectionId: String): ImageVector {
    return when (sectionId) {
        "SEARCH" -> Icons.Default.Search
        "DASHBOARD" -> Icons.Default.Dashboard
        "NEW_BOOKING" -> Icons.Default.AddCircle
        "BOOKINGS" -> Icons.Default.BookOnline
        "RESERVATIONS" -> Icons.Default.EventAvailable
        "ROOMS" -> Icons.Default.MeetingRoom
        "CUSTOMERS" -> Icons.Default.People
        "INVOICE_GENERATOR" -> Icons.Default.Receipt
        "INVOICE_HISTORY" -> Icons.Default.History
        "FOOD" -> Icons.Default.Restaurant
        "PAYMENTS" -> Icons.Default.Payments
        "EXPENSES" -> Icons.Default.AccountBalanceWallet
        "REPORTS" -> Icons.Default.Assessment
        "BACKUP_RESTORE" -> Icons.Default.Backup
        "SETTINGS" -> Icons.Default.Settings
        else -> Icons.Default.Dashboard
    }
}
