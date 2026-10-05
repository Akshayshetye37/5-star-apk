package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.InvalidationTracker
import com.example.data.database.AppDatabase
import com.example.data.model.AppSection
import com.example.data.model.BackupMetadata
import com.example.data.model.Booking
import com.example.data.model.BookingGuest
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.FoodItem
import com.example.data.model.FoodOrder
import com.example.data.model.HotelSettings
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Payment
import com.example.data.model.Reservation
import com.example.data.model.RoomEntity
import com.example.data.domain.PaymentEngineResult
import com.example.data.repository.FullDatabaseExport
import com.example.data.repository.HotelRepository
import com.example.util.BackupRestoreManager
import com.example.util.DateUtils
import com.example.util.DataExportManager
import com.example.util.GuestRoomPdfGenerator
import com.example.util.WorkbookImportManager
import com.example.util.ReminderScheduler
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DashboardStats(
    val todayCheckIns: Int = 0,
    val todayCheckOuts: Int = 0,
    val occupiedRooms: Int = 0,
    val availableRooms: Int = 0,
    val reservedRooms: Int = 0,
    val maintenanceRooms: Int = 0,
    val todayRevenue: Double = 0.0,
    val pendingPayments: Double = 0.0,
    val totalBookings: Int = 0,
    val currentGuests: Int = 0,
    val recentBookings: List<Booking> = emptyList(),
    val recentInvoices: List<Invoice> = emptyList(),
    val pendingBookings: List<Booking> = emptyList()
)

class HotelViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = HotelRepository(db)

    private var automaticBackupJob: Job? = null
    private val backupObserver = object : InvalidationTracker.Observer(
        "hotel_settings", "customers", "rooms", "reservations", "bookings",
        "booking_guests", "invoices", "invoice_items", "payments", "food_items",
        "food_orders", "expenses", "app_sections"
    ) {
        override fun onInvalidated(tables: Set<String>) {
            automaticBackupJob?.cancel()
            automaticBackupJob = viewModelScope.launch {
                delay(700)
                runAutomaticBackup()
            }
        }
    }

    // Current navigation section
    private val _currentSection = MutableStateFlow("DASHBOARD")
    val currentSection: StateFlow<String> = _currentSection.asStateFlow()

    // Navigation back stack for custom state switching
    private val sectionBackStack = mutableListOf("DASHBOARD")

    // Snackbar notification message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun navigateTo(section: String) {
        if (_currentSection.value != section) {
            sectionBackStack.add(section)
            _currentSection.value = section
        }
    }

    fun navigateBack(): Boolean {
        if (sectionBackStack.size > 1) {
            sectionBackStack.removeAt(sectionBackStack.lastIndex)
            _currentSection.value = sectionBackStack.last()
            return true
        }
        return false
    }

    // State flows from Room
    val settings: StateFlow<HotelSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HotelSettings()
    )

    val rooms = repository.allRoomsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customers = repository.allCustomersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bookings = repository.allBookingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeBookings = repository.activeBookingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val reservations = repository.allReservationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val invoices = repository.allInvoicesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val payments = repository.allPaymentsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val foodItems = repository.allFoodItemsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val expenses = repository.allExpensesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sections = repository.allSectionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val enabledSections = repository.enabledSectionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val backups = repository.allBackupsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Derived Dashboard Stats
    val dashboardStats: StateFlow<DashboardStats> = combine(
        bookings,
        rooms,
        payments,
        invoices
    ) { allBookings, allRooms, allPayments, allInvoices ->
        val todayStr = DateUtils.todayDateString()

        val checkIns = allBookings.count { it.status == "ACTIVE" && it.checkInDate == todayStr }
        val checkOuts = allBookings.count { it.status == "ACTIVE" && it.checkOutDate == todayStr }

        val occupied = allRooms.count { it.status == "OCCUPIED" }
        val available = allRooms.count { it.status == "AVAILABLE" }
        val reserved = allRooms.count { it.status == "RESERVED" }
        val maintenance = allRooms.count { it.status == "MAINTENANCE" }

        // Dashboard financial totals are derived from the payment ledger and
        // booking totals so they stay accurate after edits, imports and payments.
        // Revenue means money actually received today (not booking/invoice totals).
        val todayRev = allPayments
            .asSequence()
            .filter { it.amount > 0.0 && DateUtils.isToday(it.date) }
            .sumOf { it.amount }

        // Pending balance includes every non-cancelled booking, including
        // checked-out bookings that still have an outstanding amount.
        val pendingTotal = allBookings
            .asSequence()
            .filter { it.status != "CANCELLED" }
            .sumOf { it.pending }

        val guests = allBookings
            .filter { it.status == "ACTIVE" }
            .sumOf { it.numberOfGuests }

        val recentBk = allBookings.take(5)
        val recentInv = allInvoices.take(5)
        val pendingBk = allBookings.filter { it.status != "CANCELLED" && it.pending > 0.0 }.sortedByDescending { it.pending }.take(8)

        DashboardStats(
            todayCheckIns = checkIns,
            todayCheckOuts = checkOuts,
            occupiedRooms = occupied,
            availableRooms = available,
            reservedRooms = reserved,
            maintenanceRooms = maintenance,
            todayRevenue = todayRev,
            pendingPayments = pendingTotal,
            totalBookings = allBookings.size,
            currentGuests = guests,
            recentBookings = recentBk,
            recentInvoices = recentInv,
            pendingBookings = pendingBk
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    // Editing states for forms
    val selectedBookingForInvoice = MutableStateFlow<Booking?>(null)
    val editingBooking = MutableStateFlow<Booking?>(null)
    val editingReservation = MutableStateFlow<Reservation?>(null)
    val editingRoom = MutableStateFlow<RoomEntity?>(null)
    val editingCustomer = MutableStateFlow<Customer?>(null)
    val editingFoodItem = MutableStateFlow<FoodItem?>(null)
    val editingExpense = MutableStateFlow<Expense?>(null)
    val selectedCustomerHistory = MutableStateFlow<Customer?>(null)

    private val _globalSearchResults = MutableStateFlow<List<Booking>>(emptyList())
    val globalSearchResults: StateFlow<List<Booking>> = _globalSearchResults.asStateFlow()

    private var globalSearchJob: kotlinx.coroutines.Job? = null

    init {
        db.invalidationTracker.addObserver(backupObserver)
        viewModelScope.launch {
            AppDatabase.seedInitialData(db)
        }
    }

    override fun onCleared() {
        db.invalidationTracker.removeObserver(backupObserver)
        automaticBackupJob?.cancel()
        super.onCleared()
    }

    private suspend fun runAutomaticBackup() {
        try {
            val current = repository.getSettings()
            if (!current.localBackupEnabled && !(current.driveBackupEnabled && current.driveFolderUri.isNotBlank())) return
            val json = BackupRestoreManager.exportToJson(repository.getAllDataDirect())
            if (current.localBackupEnabled) {
                BackupRestoreManager.saveBackupToFile(getApplication(), json)
            }
            if (current.driveBackupEnabled && current.driveFolderUri.isNotBlank()) {
                BackupRestoreManager.writeBackupToTree(
                    getApplication(),
                    Uri.parse(current.driveFolderUri),
                    json
                )
            }
        } catch (_: Exception) {
            // Automatic backup must never block normal offline billing.
        }
    }

    // --- Bookings Actions ---
    fun saveBooking(
        booking: Booking,
        isEditing: Boolean,
        guests: List<BookingGuest> = emptyList(),
        initialPayment: Payment? = null,
        additionalPayments: List<Payment> = emptyList(),
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            // Check room availability
            val (isAvailable, message) = repository.checkRoomAvailability(
                roomNumber = booking.roomNumber,
                checkInTimestamp = booking.checkInTimestamp,
                checkOutTimestamp = booking.checkOutTimestamp,
                excludeBookingId = if (isEditing) booking.bookingId else null
            )

            if (!isAvailable) {
                onResult(false, message)
                return@launch
            }

            // Customer record
            val customer = Customer(
                customerId = booking.customerId.ifBlank { "CUST-${System.currentTimeMillis().toString().takeLast(6)}" },
                name = booking.customerName,
                idType = booking.idType,
                idNumber = booking.idNumber,
                phone = booking.contactNumber,
                whatsapp = booking.whatsappNumber,
                idPhotoUri = booking.idPhotoUri,
                lastVisit = System.currentTimeMillis()
            )

            val bookingToSave = booking.copy(
                customerId = customer.customerId,
                updatedAt = System.currentTimeMillis()
            )

            val res = repository.saveBooking(bookingToSave, customer, initialPayment, guests, additionalPayments)
            if (res.isSuccess) {
                ReminderScheduler.scheduleBookingReminders(
                    getApplication(),
                    bookingToSave.bookingId,
                    bookingToSave.customerName,
                    bookingToSave.roomNumber,
                    bookingToSave.checkOutTimestamp,
                    bookingToSave.pending,
                    repository.getSettings().currencySymbol
                )
                showMessage("Booking ${bookingToSave.bookingId} saved successfully!")
                onResult(true, "Booking saved.")
            } else {
                onResult(false, "Failed to save booking: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun checkoutBooking(bookingId: String) {
        viewModelScope.launch {
            repository.checkoutBooking(bookingId)
            showMessage("Booking $bookingId checked out successfully.")
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            repository.cancelBooking(bookingId)
            showMessage("Booking $bookingId cancelled.")
        }
    }

    fun deleteBooking(booking: Booking) {
        viewModelScope.launch {
            repository.deleteBooking(booking)
            showMessage("Booking ${booking.bookingId} deleted.")
        }
    }

    // --- Reservations Actions ---
    fun saveReservation(
        reservation: Reservation,
        isEditing: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val (isAvailable, message) = repository.checkRoomAvailability(
                roomNumber = reservation.roomNumber,
                checkInTimestamp = reservation.checkInTimestamp,
                checkOutTimestamp = reservation.checkOutTimestamp,
                excludeReservationId = if (isEditing) reservation.reservationId else null
            )

            if (!isAvailable) {
                onResult(false, message)
                return@launch
            }

            if (isEditing) {
                repository.updateReservation(reservation)
            } else {
                repository.saveReservation(reservation)
            }
            showMessage("Reservation ${reservation.reservationId} saved.")
            onResult(true, "Reservation saved.")
        }
    }

    fun convertReservationToBooking(reservation: Reservation) {
        val newBooking = Booking(
            bookingId = "BK-${SimpleDateFormat("yyMMdd-HHmm", Locale.getDefault()).format(Date())}",
            customerId = reservation.customerId,
            customerName = reservation.customerName,
            contactNumber = reservation.phone,
            roomNumber = reservation.roomNumber,
            roomType = reservation.roomType,
            checkInDate = reservation.checkInDate,
            checkInTime = reservation.checkInTime,
            checkOutDate = reservation.checkOutDate,
            checkOutTime = reservation.checkOutTime,
            checkInTimestamp = reservation.checkInTimestamp,
            checkOutTimestamp = reservation.checkOutTimestamp,
            numberOfGuests = reservation.guests,
            advance = reservation.advance,
            paid = reservation.advance,
            notes = reservation.notes
        )
        editingBooking.value = newBooking
        // Mark reservation completed
        viewModelScope.launch {
            repository.updateReservation(reservation.copy(status = "CHECKED-IN"))
        }
        navigateTo("NEW_BOOKING")
    }

    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            repository.getReservation(reservationId)?.let {
                repository.updateReservation(it.copy(status = "CANCELLED"))
                showMessage("Reservation $reservationId cancelled.")
            }
        }
    }

    fun deleteReservation(reservation: Reservation) {
        viewModelScope.launch {
            repository.deleteReservation(reservation)
            showMessage("Reservation deleted.")
        }
    }

    // --- Room Management ---
    fun saveRoom(room: RoomEntity, isEditing: Boolean) {
        viewModelScope.launch {
            if (isEditing) {
                repository.updateRoom(room)
                showMessage("Room ${room.roomNumber} updated.")
            } else {
                repository.insertRoom(room)
                showMessage("Room ${room.roomNumber} added.")
            }
        }
    }

    fun updateRoomStatus(roomNumber: String, status: String) {
        viewModelScope.launch {
            repository.updateRoomStatus(roomNumber, status)
            showMessage("Room $roomNumber status set to $status.")
        }
    }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch {
            repository.deleteRoom(room)
            showMessage("Room ${room.roomNumber} deleted.")
        }
    }

    // --- Customers ---
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showMessage("Customer ${customer.name} saved.")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            showMessage("Customer deleted.")
        }
    }

    // --- Invoices ---
    fun saveInvoice(invoice: Invoice, items: List<InvoiceItem>) {
        viewModelScope.launch {
            repository.saveInvoice(invoice, items)
            showMessage("Invoice ${invoice.invoiceNumber} generated successfully.")
        }
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            showMessage("Invoice ${invoice.invoiceNumber} deleted.")
        }
    }

    // --- Payments ---

    /**
     * New authoritative payment entry point.
     *
     * IMPORTANT:
     * verified must only be true when the payment has been
     * confirmed by a trusted provider/reconciliation mechanism.
     *
     * QR display, opening Google Pay/PhonePe/Paytm, returning
     * from the payment application, screenshots, or manually
     * typed UTR values are NOT payment verification.
     */
    fun recordVerifiedPayment(
        payment: Payment,
        folioId: String,
        requestUuid: String = UUID.randomUUID().toString(),
        verified: Boolean,
        onResult: ((PaymentEngineResult) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val result = repository.recordVerifiedPayment(
                    bookingId = payment.bookingId,
                    folioId = folioId,
                    amount = payment.amount,
                    paymentMethod = payment.paymentMethod,
                    referenceNumber = payment.referenceNumber,
                    requestUuid = requestUuid,
                    verified = verified,
                    customerId = payment.customerId,
                    customerName = payment.customerName,
                    invoiceId = payment.invoiceId,
                    notes = payment.notes,
                    timestamp = payment.date
                )

                when (result) {
                    is PaymentEngineResult.Success -> {
                        showMessage(
                            "Payment of ${payment.amount} recorded successfully."
                        )
                    }

                    is PaymentEngineResult.AlreadyProcessed -> {
                        showMessage(
                            "Payment was already processed."
                        )
                    }

                    is PaymentEngineResult.Rejected -> {
                        showMessage(
                            "Payment rejected: ${result.reason}"
                        )
                    }
                }

                onResult?.invoke(result)

            } catch (error: Exception) {
                showMessage(
                    "Payment failed: ${error.message ?: "Unknown error"}"
                )

                onResult?.invoke(
                    PaymentEngineResult.Rejected(
                        error.message ?: "Unknown payment error"
                    )
                )
            }
        }
    }

    /**
     * Legacy compatibility path.
     *
     * Existing screens may still call this until they are migrated
     * to recordVerifiedPayment().
     *
     * It is intentionally NOT considered verified and therefore
     * cannot trigger the UPI payment announcement.
     */
    fun recordPayment(payment: Payment) {
        viewModelScope.launch {
            repository.recordPayment(payment)
            showMessage("Payment of ${payment.amount} recorded.")
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            showMessage("Payment removed.")
        }
    }

    // --- Food Items & Orders ---
    fun saveFoodItem(item: FoodItem) {
        viewModelScope.launch {
            repository.saveFoodItem(item)
            showMessage("Food item ${item.name} saved.")
        }
    }

    fun deleteFoodItem(item: FoodItem) {
        viewModelScope.launch {
            repository.deleteFoodItem(item)
            showMessage("Food item deleted.")
        }
    }

    fun addFoodOrder(order: FoodOrder) {
        viewModelScope.launch {
            repository.addFoodOrder(order)
            showMessage("Added ${order.foodName} to room ${order.roomNumber}.")
        }
    }

    // --- Expenses ---
    fun saveExpense(expense: Expense) {
        viewModelScope.launch {
            repository.saveExpense(expense)
            showMessage("Expense saved.")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            showMessage("Expense deleted.")
        }
    }

    // --- App Sections ---
    fun updateSection(section: AppSection) {
        viewModelScope.launch {
            repository.saveSection(section)
            showMessage("Section updated.")
        }
    }

    fun toggleSection(section: AppSection) {
        viewModelScope.launch {
            repository.saveSection(section.copy(isEnabled = !section.isEnabled))
            showMessage("${section.title} ${if (!section.isEnabled) "enabled" else "hidden"}.")
        }
    }

    // --- Settings ---
    fun updateSettings(settings: HotelSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            showMessage("Hotel settings updated.")
        }
    }

    // --- Backup & Restore ---
    fun exportData(context: Context, selected: Set<String>, format: String) {
        viewModelScope.launch {
            try {
                val file = DataExportManager.export(context, repository.getAllDataDirect(), selected, format)
                DataExportManager.share(
                    context,
                    file,
                    when (format.uppercase()) {
                        "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        else -> "application/pdf"
                    }
                )
            } catch (e: Exception) {
                showMessage("Export failed: ${e.message}")
            }
        }
    }


    fun exportHotelWorkbook(
        context: Context,
        fromDate: String? = null,
        toDate: String? = null,
        bookingIds: Set<String> = emptySet(),
        customerNameQuery: String? = null
    ) {
        viewModelScope.launch {
            try {
                val file = DataExportManager.exportHotelWorkbook(
                    context,
                    repository.getAllDataDirect(),
                    fromDate,
                    toDate,
                    bookingIds,
                    customerNameQuery
                )
                DataExportManager.share(
                    context,
                    file,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            } catch (e: Exception) {
                showMessage("Workbook export failed: ${e.message}")
            }
        }
    }

    /**
     * Imports the supplied hotel workbook. Matching rows can optionally be grouped
     * into one booking when room + stay + contact information is identical.
     * Existing booking IDs are skipped to protect existing records.
     */
    fun importHotelWorkbook(
        context: Context,
        uri: Uri,
        groupMatchingStays: Boolean,
        onComplete: (Int, Int, Int) -> Unit = { _, _, _ -> }
    ) {
        viewModelScope.launch {
            var imported = 0
            var skipped = 0
            var failed = 0
            try {
                val parsed = WorkbookImportManager.parse(context, uri)
                val groups = if (groupMatchingStays) {
                    parsed.rows.groupBy {
                        listOf(it.roomNumber, it.checkInDate, it.checkOutDate, it.contact, it.whatsapp).joinToString("|")
                    }.values.toList()
                } else {
                    parsed.rows.map { listOf(it) }
                }

                for (group in groups) {
                    val first = group.first()
                    val baseId = first.bookingId.ifBlank { "IMP-${first.rowNumber}" }
                    val bookingId = if (repository.getBooking(baseId) != null) {
                        skipped++
                        continue
                    } else baseId

                    val checkIn = first.checkInDate.ifBlank { DateUtils.todayDateString() }
                    val checkOut = first.checkOutDate.ifBlank { checkIn }
                    val inTs = DateUtils.parseDateTimeToTimestamp(checkIn, "12:00 PM")
                    val outTs = DateUtils.parseDateTimeToTimestamp(
                        checkOut,
                        "11:00 AM"
                    ).let { if (it <= inTs) inTs + 86_400_000L else it }

                    val status = if (outTs < System.currentTimeMillis()) "CHECKED_OUT" else "ACTIVE"
                    val computedTotal = (
                        first.roomCharges +
                            first.mineralWater +
                            first.ghavaneChatney +
                            first.tea +
                            first.kandePohe -
                            first.discount
                        ).coerceAtLeast(0.0)
                    val paid = if (first.paid > 0) first.paid else first.advance
                    val customerId = "CUST-IMP-${System.currentTimeMillis()}-${first.rowNumber}"

                    val guests = group.mapIndexed { index, r ->
                        com.example.data.model.BookingGuest(
                            guestId = "GUEST-$bookingId-${index + 1}",
                            bookingId = bookingId,
                            sequence = index + 1,
                            name = r.customerName,
                            idType = r.idType.ifBlank { "Aadhaar Card" },
                            idNumber = r.idNumber,
                            phone = r.contact,
                            whatsapp = r.whatsapp.ifBlank { r.contact },
                            idPhotoUri = r.idPhotoPath,
                            vehicleNumbers = ""
                        )
                    }

                    val booking = Booking(
                        bookingId = bookingId,
                        customerId = customerId,
                        customerName = first.customerName,
                        idType = first.idType.ifBlank { "Aadhaar Card" },
                        idNumber = first.idNumber,
                        contactNumber = first.contact,
                        whatsappNumber = first.whatsapp.ifBlank { first.contact },
                        checkInDate = checkIn,
                        checkInTime = "12:00 PM",
                        checkOutDate = checkOut,
                        checkOutTime = "11:00 AM",
                        checkInTimestamp = inTs,
                        checkOutTimestamp = outTs,
                        numberOfGuests = guests.size,
                        roomNumber = first.roomNumber,
                        roomType = first.roomType.ifBlank { "Imported" },
                        roomCharges = first.roomCharges,
                        advance = first.advance,
                        paid = paid,
                        discount = first.discount,
                        mineralWater = first.mineralWater,
                        ghavaneChatney = first.ghavaneChatney,
                        tea = first.tea,
                        kandePohe = first.kandePohe,
                        otherCharges = 0.0,
                        notes = buildString {
                            append("Imported from Excel row ${first.rowNumber}.")
                            if (first.rawColumns.isNotEmpty()) {
                                append("\n\nOriginal workbook columns:")
                                first.rawColumns.forEach { (key, value) ->
                                    append("\n• ").append(key).append(": ").append(value)
                                }
                            }
                        },
                        idPhotoUri = first.idPhotoPath,
                        status = status
                    )

                    val payment = if (paid > 0) Payment(
                        paymentId = "PAY-IMP-${System.currentTimeMillis()}-${first.rowNumber}",
                        bookingId = bookingId,
                        customerId = customerId,
                        customerName = first.customerName,
                        amount = paid,
                        paymentMethod = "Imported Workbook",
                        notes = "Imported from Excel"
                    ) else null

                    val result = repository.saveBooking(
                        booking = booking,
                        customerToUpsert = Customer(
                            customerId = customerId,
                            name = first.customerName,
                            idType = first.idType.ifBlank { "Aadhaar Card" },
                            idNumber = first.idNumber,
                            phone = first.contact,
                            whatsapp = first.whatsapp.ifBlank { first.contact }
                        ),
                        initialPayment = payment,
                        guests = guests
                    )
                    if (result.isSuccess) imported++ else failed++
                }

                val warningCount = parsed.warnings.size
                showMessage("Workbook import complete: $imported imported, $skipped skipped, $failed failed${if (warningCount > 0) ", $warningCount warnings" else ""}.")
                onComplete(imported, skipped, failed)
            } catch (e: Exception) {
                showMessage("Workbook import failed: ${e.message}")
                onComplete(imported, skipped, failed + 1)
            }
        }
    }

    fun createBackup(onSuccess: (File) -> Unit) {
        viewModelScope.launch {
            val allData = repository.getAllDataDirect()
            val json = BackupRestoreManager.exportToJson(allData)
            val file = BackupRestoreManager.saveBackupToFile(getApplication(), json)

            val meta = BackupMetadata(
                backupId = "BCK-${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                deviceInfo = android.os.Build.MODEL,
                recordsCount = allData.bookings.size + allData.invoices.size + allData.customers.size,
                notes = "Local full backup"
            )
            repository.recordBackup(meta)
            showMessage("Backup created: ${file.name}")
            onSuccess(file)
        }
    }

    fun restoreBackup(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val data = BackupRestoreManager.parseJsonToData(jsonString)
                repository.restoreFullDatabase(data)
                showMessage("Database restored successfully!")
                onResult(true, "Restore completed.")
            } catch (e: Exception) {
                showMessage("Failed to restore: ${e.message}")
                onResult(false, e.message ?: "Invalid backup file.")
            }
        }
    }

    fun searchGlobally(query: String) {
        globalSearchJob?.cancel()
        val cleaned = query.trim()
        if (cleaned.isBlank()) {
            _globalSearchResults.value = emptyList()
            return
        }
        globalSearchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(150)
            repository.searchBookings(cleaned).collect { results ->
                _globalSearchResults.value = results
            }
        }
    }

    fun clearGlobalSearch() {
        globalSearchJob?.cancel()
        _globalSearchResults.value = emptyList()
    }

    fun openGuestDetails(booking: Booking) {
        selectedBookingForInvoice.value = booking
        navigateTo("GUEST_DETAILS")
    }

    fun getCurrentActiveBookingForRoom(roomNumber: String, onResult: (Booking?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getCurrentActiveBookingForRoom(roomNumber))
        }
    }

    fun shareGuestRoomPdf(context: Context, booking: Booking) {
        viewModelScope.launch {
            val customer = booking.customerId.takeIf { it.isNotBlank() }?.let { repository.getCustomer(it) }
            val payments = repository.getPaymentsForBookingDirect(booking.bookingId)
            val reservationId = booking.customerId.takeIf { it.isNotBlank() }?.let {
                repository.getLatestReservationForGuest(it, booking.roomNumber)?.reservationId
            }.orEmpty()
            val guests = repository.getGuestsForBookingDirect(booking.bookingId)
            val file = GuestRoomPdfGenerator.generate(context, settings.value, booking, customer, payments, reservationId, guests)
            GuestRoomPdfGenerator.share(context, file)
        }
    }

    fun shareInvoicePdf(context: Context, booking: Booking) {
        viewModelScope.launch {
            try {
                val existing = repository.getInvoiceForBooking(booking.bookingId)
                val invoice = existing ?: Invoice(
                    invoiceNumber = "${settings.value.invoicePrefix}${booking.bookingId}",
                    bookingId = booking.bookingId,
                    customerId = booking.customerId,
                    customerName = booking.customerName,
                    customerContact = booking.contactNumber,
                    roomNumber = booking.roomNumber,
                    roomType = booking.roomType,
                    checkInDate = booking.checkInDate,
                    checkOutDate = booking.checkOutDate,
                    guests = booking.numberOfGuests,
                    roomCharges = booking.roomCharges,
                    foodCharges = booking.mineralWater + booking.ghavaneChatney + booking.tea + booking.kandePohe,
                    otherCharges = booking.otherCharges,
                    discount = booking.discount,
                    grandTotal = booking.grandTotal,
                    advance = booking.advance,
                    paid = booking.paid,
                    pending = booking.pending,
                    paymentMethod = if (booking.advance > 0 && booking.paid > booking.advance) "Cash + UPI" else if (booking.paid > 0) "Cash" else "Pending",
                    paymentStatus = if (booking.pending <= 0) "PAID" else if (booking.paid > 0) "PARTIALLY_PAID" else "PENDING"
                )
                val file = com.example.util.PdfInvoiceGenerator.generateInvoicePdf(
                    context, settings.value, invoice, repository.getItemsForInvoiceDirect(invoice.invoiceNumber)
                )
                com.example.util.PdfInvoiceGenerator.shareInvoicePdf(context, file)
            } catch (e: Exception) {
                showMessage("Invoice PDF failed: ${e.message}")
            }
        }
    }

    fun generateGuestRoomPdf(context: Context, booking: Booking, onGenerated: (File) -> Unit) {
        viewModelScope.launch {
            val customer = booking.customerId.takeIf { it.isNotBlank() }?.let { repository.getCustomer(it) }
            val payments = repository.getPaymentsForBookingDirect(booking.bookingId)
            val reservationId = booking.customerId.takeIf { it.isNotBlank() }?.let {
                repository.getLatestReservationForGuest(it, booking.roomNumber)?.reservationId
            }.orEmpty()
            val guests = repository.getGuestsForBookingDirect(booking.bookingId)
            val file = GuestRoomPdfGenerator.generate(context, settings.value, booking, customer, payments, reservationId, guests)
            onGenerated(file)
        }
    }

    suspend fun getCustomerForBooking(booking: Booking): Customer? =
        booking.customerId.takeIf { it.isNotBlank() }?.let { repository.getCustomer(it) }

    suspend fun getGuestsForBookingDirect(bookingId: String): List<BookingGuest> =
        repository.getGuestsForBookingDirect(bookingId)

    // Local ID Photo copying to app private storage
    fun copyImageToInternalStorage(uri: Uri): String {
        return try {
            val context = getApplication<Application>()
            val input = context.contentResolver.openInputStream(uri) ?: return ""
            val photosDir = File(context.filesDir, "id_photos").apply { mkdirs() }
            val destFile = File(photosDir, "id_${UUID.randomUUID()}.jpg")
            val output = FileOutputStream(destFile)
            input.copyTo(output)
            input.close()
            output.close()
            destFile.absolutePath
        } catch (e: Exception) {
            ""
        }
    }
}
