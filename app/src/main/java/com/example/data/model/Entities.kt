package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1. HotelSettings
 */
@Entity(tableName = "hotel_settings")
data class HotelSettings(
    @PrimaryKey val id: Int = 1,
    val hotelName: String = "HOTEL BILLING SYSTEM",
    val address: String = "Station Road, City Center",
    val phone: String = "+91 98765 43210",
    val whatsapp: String = "+91 98765 43210",
    val email: String = "info@hotelbilling.com",
    val gstNumber: String = "",
    val logoUri: String = "",
    val slogan: String = "",
    val upiId: String = "hotelbilling@upi",
    val manualQrUri: String = "",
    val localBackupEnabled: Boolean = true,
    val driveBackupEnabled: Boolean = false,
    val driveFolderUri: String = "",
    val backupSensitiveData: Boolean = true,
    val upiPayeeName: String = "Hotel Billing System",
    val upiPaymentAnnouncementEnabled: Boolean = false,
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val invoicePrefix: String = "INV-",
    val invoiceFooter: String = "Thank you for staying with us! Have a pleasant journey.",
    val termsAndConditions: String = "1. Standard Check-in: 12:00 PM | Check-out: 11:00 AM.\n2. Valid Government photo ID required at check-in.\n3. Non-smoking premises in common areas.",
    val taxPercentage: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 2. Customer
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["whatsapp"]),
        Index(value = ["name"]),
        Index(value = ["idNumber"])
    ]
)
data class Customer(
    @PrimaryKey val customerId: String,
    val name: String,
    val idType: String = "Aadhaar Card",
    val idNumber: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val address: String = "",
    val notes: String = "",
    val idPhotoUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastVisit: Long = System.currentTimeMillis(),
    val totalVisits: Int = 1
)

/**
 * 3. Room
 */
@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey val roomNumber: String,
    val roomType: String, // "Micro Refine Suit" or "Compact Private Suite"
    val floor: String = "1st Floor",
    val rate: Double = 0.0, // 0.0 initially as requested: not fabricated
    val status: String = "AVAILABLE", // AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val deviceId: String = ""
)

/**
 * 4. Reservation
 */
@Entity(
    tableName = "reservations",
    indices = [
        Index(value = ["roomNumber"]),
        Index(value = ["status"]),
        Index(value = ["checkInTimestamp", "checkOutTimestamp"])
    ]
)
data class Reservation(
    @PrimaryKey val reservationId: String,
    val customerId: String = "",
    val customerName: String,
    val phone: String,
    val roomNumber: String,
    val roomType: String,
    val checkInDate: String,
    val checkInTime: String = "12:00 PM",
    val checkOutDate: String,
    val checkOutTime: String = "11:00 AM",
    val checkInTimestamp: Long,
    val checkOutTimestamp: Long,
    val guests: Int = 1,
    val advance: Double = 0.0,
    val notes: String = "",
    val status: String = "PENDING", // PENDING, CONFIRMED, CHECKED-IN, CANCELLED, COMPLETED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 5. Booking
 */
@Entity(
    tableName = "bookings",
    indices = [
        Index(value = ["roomNumber"]),
        Index(value = ["status"]),
        Index(value = ["checkInTimestamp", "checkOutTimestamp"]),
        Index(value = ["customerName"]),
        Index(value = ["contactNumber"]),
        Index(value = ["whatsappNumber"])
    ]
)
data class Booking(
    @PrimaryKey val bookingId: String,
    val customerId: String = "",
    val customerName: String,
    val idType: String = "Aadhaar Card",
    val idNumber: String = "",
    val contactNumber: String = "",
    val whatsappNumber: String = "",
    val checkInDate: String,
    val checkInTime: String = "12:00 PM",
    val checkOutDate: String,
    val checkOutTime: String = "11:00 AM",
    val checkInTimestamp: Long,
    val checkOutTimestamp: Long,
    val numberOfGuests: Int = 1,
    val roomNumber: String,
    val roomType: String,
    val roomCharges: Double = 0.0,
    val advance: Double = 0.0,
    val paid: Double = 0.0,
    val discount: Double = 0.0,
    val mineralWater: Double = 0.0,
    val ghavaneChatney: Double = 0.0,
    val tea: Double = 0.0,
    val kandePohe: Double = 0.0,
    val otherCharges: Double = 0.0,
    val notes: String = "",
    val idPhotoUri: String = "",
    val status: String = "ACTIVE", // ACTIVE, CHECKED_OUT, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val grandTotal: Double
        get() = (roomCharges + mineralWater + ghavaneChatney + tea + kandePohe + otherCharges - discount).coerceAtLeast(0.0)

    val pending: Double
        get() = (grandTotal - paid).coerceAtLeast(0.0)
}


/**
 * Additional guests attached to one booking/room.
 * The first guest is also mirrored into the legacy Booking fields for backward compatibility.
 */
@Entity(
    tableName = "booking_guests",
    indices = [
        Index(value = ["bookingId"]),
        Index(value = ["name"]),
        Index(value = ["phone"])
    ]
)
data class BookingGuest(
    @PrimaryKey val guestId: String,
    val bookingId: String,
    val sequence: Int = 1,
    val name: String,
    val idType: String = "Aadhaar Card",
    val idNumber: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val idPhotoUri: String = "",
    /** Vehicle registration numbers separated by newlines for easy export and display. */
    val vehicleNumbers: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 6. Invoice
 */
@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["bookingId"]),
        Index(value = ["paymentStatus"]),
        Index(value = ["invoiceDate"])
    ]
)
data class Invoice(
    @PrimaryKey val invoiceNumber: String,
    val bookingId: String,
    val invoiceDate: Long = System.currentTimeMillis(),
    val customerId: String = "",
    val customerName: String,
    val customerContact: String = "",
    val roomNumber: String,
    val roomType: String,
    val checkInDate: String,
    val checkOutDate: String,
    val guests: Int = 1,
    val roomCharges: Double = 0.0,
    val foodCharges: Double = 0.0,
    val otherCharges: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val grandTotal: Double = 0.0,
    val advance: Double = 0.0,
    val paid: Double = 0.0,
    val pending: Double = 0.0,
    val paymentMethod: String = "Cash",
    val paymentStatus: String = "PENDING", // PAID, PARTIALLY_PAID, PENDING
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 7. InvoiceItem
 */
@Entity(
    tableName = "invoice_items",
    indices = [Index(value = ["invoiceNumber"])]
)
data class InvoiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val description: String,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

/**
 * 8. Payment
 */
@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["bookingId"]),
        Index(value = ["invoiceId"]),
        Index(value = ["date"])
    ]
)
data class Payment(
    @PrimaryKey val paymentId: String,
    val bookingId: String = "",
    val invoiceId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Cash", // Cash, UPI, Google Pay, Card, Bank Transfer, Other
    val referenceNumber: String = "",
    val notes: String = "",
      /**
       * Payment authorization/verification lifecycle.
       * Only VERIFIED payments are authoritative ledger entries.
       * PENDING_VERIFICATION is not included in the ledger balance.
       */
      val verificationStatus: String = "VERIFIED",
    val createdAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val deviceId: String = ""
)

