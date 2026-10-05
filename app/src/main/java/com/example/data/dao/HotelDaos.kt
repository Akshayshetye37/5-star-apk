package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface HotelSettingsDao {
    @Query("SELECT * FROM hotel_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<HotelSettings?>

    @Query("SELECT * FROM hotel_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): HotelSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: HotelSettings)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE customerId = :id LIMIT 1")
    suspend fun getCustomerById(id: String): Customer?

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(phone: String): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR idNumber LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun getCount(): Int

    @Query("SELECT * FROM customers")
    suspend fun getAllDirect(): List<Customer>
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY roomNumber ASC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE roomNumber = :roomNumber LIMIT 1")
    suspend fun getRoomByNumber(roomNumber: String): RoomEntity?

    @Query("SELECT * FROM rooms WHERE status = :status ORDER BY roomNumber ASC")
    fun getRoomsByStatus(status: String): Flow<List<RoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rooms: List<RoomEntity>)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("UPDATE rooms SET status = :status, updatedAt = :updatedAt WHERE roomNumber = :roomNumber")
    suspend fun updateStatus(roomNumber: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM rooms")
    suspend fun getAllDirect(): List<RoomEntity>
}

@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations ORDER BY checkInTimestamp DESC")
    fun getAllReservations(): Flow<List<Reservation>>

    @Query("SELECT * FROM reservations WHERE reservationId = :id LIMIT 1")
    suspend fun getReservationById(id: String): Reservation?

    @Query("SELECT * FROM reservations WHERE customerId = :customerId AND roomNumber = :roomNumber ORDER BY checkInTimestamp DESC LIMIT 1")
    suspend fun getLatestReservationForGuest(customerId: String, roomNumber: String): Reservation?

    @Query("SELECT * FROM reservations WHERE status = :status ORDER BY checkInTimestamp ASC")
    fun getReservationsByStatus(status: String): Flow<List<Reservation>>

    @Query("""
        SELECT * FROM reservations 
        WHERE roomNumber = :roomNumber 
          AND status IN ('CONFIRMED', 'PENDING')
          AND checkInTimestamp < :checkOutTimestamp 
          AND checkOutTimestamp > :checkInTimestamp
          AND (:excludeId IS NULL OR reservationId != :excludeId)
    """)
    suspend fun findConflictingReservations(
        roomNumber: String,
        checkInTimestamp: Long,
        checkOutTimestamp: Long,
        excludeId: String? = null
    ): List<Reservation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: Reservation)

    @Update
    suspend fun updateReservation(reservation: Reservation)

    @Delete
    suspend fun deleteReservation(reservation: Reservation)

    @Query("SELECT * FROM reservations")
    suspend fun getAllDirect(): List<Reservation>
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY checkInTimestamp DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE bookingId = :id LIMIT 1")
    suspend fun getBookingById(id: String): Booking?

    @Query("SELECT * FROM bookings WHERE status = 'ACTIVE' ORDER BY checkInTimestamp DESC")
    fun getActiveBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE customerId = :customerId ORDER BY checkInTimestamp DESC")
    fun getBookingsForCustomer(customerId: String): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE customerId = :customerId ORDER BY checkInTimestamp DESC")
    suspend fun getBookingsForCustomerDirect(customerId: String): List<Booking>

    @Query("SELECT * FROM bookings WHERE roomNumber = :roomNumber AND status = 'ACTIVE' ORDER BY checkInTimestamp DESC LIMIT 1")
    suspend fun getCurrentActiveBookingForRoom(roomNumber: String): Booking?

    @Query("""
        SELECT * FROM bookings 
        WHERE roomNumber = :roomNumber 
          AND status = 'ACTIVE'
          AND checkInTimestamp < :checkOutTimestamp 
          AND checkOutTimestamp > :checkInTimestamp
          AND (:excludeId IS NULL OR bookingId != :excludeId)
    """)
    suspend fun findConflictingBookings(
        roomNumber: String,
        checkInTimestamp: Long,
        checkOutTimestamp: Long,
        excludeId: String? = null
    ): List<Booking>

    @Query("""
        SELECT b.* FROM bookings b
        LEFT JOIN customers c ON c.customerId = b.customerId
        WHERE b.bookingId LIKE '%' || :query || '%'
           OR b.customerName LIKE '%' || :query || '%'
           OR b.contactNumber LIKE '%' || :query || '%'
           OR b.whatsappNumber LIKE '%' || :query || '%'
           OR REPLACE(REPLACE(REPLACE(REPLACE(b.contactNumber, ' ', ''), '-', ''), '+', ''), '(', '') LIKE '%' || REPLACE(REPLACE(REPLACE(REPLACE(:query, ' ', ''), '-', ''), '+', ''), '(', '') || '%'
           OR REPLACE(REPLACE(REPLACE(REPLACE(b.whatsappNumber, ' ', ''), '-', ''), '+', ''), '(', '') LIKE '%' || REPLACE(REPLACE(REPLACE(REPLACE(:query, ' ', ''), '-', ''), '+', ''), '(', '') || '%'
           OR b.roomNumber LIKE '%' || :query || '%'
           OR c.name LIKE '%' || :query || '%'
           OR c.phone LIKE '%' || :query || '%'
           OR c.whatsapp LIKE '%' || :query || '%'
        ORDER BY b.checkInTimestamp DESC
        LIMIT 100
    """)
    fun searchBookings(query: String): Flow<List<Booking>>

    @Query("""
        SELECT b.* FROM bookings b
        LEFT JOIN customers c ON c.customerId = b.customerId
        WHERE REPLACE(REPLACE(REPLACE(REPLACE(b.contactNumber, ' ', ''), '-', ''), '+', ''), '(', '') LIKE '%' || :normalized || '%'
           OR REPLACE(REPLACE(REPLACE(REPLACE(b.whatsappNumber, ' ', ''), '-', ''), '+', ''), '(', '') LIKE '%' || :normalized || '%'
           OR REPLACE(REPLACE(REPLACE(REPLACE(c.phone, ' ', ''), '-', ''), '+', ''), '(', '') LIKE '%' || :normalized || '%'
           OR REPLACE(REPLACE(REPLACE(REPLACE(c.whatsapp, ' ', ''), '-', ''), '+', ''), '(', '') LIKE '%' || :normalized || '%'
        ORDER BY b.checkInTimestamp DESC
        LIMIT 100
    """)
    fun searchBookingsByNormalizedPhone(normalized: String): Flow<List<Booking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking)

    @Update
    suspend fun updateBooking(booking: Booking)

    @Delete
    suspend fun deleteBooking(booking: Booking)

    @Query("SELECT * FROM bookings")
    suspend fun getAllDirect(): List<Booking>
}


