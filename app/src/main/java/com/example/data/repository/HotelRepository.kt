package com.example.data.repository

import androidx.room.withTransaction
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HotelRepository(private val database: AppDatabase) {

    private val settingsDao = database.hotelSettingsDao()
    private val customerDao = database.customerDao()
    private val roomDao = database.roomDao()
    private val reservationDao = database.reservationDao()
    private val bookingDao = database.bookingDao()
    private val bookingGuestDao = database.bookingGuestDao()
    private val invoiceDao = database.invoiceDao()
    private val paymentDao = database.paymentDao()
    private val foodDao = database.foodDao()
    private val expenseDao = database.expenseDao()
    private val appSectionDao = database.appSectionDao()
    private val backupMetadataDao = database.backupMetadataDao()

    // --- Settings ---
    val settingsFlow: Flow<HotelSettings> = settingsDao.getSettings().map {
        it ?: HotelSettings()
    }

    suspend fun getSettings(): HotelSettings {
        return withContext(Dispatchers.IO) {
            settingsDao.getSettingsDirect() ?: HotelSettings().also {
                settingsDao.saveSettings(it)
            }
        }
    }

    suspend fun saveSettings(settings: HotelSettings) {
        withContext(Dispatchers.IO) {
            settingsDao.saveSettings(settings.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    // --- Rooms ---
    val allRoomsFlow: Flow<List<RoomEntity>> = roomDao.getAllRooms()

    suspend fun getRoom(roomNumber: String): RoomEntity? = withContext(Dispatchers.IO) {
        roomDao.getRoomByNumber(roomNumber)
    }

    suspend fun insertRoom(room: RoomEntity) = withContext(Dispatchers.IO) {
        roomDao.insertRoom(room.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateRoom(room: RoomEntity) = withContext(Dispatchers.IO) {
        roomDao.updateRoom(room.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteRoom(room: RoomEntity) = withContext(Dispatchers.IO) {
        roomDao.deleteRoom(room)
    }

    suspend fun updateRoomStatus(roomNumber: String, status: String) = withContext(Dispatchers.IO) {
        roomDao.updateStatus(roomNumber, status)
    }

    // --- Availability Check ---
    suspend fun checkRoomAvailability(
        roomNumber: String,
        checkInTimestamp: Long,
        checkOutTimestamp: Long,
        excludeBookingId: String? = null,
        excludeReservationId: String? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (checkOutTimestamp <= checkInTimestamp) {
            return@withContext Pair(false, "Check-out time must be after check-in time.")
        }

        val conflictingBookings = bookingDao.findConflictingBookings(
            roomNumber = roomNumber,
            checkInTimestamp = checkInTimestamp,
            checkOutTimestamp = checkOutTimestamp,
            excludeId = excludeBookingId
        )

        if (conflictingBookings.isNotEmpty()) {
            val conflict = conflictingBookings.first()
            return@withContext Pair(
                false,
                "Room $roomNumber is already booked by ${conflict.customerName} (${conflict.checkInDate} to ${conflict.checkOutDate})."
            )
        }

        val conflictingReservations = reservationDao.findConflictingReservations(
            roomNumber = roomNumber,
            checkInTimestamp = checkInTimestamp,
            checkOutTimestamp = checkOutTimestamp,
            excludeId = excludeReservationId
        )

        if (conflictingReservations.isNotEmpty()) {
            val conflict = conflictingReservations.first()
            return@withContext Pair(
                false,
                "Room $roomNumber is already reserved for ${conflict.customerName} (${conflict.checkInDate} to ${conflict.checkOutDate})."
            )
        }

        Pair(true, "Room is available.")
    }

    // --- Customers ---
    val allCustomersFlow: Flow<List<Customer>> = customerDao.getAllCustomers()

    fun searchCustomers(query: String): Flow<List<Customer>> = customerDao.searchCustomers(query)

    suspend fun getCustomer(id: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(id)
    }

    suspend fun getCustomerByPhone(phone: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerByPhone(phone)
    }

    suspend fun saveCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    // --- Bookings ---
    val allBookingsFlow: Flow<List<Booking>> = bookingDao.getAllBookings()
    val activeBookingsFlow: Flow<List<Booking>> = bookingDao.getActiveBookings()

    fun searchBookings(query: String): Flow<List<Booking>> = bookingDao.searchBookings(query)

    fun searchBookingsByNormalizedPhone(query: String): Flow<List<Booking>> =
        bookingDao.searchBookingsByNormalizedPhone(
            query.filter { it.isDigit() }
        )

    suspend fun getBooking(id: String): Booking? = withContext(Dispatchers.IO) {
        bookingDao.getBookingById(id)
    }

    suspend fun getCurrentActiveBookingForRoom(roomNumber: String): Booking? =
        withContext(Dispatchers.IO) { bookingDao.getCurrentActiveBookingForRoom(roomNumber) }

    fun getBookingsForCustomer(customerId: String): Flow<List<Booking>> =
        bookingDao.getBookingsForCustomer(customerId)

    suspend fun getBookingsForCustomerDirect(customerId: String): List<Booking> =
        withContext(Dispatchers.IO) { bookingDao.getBookingsForCustomerDirect(customerId) }


    fun getGuestsForBooking(bookingId: String): Flow<List<BookingGuest>> =
        bookingGuestDao.getForBooking(bookingId)

    suspend fun getGuestsForBookingDirect(bookingId: String): List<BookingGuest> =
        withContext(Dispatchers.IO) {
            val existing = bookingGuestDao.getForBookingDirect(bookingId)
            if (existing.isNotEmpty()) existing else bookingDao.getBookingById(bookingId)?.let { b ->
                listOf(
                    BookingGuest(
                        guestId = "LEGACY-${b.bookingId}",
                        bookingId = b.bookingId,
                        sequence = 1,
                        name = b.customerName,
                        idType = b.idType,
                        idNumber = b.idNumber,
                        phone = b.contactNumber,
                        whatsapp = b.whatsappNumber,
                        idPhotoUri = b.idPhotoUri
                    )
                )
            } ?: emptyList()
        }

    suspend fun saveBooking(
        booking: Booking,
        customerToUpsert: Customer? = null,
        initialPayment: Payment? = null,
        guests: List<BookingGuest> = emptyList(),
        additionalPayments: List<Payment> = emptyList()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                // Upsert customer if provided
                if (customerToUpsert != null) {
                    val existing = customerDao.getCustomerById(customerToUpsert.customerId)
                        ?: if (customerToUpsert.phone.isNotBlank()) customerDao.getCustomerByPhone(customerToUpsert.phone) else null
                    if (existing != null) {
                        customerDao.updateCustomer(
                            existing.copy(
                                name = customerToUpsert.name.ifBlank { existing.name },
                                idType = customerToUpsert.idType.ifBlank { existing.idType },
                                idNumber = customerToUpsert.idNumber.ifBlank { existing.idNumber },
                                whatsapp = customerToUpsert.whatsapp.ifBlank { existing.whatsapp },
                                address = customerToUpsert.address.ifBlank { existing.address },
                                idPhotoUri = customerToUpsert.idPhotoUri.ifBlank { existing.idPhotoUri },
                                lastVisit = System.currentTimeMillis(),
                                totalVisits = existing.totalVisits + 1,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        customerDao.insertCustomer(customerToUpsert)
                    }
                }

                // Insert booking
                bookingDao.insertBooking(booking)

                // Persist all guests attached to this booking.
                bookingGuestDao.deleteForBooking(booking.bookingId)
                if (guests.isNotEmpty()) bookingGuestDao.insertAll(guests)

                // Update room status to OCCUPIED if booking is active
                if (booking.status == "ACTIVE") {
                    roomDao.updateStatus(booking.roomNumber, "OCCUPIED")
                }

                // Insert initial payment if any
                if (initialPayment != null && initialPayment.amount > 0) {
                    paymentDao.insertPayment(initialPayment)
                }
                additionalPayments.filter { it.amount > 0 }.forEach { paymentDao.insertPayment(it) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBooking(booking: Booking) = withContext(Dispatchers.IO) {
        bookingDao.updateBooking(booking.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun checkoutBooking(bookingId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val booking = bookingDao.getBookingById(bookingId) ?: return@withTransaction
            bookingDao.updateBooking(
                booking.copy(
                    status = "CHECKED_OUT",
                    updatedAt = System.currentTimeMillis()
                )
            )
            // Check if any other active booking in this room
            val remainingInRoom = bookingDao.findConflictingBookings(
                roomNumber = booking.roomNumber,
                checkInTimestamp = System.currentTimeMillis() - 1000,
                checkOutTimestamp = System.currentTimeMillis() + 86400000L,
                excludeId = bookingId
            )
            if (remainingInRoom.isEmpty()) {
                roomDao.updateStatus(booking.roomNumber, "AVAILABLE")
            }
        }
    }

    suspend fun cancelBooking(bookingId: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val booking = bookingDao.getBookingById(bookingId) ?: return@withTransaction
            bookingDao.updateBooking(
                booking.copy(
                    status = "CANCELLED",
                    updatedAt = System.currentTimeMillis()
                )
            )
            roomDao.updateStatus(booking.roomNumber, "AVAILABLE")
        }
    }

    suspend fun deleteBooking(booking: Booking) = withContext(Dispatchers.IO) {
        database.withTransaction {
            bookingDao.deleteBooking(booking)
            roomDao.updateStatus(booking.roomNumber, "AVAILABLE")
        }
    }

    // --- Reservations ---
    val allReservationsFlow: Flow<List<Reservation>> = reservationDao.getAllReservations()

    suspend fun getReservation(id: String): Reservation? = withContext(Dispatchers.IO) {
        reservationDao.getReservationById(id)
    }

    suspend fun getLatestReservationForGuest(customerId: String, roomNumber: String): Reservation? =
        withContext(Dispatchers.IO) { reservationDao.getLatestReservationForGuest(customerId, roomNumber) }

    suspend fun saveReservation(reservation: Reservation) = withContext(Dispatchers.IO) {
        database.withTransaction {
            reservationDao.insertReservation(reservation.copy(updatedAt = System.currentTimeMillis()))
            if (reservation.status in listOf("PENDING", "CONFIRMED")) {
                roomDao.updateStatus(reservation.roomNumber, "RESERVED")
            }
        }
    }

    suspend fun updateReservation(reservation: Reservation) = withContext(Dispatchers.IO) {
        database.withTransaction {
            reservationDao.updateReservation(reservation.copy(updatedAt = System.currentTimeMillis()))
            if (reservation.status == "CANCELLED" || reservation.status == "COMPLETED") {
                roomDao.updateStatus(reservation.roomNumber, "AVAILABLE")
            }
        }
    }

    suspend fun deleteReservation(reservation: Reservation) = withContext(Dispatchers.IO) {
        database.withTransaction {
            reservationDao.deleteReservation(reservation)
            roomDao.updateStatus(reservation.roomNumber, "AVAILABLE")
        }
    }

    // --- Invoices ---
    val allInvoicesFlow: Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    fun searchInvoices(query: String): Flow<List<Invoice>> = invoiceDao.searchInvoices(query)

    suspend fun getInvoice(invoiceNumber: String): Invoice? = withContext(Dispatchers.IO) {
        invoiceDao.getInvoiceByNumber(invoiceNumber)
    }

    suspend fun getInvoiceForBooking(bookingId: String): Invoice? = withContext(Dispatchers.IO) {
        invoiceDao.getInvoiceByBookingId(bookingId)
    }

    fun getItemsForInvoice(invoiceNumber: String): Flow<List<InvoiceItem>> =
        invoiceDao.getItemsForInvoice(invoiceNumber)

    suspend fun getItemsForInvoiceDirect(invoiceNumber: String): List<InvoiceItem> = withContext(Dispatchers.IO) {
        invoiceDao.getItemsForInvoiceDirect(invoiceNumber)
    }

    suspend fun saveInvoice(invoice: Invoice, items: List<InvoiceItem>) = withContext(Dispatchers.IO) {
        database.withTransaction {
            invoiceDao.insertInvoice(invoice.copy(updatedAt = System.currentTimeMillis()))
            invoiceDao.deleteItemsForInvoice(invoice.invoiceNumber)
            invoiceDao.insertInvoiceItems(items.map { it.copy(invoiceNumber = invoice.invoiceNumber) })
        }
    }

    suspend fun deleteInvoice(invoice: Invoice) = withContext(Dispatchers.IO) {
        database.withTransaction {
            invoiceDao.deleteItemsForInvoice(invoice.invoiceNumber)
            invoiceDao.deleteInvoice(invoice)
        }
    }

    // --- Payments ---
    val allPaymentsFlow: Flow<List<Payment>> = paymentDao.getAllPayments()

    fun getPaymentsForBooking(bookingId: String): Flow<List<Payment>> =
        paymentDao.getPaymentsForBooking(bookingId)

    suspend fun getPaymentsForBookingDirect(bookingId: String): List<Payment> = withContext(Dispatchers.IO) {
        paymentDao.getPaymentsForBookingDirect(bookingId)
    }

    suspend fun recordPayment(payment: Payment) = withContext(Dispatchers.IO) {
        database.withTransaction {
            paymentDao.insertPayment(payment)
            if (payment.bookingId.isNotBlank()) {
                val booking = bookingDao.getBookingById(payment.bookingId)
                if (booking != null) {
                    val allPayments = paymentDao.getPaymentsForBookingDirect(payment.bookingId)
                    val totalPaidAmount = allPayments.sumOf { it.amount }
                    bookingDao.updateBooking(booking.copy(paid = totalPaidAmount, updatedAt = System.currentTimeMillis()))
                }
            }
            if (payment.invoiceId.isNotBlank()) {
                val invoice = invoiceDao.getInvoiceByNumber(payment.invoiceId)
                if (invoice != null) {
                    val allPayments = paymentDao.getPaymentsForBookingDirect(invoice.bookingId)
                    val totalPaidAmount = allPayments.sumOf { it.amount }
                    val newPending = (invoice.grandTotal - totalPaidAmount).coerceAtLeast(0.0)
                    val status = if (newPending <= 0.0) "PAID" else if (totalPaidAmount > 0) "PARTIALLY_PAID" else "PENDING"
                    invoiceDao.updateInvoice(
                        invoice.copy(
                            paid = totalPaidAmount,
                            pending = newPending,
                            paymentStatus = status,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    suspend fun deletePayment(payment: Payment) = withContext(Dispatchers.IO) {
        database.withTransaction {
            paymentDao.deletePayment(payment)
            if (payment.bookingId.isNotBlank()) {
                val booking = bookingDao.getBookingById(payment.bookingId)
                if (booking != null) {
                    val remaining = paymentDao.getPaymentsForBookingDirect(payment.bookingId)
                    val totalPaidAmount = remaining.sumOf { it.amount }
                    bookingDao.updateBooking(booking.copy(paid = totalPaidAmount, updatedAt = System.currentTimeMillis()))
                }
            }
        }
    }

    // --- Food Items & Orders ---
    val allFoodItemsFlow: Flow<List<FoodItem>> = foodDao.getAllFoodItems()
    val activeFoodItemsFlow: Flow<List<FoodItem>> = foodDao.getActiveFoodItems()

    suspend fun saveFoodItem(item: FoodItem) = withContext(Dispatchers.IO) {
        foodDao.insertFoodItem(item)
    }

    suspend fun deleteFoodItem(item: FoodItem) = withContext(Dispatchers.IO) {
        foodDao.deleteFoodItem(item)
    }

    fun getOrdersForBooking(bookingId: String): Flow<List<FoodOrder>> =
        foodDao.getOrdersForBooking(bookingId)

    suspend fun addFoodOrder(order: FoodOrder) = withContext(Dispatchers.IO) {
        database.withTransaction {
            foodDao.insertFoodOrder(order)
            val booking = bookingDao.getBookingById(order.bookingId)
            if (booking != null) {
                // Update booking food items depending on name
                when {
                    order.foodName.equals("Mineral Water", ignoreCase = true) ->
                        bookingDao.updateBooking(booking.copy(mineralWater = booking.mineralWater + order.total))
                    order.foodName.equals("Ghavane Chatney", ignoreCase = true) ->
                        bookingDao.updateBooking(booking.copy(ghavaneChatney = booking.ghavaneChatney + order.total))
                    order.foodName.equals("Tea", ignoreCase = true) ->
                        bookingDao.updateBooking(booking.copy(tea = booking.tea + order.total))
                    order.foodName.equals("Kande Pohe", ignoreCase = true) ->
                        bookingDao.updateBooking(booking.copy(kandePohe = booking.kandePohe + order.total))
                    else ->
                        bookingDao.updateBooking(booking.copy(otherCharges = booking.otherCharges + order.total))
                }
            }
        }
    }

    suspend fun deleteFoodOrder(order: FoodOrder) = withContext(Dispatchers.IO) {
        foodDao.deleteFoodOrder(order)
    }

    // --- Expenses ---
    val allExpensesFlow: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun saveExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    // --- App Sections ---
    val allSectionsFlow: Flow<List<AppSection>> = appSectionDao.getAllSections()
    val enabledSectionsFlow: Flow<List<AppSection>> = appSectionDao.getEnabledSections()

    suspend fun saveSection(section: AppSection) = withContext(Dispatchers.IO) {
        appSectionDao.insertSection(section)
    }

    suspend fun deleteSection(section: AppSection) = withContext(Dispatchers.IO) {
        if (!section.isCore) {
            appSectionDao.deleteSection(section)
        }
    }

    // --- Backups ---
    val allBackupsFlow: Flow<List<BackupMetadata>> = backupMetadataDao.getAllBackups()

    suspend fun recordBackup(metadata: BackupMetadata) = withContext(Dispatchers.IO) {
        backupMetadataDao.insertBackup(metadata)
    }

    // For Export & Backup
    suspend fun getAllDataDirect() = withContext(Dispatchers.IO) {
        FullDatabaseExport(
            settings = settingsDao.getSettingsDirect() ?: HotelSettings(),
            customers = customerDao.getAllDirect(),
            rooms = roomDao.getAllDirect(),
            reservations = reservationDao.getAllDirect(),
            bookings = bookingDao.getAllDirect(),
            bookingGuests = bookingGuestDao.getAllDirect(),
            invoices = invoiceDao.getAllDirect(),
            invoiceItems = invoiceDao.getAllInvoiceItemsDirect(),
            payments = paymentDao.getAllDirect(),
            foodItems = foodDao.getAllDirect(),
            foodOrders = foodDao.getAllOrdersDirect(),
            expenses = expenseDao.getAllDirect(),
            appSections = appSectionDao.getAllDirect(),
            backups = backupMetadataDao.getAllDirect()
        )
    }

    suspend fun restoreFullDatabase(data: FullDatabaseExport) = withContext(Dispatchers.IO) {
        database.withTransaction {
            settingsDao.saveSettings(data.settings)
            data.rooms.forEach { roomDao.insertRoom(it) }
            data.customers.forEach { customerDao.insertCustomer(it) }
            data.reservations.forEach { reservationDao.insertReservation(it) }
            data.bookings.forEach { bookingDao.insertBooking(it) }
            data.bookingGuests.forEach { bookingGuestDao.insert(it) }
            data.invoices.forEach { invoiceDao.insertInvoice(it) }
            invoiceDao.insertInvoiceItems(data.invoiceItems)
            data.payments.forEach { paymentDao.insertPayment(it) }
            data.foodItems.forEach { foodDao.insertFoodItem(it) }
            data.foodOrders.forEach { foodDao.insertFoodOrder(it) }
            data.expenses.forEach { expenseDao.insertExpense(it) }
            data.appSections.forEach { appSectionDao.insertSection(it) }
        }
    }
}

data class FullDatabaseExport(
    val settings: HotelSettings,
    val customers: List<Customer>,
    val rooms: List<RoomEntity>,
    val reservations: List<Reservation>,
    val bookings: List<Booking>,
    val bookingGuests: List<BookingGuest>,
    val invoices: List<Invoice>,
    val invoiceItems: List<InvoiceItem>,
    val payments: List<Payment>,
    val foodItems: List<FoodItem>,
    val foodOrders: List<FoodOrder>,
    val expenses: List<Expense>,
    val appSections: List<AppSection>,
    val backups: List<BackupMetadata>
)
