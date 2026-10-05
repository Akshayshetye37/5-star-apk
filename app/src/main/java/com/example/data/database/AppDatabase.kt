package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.example.data.dao.AppSectionDao
import com.example.data.dao.BackupMetadataDao
import com.example.data.dao.BookingDao
import com.example.data.dao.BookingGuestDao
import com.example.data.dao.GuestVehicleDao
import com.example.data.dao.GuestIdentityDao
import com.example.data.dao.FinanceTransactionDao
import com.example.data.dao.FolioChargeDao
import com.example.data.dao.FolioDao
import com.example.data.dao.CustomerDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.FoodDao
import com.example.data.dao.HotelSettingsDao
import com.example.data.dao.InvoiceDao
import com.example.data.dao.PaymentDao
import com.example.data.dao.ReservationDao
import com.example.data.dao.RoomDao
import com.example.data.model.AppSection
import com.example.data.model.BackupMetadata
import com.example.data.model.Booking
import com.example.data.model.BookingGuest
import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.GuestVehicle
import com.example.data.model.GuestIdentity
import com.example.data.model.FinanceTransaction
import com.example.data.model.Folio
import com.example.data.model.FolioCharge
import com.example.data.model.FoodItem
import com.example.data.model.FoodOrder
import com.example.data.model.HotelSettings
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Payment
import com.example.data.model.Reservation
import com.example.data.model.RoomEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS folios (
                folioId TEXT NOT NULL,
                bookingId TEXT NOT NULL,
                status TEXT NOT NULL,
                currencyCode TEXT NOT NULL,
                version INTEGER NOT NULL,
                lastModifiedTimestamp INTEGER NOT NULL,
                deviceId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(folioId)
            )
        """.trimIndent())

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_folios_bookingId ON folios(bookingId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_folios_status ON folios(status)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_folios_updatedAt ON folios(updatedAt)"
        )

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS folio_charges (
                chargeId TEXT NOT NULL,
                folioId TEXT NOT NULL,
                bookingId TEXT NOT NULL,
                chargeType TEXT NOT NULL,
                sourceId TEXT NOT NULL,
                description TEXT NOT NULL,
                quantity REAL NOT NULL,
                unitPrice REAL NOT NULL,
                grossAmount REAL NOT NULL,
                discountAmount REAL NOT NULL,
                taxAmount REAL NOT NULL,
                netAmount REAL NOT NULL,
                occurredAt INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                lastModifiedTimestamp INTEGER NOT NULL,
                deviceId TEXT NOT NULL,
                PRIMARY KEY(chargeId)
            )
        """.trimIndent())

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_folio_charges_folioId ON folio_charges(folioId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_folio_charges_bookingId ON folio_charges(bookingId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_folio_charges_chargeType ON folio_charges(chargeType)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_folio_charges_createdAt ON folio_charges(createdAt)"
        )

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS finance_transactions (
                transactionId TEXT NOT NULL,
                paymentId TEXT,
                bookingId TEXT NOT NULL,
                folioId TEXT NOT NULL,
                transactionType TEXT NOT NULL,
                paymentMethod TEXT NOT NULL,
                amount REAL NOT NULL,
                debitAccount TEXT NOT NULL,
                creditAccount TEXT NOT NULL,
                referenceNumber TEXT NOT NULL,
                description TEXT NOT NULL,
                transactionTimestamp INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                deviceId TEXT NOT NULL,
                lastModifiedTimestamp INTEGER NOT NULL,
                PRIMARY KEY(transactionId)
            )
        """.trimIndent())

        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_finance_transactions_paymentId ON finance_transactions(paymentId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_finance_transactions_bookingId ON finance_transactions(bookingId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_finance_transactions_folioId ON finance_transactions(folioId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_finance_transactions_transactionType ON finance_transactions(transactionType)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_finance_transactions_transactionTimestamp ON finance_transactions(transactionTimestamp)"
        )

        /*
         * Existing data gets safe defaults.
         * Existing Booking/Invoice financial values are deliberately NOT
         * rewritten here; the reconciliation/backfill use case will create
         * authoritative FolioCharge records in a controlled transaction.
         */
        db.execSQL(
            "ALTER TABLE rooms ADD COLUMN version INTEGER NOT NULL DEFAULT 1"
        )
        db.execSQL(
            "ALTER TABLE rooms ADD COLUMN lastModifiedTimestamp INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE rooms ADD COLUMN deviceId TEXT NOT NULL DEFAULT ''"
        )

        db.execSQL(
            "UPDATE rooms SET lastModifiedTimestamp = updatedAt WHERE lastModifiedTimestamp = 0"
        )

        db.execSQL(
            "ALTER TABLE payments ADD COLUMN version INTEGER NOT NULL DEFAULT 1"
        )
        db.execSQL(
            "ALTER TABLE payments ADD COLUMN lastModifiedTimestamp INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE payments ADD COLUMN deviceId TEXT NOT NULL DEFAULT ''"
        )

        db.execSQL(
            "UPDATE payments SET lastModifiedTimestamp = COALESCE(createdAt, date) WHERE lastModifiedTimestamp = 0"
        )
    }
}


private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE hotel_settings ADD COLUMN upiPaymentAnnouncementEnabled INTEGER NOT NULL DEFAULT 0"
        )
    }
}

@Database(
    entities = [
        HotelSettings::class,
        Customer::class,
        RoomEntity::class,
        Reservation::class,
        Booking::class,
        Invoice::class,
        InvoiceItem::class,
        Payment::class,
        FoodItem::class,
        FoodOrder::class,
        Expense::class,
        AppSection::class,
        BackupMetadata::class,
        BookingGuest::class,
        GuestVehicle::class,
        GuestIdentity::class,
        Folio::class,
        FolioCharge::class,
        FinanceTransaction::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun hotelSettingsDao(): HotelSettingsDao
    abstract fun customerDao(): CustomerDao
    abstract fun roomDao(): RoomDao
    abstract fun reservationDao(): ReservationDao
    abstract fun bookingDao(): BookingDao
    abstract fun bookingGuestDao(): BookingGuestDao
    abstract fun guestVehicleDao(): GuestVehicleDao
    abstract fun guestIdentityDao(): GuestIdentityDao
    abstract fun folioDao(): FolioDao
    abstract fun folioChargeDao(): FolioChargeDao
    abstract fun financeTransactionDao(): FinanceTransactionDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun foodDao(): FoodDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun appSectionDao(): AppSectionDao
    abstract fun backupMetadataDao(): BackupMetadataDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_whatsapp ON customers(whatsapp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookings_contactNumber ON bookings(contactNumber)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bookings_whatsappNumber ON bookings(whatsappNumber)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS booking_guests (
                        guestId TEXT NOT NULL,
                        bookingId TEXT NOT NULL,
                        sequence INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        idType TEXT NOT NULL,
                        idNumber TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        whatsapp TEXT NOT NULL,
                        idPhotoUri TEXT NOT NULL,
                        vehicleNumbers TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(guestId)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_booking_guests_bookingId ON booking_guests(bookingId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_booking_guests_name ON booking_guests(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_booking_guests_phone ON booking_guests(phone)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS guest_vehicles (
                        vehicleId TEXT NOT NULL,
                        bookingId TEXT NOT NULL,
                        guestId TEXT,
                        registrationNumber TEXT NOT NULL,
                        vehicleType TEXT NOT NULL,
                        makeModel TEXT NOT NULL,
                        color TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(vehicleId)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_guest_vehicles_bookingId
                    ON guest_vehicles(bookingId)
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_guest_vehicles_guestId
                    ON guest_vehicles(guestId)
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_guest_vehicles_registrationNumber
                    ON guest_vehicles(registrationNumber)
                """.trimIndent())

                /*
                 * Preserve the legacy BookingGuest.vehicleNumbers field.
                 *
                 * We intentionally do not delete or rewrite that column.
                 * Existing data remains available for compatibility while
                 * the new relational vehicle table becomes the source of
                 * truth for newly managed vehicles.
                 *
                 * SQLite does not provide a safe built-in split operation
                 * for arbitrary comma-separated values, so legacy values
                 * are migrated conservatively as one vehicle record per
                 * non-empty legacy value when possible.
                 */
                val cursor = db.query("""
                    SELECT guestId, bookingId, vehicleNumbers
                    FROM booking_guests
                    WHERE vehicleNumbers IS NOT NULL
                      AND TRIM(vehicleNumbers) != ''
                """.trimIndent())

                cursor.use {
                    val guestIdIndex = it.getColumnIndexOrThrow("guestId")
                    val bookingIdIndex = it.getColumnIndexOrThrow("bookingId")
                    val vehiclesIndex = it.getColumnIndexOrThrow("vehicleNumbers")

                    while (it.moveToNext()) {
                        val guestId = it.getString(guestIdIndex)
                        val bookingId = it.getString(bookingIdIndex)
                        val rawVehicles = it.getString(vehiclesIndex)

                        rawVehicles
                            .split(",", ";", "\n")
                            .map { value -> value.trim() }
                            .filter { value -> value.isNotEmpty() }
                            .distinct()
                            .forEachIndexed { index, registration ->
                                val vehicleId =
                                    "LEGACY-${bookingId}-${guestId}-${index + 1}"

                                db.execSQL(
                                    """
                                    INSERT OR IGNORE INTO guest_vehicles
                                    (
                                        vehicleId,
                                        bookingId,
                                        guestId,
                                        registrationNumber,
                                        vehicleType,
                                        makeModel,
                                        color,
                                        createdAt,
                                        updatedAt
                                    )
                                    VALUES (?, ?, ?, ?, '', '', '', ?, ?)
                                    """.trimIndent(),
                                    arrayOf(
                                        vehicleId,
                                        bookingId,
                                        guestId,
                                        registration,
                                        System.currentTimeMillis(),
                                        System.currentTimeMillis()
                                    )
                                )
                            }
                    }
                }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS guest_identities (
                        identityId TEXT NOT NULL,
                        bookingId TEXT NOT NULL,
                        guestId TEXT NOT NULL,
                        idType TEXT NOT NULL,
                        idNumber TEXT NOT NULL,
                        frontPhotoUri TEXT NOT NULL,
                        backPhotoUri TEXT NOT NULL,
                        ocrName TEXT NOT NULL,
                        ocrDateOfBirth TEXT NOT NULL,
                        ocrGender TEXT NOT NULL,
                        ocrAddress TEXT NOT NULL,
                        ocrFatherName TEXT NOT NULL,
                        ocrNationality TEXT NOT NULL,
                        ocrExpiryDate TEXT NOT NULL,
                        ocrRawText TEXT NOT NULL,
                        ocrConfidence REAL NOT NULL,
                        isOcrVerified INTEGER NOT NULL,
                        verifiedBy TEXT NOT NULL,
                        verifiedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(identityId)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_guest_identities_bookingId
                    ON guest_identities(bookingId)
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_guest_identities_guestId
                    ON guest_identities(guestId)
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_guest_identities_idType
                    ON guest_identities(idType)
                """.trimIndent())

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_guest_identities_idNumber
                    ON guest_identities(idNumber)
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hotel_billing_database.db"
                )
                    .fallbackToDestructiveMigration(false)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val appDb = getDatabase(context)
                    seedInitialData(appDb)
                }
            }
        }

        suspend fun seedInitialData(db: AppDatabase) {
            // 1. Initial Hotel Settings
            if (db.hotelSettingsDao().getSettingsDirect() == null) {
                db.hotelSettingsDao().saveSettings(
                    HotelSettings(
                        id = 1,
                        hotelName = "HOTEL BILLING SYSTEM",
                        address = "Hotel Premises, Main Road",
                        phone = "",
                        whatsapp = "",
                        email = "",
                        gstNumber = "",
                        logoUri = "android.resource://com.hotelbilling.system/drawable/hotel_logo",
                        slogan = "",
                        upiId = "",
                        upiPayeeName = "",
                        currencySymbol = "₹",
                        currencyCode = "INR",
                        invoicePrefix = "INV-",
                        invoiceFooter = "Thank you for staying with us! Have a pleasant journey.",
                        termsAndConditions = "1. Standard Check-in: 12:00 PM | Check-out: 11:00 AM.\n2. Valid Government photo ID required at check-in.\n3. Non-smoking premises in common areas."
                    )
                )
            }

            // 2. THE HOTEL HAS EXACTLY FOUR ROOMS INITIALLY.
            // 111 — Micro Refine Suit
            // 156 — Compact Private Suite
            // 169 — Micro Refine Suit
            // 166 — Compact Private Suite
            // Rates set to 0.0 initially as requested.
            val initialRooms = listOf(
                RoomEntity(
                    roomNumber = "111",
                    roomType = "Micro Refine Suit",
                    floor = "1st Floor",
                    rate = 0.0,
                    status = "AVAILABLE"
                ),
                RoomEntity(
                    roomNumber = "156",
                    roomType = "Compact Private Suite",
                    floor = "1st Floor",
                    rate = 0.0,
                    status = "AVAILABLE"
                ),
                RoomEntity(
                    roomNumber = "169",
                    roomType = "Micro Refine Suit",
                    floor = "1st Floor",
                    rate = 0.0,
                    status = "AVAILABLE"
                ),
                RoomEntity(
                    roomNumber = "166",
                    roomType = "Compact Private Suite",
                    floor = "1st Floor",
                    rate = 0.0,
                    status = "AVAILABLE"
                )
            )
            db.roomDao().insertAll(initialRooms)

            // 3. Initial food items:
            // Mineral Water, Ghavane Chatney, Tea, Kande Pohe
            val initialFoodItems = listOf(
                FoodItem(name = "Mineral Water", category = "Beverage", price = 0.0, description = "Packaged drinking water"),
                FoodItem(name = "Ghavane Chatney", category = "Breakfast", price = 0.0, description = "Traditional Konkani delicacy with coconut chutney"),
                FoodItem(name = "Tea", category = "Beverage", price = 0.0, description = "Freshly brewed hot tea"),
                FoodItem(name = "Kande Pohe", category = "Breakfast", price = 0.0, description = "Classic savory flattened rice with roasted peanuts")
            )
            db.foodDao().insertAllItems(initialFoodItems)

            // 4. Default 14 Sections
            val defaultSections = listOf(
                AppSection(id = "DASHBOARD", title = "Dashboard", iconName = "Dashboard", orderIndex = 0, isEnabled = true, isCore = true),
                AppSection(id = "NEW_BOOKING", title = "New Booking", iconName = "AddCircle", orderIndex = 1, isEnabled = true, isCore = true),
                AppSection(id = "BOOKINGS", title = "Bookings", iconName = "BookOnline", orderIndex = 2, isEnabled = true, isCore = true),
                AppSection(id = "RESERVATIONS", title = "Reservations", iconName = "EventAvailable", orderIndex = 3, isEnabled = true, isCore = true),
                AppSection(id = "ROOMS", title = "Rooms & Rates", iconName = "MeetingRoom", orderIndex = 4, isEnabled = true, isCore = true),
                AppSection(id = "CUSTOMERS", title = "Customers", iconName = "People", orderIndex = 5, isEnabled = true, isCore = true),
                AppSection(id = "INVOICE_GENERATOR", title = "Invoice Generator", iconName = "Receipt", orderIndex = 6, isEnabled = true, isCore = true),
                AppSection(id = "INVOICE_HISTORY", title = "Invoice History", iconName = "History", orderIndex = 7, isEnabled = true, isCore = true),
                AppSection(id = "FOOD", title = "Breakfast / Food", iconName = "Restaurant", orderIndex = 8, isEnabled = true, isCore = true),
                AppSection(id = "PAYMENTS", title = "Payments", iconName = "Payments", orderIndex = 9, isEnabled = true, isCore = true),
                AppSection(id = "EXPENSES", title = "Expenses", iconName = "AccountBalanceWallet", orderIndex = 10, isEnabled = true, isCore = true),
                AppSection(id = "REPORTS", title = "Reports", iconName = "Assessment", orderIndex = 11, isEnabled = true, isCore = true),
                AppSection(id = "BACKUP_RESTORE", title = "Backup / Restore", iconName = "Backup", orderIndex = 12, isEnabled = true, isCore = true),
                AppSection(id = "SETTINGS", title = "Settings", iconName = "Settings", orderIndex = 13, isEnabled = true, isCore = true)
            )
            db.appSectionDao().insertAll(defaultSections)
        }
    }
}