/**
 * 9. FoodItem
 */
@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Breakfast",
    val price: Double = 0.0,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 10. FoodOrder
 */
@Entity(
    tableName = "food_orders",
    indices = [Index(value = ["bookingId"])]
)
data class FoodOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: String,
    val roomNumber: String,
    val foodItemId: Long = 0,
    val foodName: String,
    val quantity: Int = 1,
    val price: Double = 0.0,
    val total: Double = 0.0,
    val orderTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

/**
 * 11. Expense
 */
@Entity(
    tableName = "expenses",
    indices = [Index(value = ["date"])]
)
data class Expense(
    @PrimaryKey val expenseId: String,
    val date: Long = System.currentTimeMillis(),
    val category: String, // Maintenance, Groceries, Utilities, Salary, Other
    val description: String,
    val amount: Double,
    val paymentMethod: String = "Cash",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 12. AppSection
 */
@Entity(tableName = "app_sections")
data class AppSection(
    @PrimaryKey val id: String,
    val title: String,
    val iconName: String,
    val orderIndex: Int,
    val isEnabled: Boolean = true,
    val isCore: Boolean = true
)

/**
 * 13. BackupMetadata
 */
@Entity(tableName = "backup_metadata")
data class BackupMetadata(
    @PrimaryKey val backupId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceInfo: String = "",
    val versionCode: Int = 1,
    val recordsCount: Int = 0,
    val notes: String = ""
)
/**
 * V6 RESTAURANT / POS
 */

/**
 * V6 RESTAURANT / POS
 *
 * Made Food is customer-facing food prepared by the hotel.
 * Restaurant ingredients are internal stock.
 * Ready-made inventory is purchased finished stock.
 */
@Entity(
    tableName = "made_foods",
    indices = [Index(value = ["name"]), Index(value = ["isActive"])]
)
data class MadeFood(
    @PrimaryKey val id: String,
    val name: String,
    val sellingPrice: Double = 0.0,
    val unit: String = "plate",
    val details: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "restaurant_inventory",
    indices = [Index(value = ["name"]), Index(value = ["isActive"])]
)
data class RestaurantInventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val purchaseRate: Double = 0.0,
    val unit: String = "kg",
    val openingStock: Double = 0.0,
    val reorderThreshold: Double = 0.0,
    val isActive: Boolean = true,
    val cycleStartedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ready_made_inventory",
    indices = [Index(value = ["name"]), Index(value = ["isActive"])]
)
data class ReadyMadeInventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val purchaseRate: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val unit: String = "piece",
    val openingStock: Double = 0.0,
    val reorderThreshold: Double = 0.0,
    val isActive: Boolean = true,
    val cycleStartedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "restaurant_inventory_ledger",
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["type"]),
        Index(value = ["at"]),
        Index(value = ["restaurantSaleId"])
    ]
)
data class RestaurantInventoryLedger(
    @PrimaryKey val id: String,
    val itemId: String,
    val type: String, // purchase, sale, waste
    val quantity: Double,
    val unitRate: Double = 0.0,
    val amount: Double = 0.0,
    val bookingId: String = "",
    val restaurantSaleId: String = "",
    val at: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(
    tableName = "restaurant_sales",
    indices = [
        Index(value = ["status"]),
        Index(value = ["mode"]),
        Index(value = ["bookingId"]),
        Index(value = ["createdAt"])
    ]
)
data class RestaurantSale(
    @PrimaryKey val saleId: String,
    val mode: String, // room, walkin
    val bookingId: String = "",
    val roomNumber: String = "",
    val mobile: String = "",
    val amount: Double = 0.0,
    val paid: Double = 0.0,
    val status: String = "OPEN", // OPEN, PARTIALLY_PAID, PAID, CLOSED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "restaurant_sale_items",
    indices = [Index(value = ["saleId"])]
)
data class RestaurantSaleItem(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String,
    val productName: String,
    val quantity: Double = 1.0,
    val sellingPrice: Double = 0.0,
    val total: Double = 0.0,
    val productType: String = "MADE_FOOD" // MADE_FOOD, READY_MADE
)

