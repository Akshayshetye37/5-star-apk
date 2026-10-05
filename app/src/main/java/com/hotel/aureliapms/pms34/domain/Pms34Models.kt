package com.hotel.aureliapms.pms34.domain

import java.time.Instant
import java.util.UUID

enum class BookingStatus { RESERVED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW }
enum class RoomStatus { VACANT_CLEAN, VACANT_DIRTY, OCCUPIED, INSPECT, OUT_OF_ORDER, OUT_OF_SERVICE }
enum class PaymentMethod { CASH, CARD, UPI, BANK_TRANSFER, OTHER }
enum class PaymentStatus { PENDING, PARTIAL, FULL, OVERPAYMENT }
enum class HousekeepingStatus { PENDING, IN_PROGRESS, INSPECT, COMPLETED, BLOCKED }
enum class MaintenanceStatus { OPEN, IN_PROGRESS, RESOLVED, CANCELLED }
enum class OrderStatus { NEW, ACCEPTED, PREPARING, READY, SERVED, CANCELLED }
enum class PurchaseStatus { DRAFT, ORDERED, RECEIVED, CANCELLED }
enum class StaffRole { ADMIN, MANAGER, FRONT_DESK, HOUSEKEEPING, MAINTENANCE, RESTAURANT, KITCHEN, FINANCE, AUDITOR }

data class Guest(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val nationality: String = "",
    val idType: String = "",
    val idNumber: String = "",
    val notes: String = ""
)

data class Room(
    val id: String = UUID.randomUUID().toString(),
    val number: String,
    val type: String,
    val floor: Int = 0,
    val ratePaise: Long = 0,
    val status: RoomStatus = RoomStatus.VACANT_CLEAN,
    val version: Long = 0
)

data class Booking(
    val id: String = UUID.randomUUID().toString(),
    val displayId: String,
    val primaryGuestId: String,
    val roomId: String?,
    val checkIn: Instant,
    val checkOut: Instant,
    val adults: Int = 1,
    val children: Int = 0,
    val status: BookingStatus = BookingStatus.RESERVED,
    val version: Long = 0
)

data class Folio(
    val id: String = UUID.randomUUID().toString(),
    val bookingId: String,
    val currency: String = "INR",
    val version: Long = 0
)

data class FolioCharge(
    val id: String = UUID.randomUUID().toString(),
    val folioId: String,
    val description: String,
    val amountPaise: Long,
    val source: String,
    val createdAt: Instant = Instant.now()
)

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val bookingId: String,
    val folioId: String,
    val amountPaise: Long,
    val method: PaymentMethod,
    val reference: String = "",
    val idempotencyKey: String,
    val createdAt: Instant = Instant.now(),
    val version: Long = 0
)

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val displayId: String,
    val bookingId: String,
    val folioId: String,
    val subtotalPaise: Long,
    val taxPaise: Long,
    val grandTotalPaise: Long,
    val paidPaise: Long,
    val snapshotAt: Instant = Instant.now()
)

data class HousekeepingTask(
    val id: String = UUID.randomUUID().toString(),
    val roomId: String,
    val bookingId: String? = null,
    val status: HousekeepingStatus = HousekeepingStatus.PENDING,
    val assignedStaffId: String? = null,
    val dueAt: Instant? = null
)

data class MaintenanceTicket(
    val id: String = UUID.randomUUID().toString(),
    val roomId: String?,
    val title: String,
    val description: String,
    val status: MaintenanceStatus = MaintenanceStatus.OPEN,
    val priority: Int = 3
)

data class RestaurantOrder(
    val id: String = UUID.randomUUID().toString(),
    val bookingId: String? = null,
    val roomNumber: String? = null,
    val status: OrderStatus = OrderStatus.NEW,
    val totalPaise: Long = 0
)

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val sku: String,
    val name: String,
    val unit: String,
    val quantity: Double,
    val reorderLevel: Double,
    val unitCostPaise: Long
)

data class Supplier(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val email: String = ""
)

data class PurchaseOrder(
    val id: String = UUID.randomUUID().toString(),
    val supplierId: String,
    val status: PurchaseStatus = PurchaseStatus.DRAFT,
    val totalPaise: Long = 0
)

data class LedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val bookingId: String?,
    val folioId: String?,
    val account: String,
    val debitPaise: Long = 0,
    val creditPaise: Long = 0,
    val reference: String,
    val createdAt: Instant = Instant.now()
)

data class AuditEntry(
    val id: String = UUID.randomUUID().toString(),
    val actorId: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val beforeJson: String?,
    val afterJson: String?,
    val createdAt: Instant = Instant.now()
)

data class DashboardSnapshot(
    val occupancyPercent: Double,
    val arrivals: Int,
    val departures: Int,
    val roomsDirty: Int,
    val maintenanceOpen: Int,
    val outstandingPaise: Long,
    val restaurantOrders: Int
)