@Dao
interface BookingGuestDao {
    @Query("SELECT * FROM booking_guests WHERE bookingId = :bookingId ORDER BY sequence ASC")
    suspend fun getForBookingDirect(bookingId: String): List<BookingGuest>

    @Query("SELECT * FROM booking_guests WHERE bookingId = :bookingId ORDER BY sequence ASC")
    fun getForBooking(bookingId: String): Flow<List<BookingGuest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(guests: List<BookingGuest>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(guest: BookingGuest)

    @Query("DELETE FROM booking_guests WHERE bookingId = :bookingId")
    suspend fun deleteForBooking(bookingId: String)

    @Query("SELECT * FROM booking_guests")
    suspend fun getAllDirect(): List<BookingGuest>
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY invoiceDate DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(invoiceNumber: String): Invoice?

    @Query("SELECT * FROM invoices WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getInvoiceByBookingId(bookingId: String): Invoice?

    @Query("""
        SELECT * FROM invoices 
        WHERE invoiceNumber LIKE '%' || :query || '%' 
           OR bookingId LIKE '%' || :query || '%' 
           OR customerName LIKE '%' || :query || '%' 
           OR roomNumber LIKE '%' || :query || '%'
        ORDER BY invoiceDate DESC
    """)
    fun searchInvoices(query: String): Flow<List<Invoice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice)

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoices")
    suspend fun getAllDirect(): List<Invoice>

    // Items
    @Query("SELECT * FROM invoice_items WHERE invoiceNumber = :invoiceNumber")
    fun getItemsForInvoice(invoiceNumber: String): Flow<List<InvoiceItem>>

    @Query("SELECT * FROM invoice_items WHERE invoiceNumber = :invoiceNumber")
    suspend fun getItemsForInvoiceDirect(invoiceNumber: String): List<InvoiceItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Query("DELETE FROM invoice_items WHERE invoiceNumber = :invoiceNumber")
    suspend fun deleteItemsForInvoice(invoiceNumber: String)

    @Query("SELECT * FROM invoice_items")
    suspend fun getAllInvoiceItemsDirect(): List<InvoiceItem>
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE bookingId = :bookingId ORDER BY date DESC")
    fun getPaymentsForBooking(bookingId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE bookingId = :bookingId")
    suspend fun getPaymentsForBookingDirect(bookingId: String): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("SELECT * FROM payments")
    suspend fun getAllDirect(): List<Payment>
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY category ASC, name ASC")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveFoodItems(): Flow<List<FoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllItems(items: List<FoodItem>)

    @Update
    suspend fun updateFoodItem(item: FoodItem)

    @Delete
    suspend fun deleteFoodItem(item: FoodItem)

    @Query("SELECT * FROM food_items")
    suspend fun getAllDirect(): List<FoodItem>

    // Orders
    @Query("SELECT * FROM food_orders WHERE bookingId = :bookingId ORDER BY orderTimestamp DESC")
    fun getOrdersForBooking(bookingId: String): Flow<List<FoodOrder>>

    @Query("SELECT * FROM food_orders WHERE bookingId = :bookingId")
    suspend fun getOrdersForBookingDirect(bookingId: String): List<FoodOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodOrder(order: FoodOrder)

    @Delete
    suspend fun deleteFoodOrder(order: FoodOrder)

    @Query("SELECT * FROM food_orders")
    suspend fun getAllOrdersDirect(): List<FoodOrder>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses")
    suspend fun getAllDirect(): List<Expense>
}

@Dao
interface AppSectionDao {
    @Query("SELECT * FROM app_sections ORDER BY orderIndex ASC")
    fun getAllSections(): Flow<List<AppSection>>

    @Query("SELECT * FROM app_sections WHERE isEnabled = 1 ORDER BY orderIndex ASC")
    fun getEnabledSections(): Flow<List<AppSection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: AppSection)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sections: List<AppSection>)

    @Update
    suspend fun updateSection(section: AppSection)

    @Delete
    suspend fun deleteSection(section: AppSection)

    @Query("SELECT * FROM app_sections")
    suspend fun getAllDirect(): List<AppSection>
}

@Dao
interface BackupMetadataDao {
    @Query("SELECT * FROM backup_metadata ORDER BY timestamp DESC")
    fun getAllBackups(): Flow<List<BackupMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(metadata: BackupMetadata)

    @Query("SELECT * FROM backup_metadata")
    suspend fun getAllDirect(): List<BackupMetadata>
}
