#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

# AURELIA PMS — 34-point implementation pass
# Keeps the existing payment-announcement implementation untouched.
# Run from the Android project root (the directory containing app/).
#
# This script adds the 34-point domain/data foundation and does not delete
# existing screens or announcement code. Existing files are backed up before
# replacement.

ROOT="${1:-$PWD}"
cd "$ROOT"

[ -d app ] || { echo "ERROR: app/ directory not found: $ROOT"; exit 1; }

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP="$HOME/AureliaPMS-before-34-$STAMP.tar.gz"
tar -czf "$BACKUP" app gradle gradlew gradle.properties settings.gradle.kts 2>/dev/null || true

PKG="com.hotel.aureliapms.pms34"
BASE="app/src/main/java/com/hotel/aureliapms/pms34"
mkdir -p "$BASE/domain" "$BASE/data" "$BASE/data/local" "$BASE/data/local/dao" \
         "$BASE/data/local/entity" "$BASE/data/repository" "$BASE/work" \
         "$BASE/validation" "$BASE/report"

cat > "$BASE/domain/Pms34Models.kt" <<'KOT'
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
KOT

cat > "$BASE/domain/Pms34UseCases.kt" <<'KOT'
package com.hotel.aureliapms.pms34.domain

import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class Pms34Exception(message: String): IllegalStateException(message)

object DisplayIdFactory {
    fun invoice(year: Int, sequence: Long) = "INV-%04d-%04d".format(year, sequence)
    fun booking(year: Int, sequence: Long) = "BKG-%04d-%04d".format(year, sequence)
    fun reservation(year: Int, sequence: Long) = "RES-%04d-%04d".format(year, sequence)
}

object Money {
    fun requireNonNegative(value: Long) {
        if (value < 0) throw Pms34Exception("Amount cannot be negative")
    }
    fun balance(chargesPaise: Long, paymentsPaise: Long) = chargesPaise - paymentsPaise
}

class GetLedgerBalanceUseCase(private val repository: Pms34Repository) {
    suspend operator fun invoke(folioId: String): Long {
        val charges = repository.sumCharges(folioId)
        val payments = repository.sumPayments(folioId)
        return Money.balance(charges, payments)
    }
}

class RecordPaymentUseCase(private val repository: Pms34Repository) {
    suspend operator fun invoke(
        bookingId: String,
        folioId: String,
        amountPaise: Long,
        method: PaymentMethod,
        reference: String = "",
        requestUuid: String = UUID.randomUUID().toString(),
        verified: Boolean
    ): Result<Payment> = runCatching {
        if (!verified) throw Pms34Exception("Payment must be verified before posting")
        Money.requireNonNegative(amountPaise)
        if (amountPaise == 0L) throw Pms34Exception("Payment must be greater than zero")

        val now = Instant.now()
        val key = sha256(
            "$bookingId|$folioId|$amountPaise|${now.epochSecond}|${method.name}|$requestUuid"
        )
        repository.transaction {
            if (repository.paymentExists(key)) throw Pms34Exception("Duplicate payment request")
            val balance = GetLedgerBalanceUseCase(repository)(folioId)
            if (amountPaise > balance) throw Pms34Exception("Overpayment requires explicit treatment")
            val payment = Payment(
                bookingId = bookingId,
                folioId = folioId,
                amountPaise = amountPaise,
                method = method,
                reference = reference,
                idempotencyKey = key,
                createdAt = now
            )
            repository.insertPayment(payment)
            repository.insertLedger(
                LedgerEntry(
                    bookingId = bookingId,
                    folioId = folioId,
                    account = "CASHIER",
                    debitPaise = amountPaise,
                    reference = payment.id
                )
            )
            payment
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

class CheckoutUseCase(private val repository: Pms34Repository) {
    suspend operator fun invoke(bookingId: String): Result<Unit> = runCatching {
        repository.transaction {
            val booking = repository.getBooking(bookingId)
                ?: throw Pms34Exception("Booking not found")
            if (booking.status != BookingStatus.CHECKED_IN)
                throw Pms34Exception("Booking is not checked in")
            val folio = repository.getFolioForBooking(bookingId)
                ?: throw Pms34Exception("Folio not found")
            val balance = GetLedgerBalanceUseCase(repository)(folio.id)
            if (balance > 0) throw Pms34Exception("Outstanding balance: $balance paise")
            repository.checkoutBooking(bookingId, booking.version)
            if (booking.roomId != null) repository.setRoomStatus(
                booking.roomId, RoomStatus.VACANT_DIRTY
            )
            repository.audit(
                AuditEntry(
                    actorId = "current-user",
                    action = "CHECKOUT",
                    entity = "Booking",
                    entityId = bookingId,
                    beforeJson = null,
                    afterJson = """{"status":"CHECKED_OUT"}"""
                )
            )
        }
    }
}

class HousekeepingTransitionUseCase(private val repository: Pms34Repository) {
    suspend operator fun invoke(taskId: String, next: HousekeepingStatus): Result<Unit> =
        runCatching {
            val task = repository.getHousekeepingTask(taskId)
                ?: throw Pms34Exception("Task not found")
            val allowed = when (task.status) {
                HousekeepingStatus.PENDING -> next == HousekeepingStatus.IN_PROGRESS
                HousekeepingStatus.IN_PROGRESS -> next == HousekeepingStatus.INSPECT
                HousekeepingStatus.INSPECT -> next == HousekeepingStatus.COMPLETED
                HousekeepingStatus.COMPLETED -> false
                HousekeepingStatus.BLOCKED -> next == HousekeepingStatus.IN_PROGRESS
            }
            if (!allowed) throw Pms34Exception("Invalid housekeeping transition")
            repository.updateHousekeeping(task.copy(status = next))
            if (next == HousekeepingStatus.COMPLETED)
                repository.setRoomStatus(task.roomId, RoomStatus.VACANT_CLEAN)
        }

}

class DashboardUseCase(private val repository: Pms34Repository) {
    suspend operator fun invoke(): DashboardSnapshot {
        val rooms = repository.rooms()
        val occupied = rooms.count { it.status == RoomStatus.OCCUPIED }
        return DashboardSnapshot(
            occupancyPercent = if (rooms.isEmpty()) 0.0 else occupied * 100.0 / rooms.size,
            arrivals = repository.arrivalsToday(),
            departures = repository.departuresToday(),
            roomsDirty = rooms.count { it.status == RoomStatus.VACANT_DIRTY },
            maintenanceOpen = repository.openMaintenanceCount(),
            outstandingPaise = repository.totalOutstanding(),
            restaurantOrders = repository.openRestaurantOrders()
        )
    }
}

class MorningBriefingUseCase(private val repository: Pms34Repository) {
    suspend operator fun invoke(): DashboardSnapshot = DashboardUseCase(repository)()
}

class InvoiceSnapshotUseCase(private val repository: Pms34Repository) {
    suspend operator fun invoke(bookingId: String): Invoice = repository.createInvoiceSnapshot(bookingId)
}

class Pms34Repository {
    private val guests = LinkedHashMap<String, Guest>()
    private val rooms = LinkedHashMap<String, Room>()
    private val bookings = LinkedHashMap<String, Booking>()
    private val folios = LinkedHashMap<String, Folio>()
    private val charges = LinkedHashMap<String, FolioCharge>()
    private val payments = LinkedHashMap<String, Payment>()
    private val ledger = LinkedHashMap<String, LedgerEntry>()
    private val audits = LinkedHashMap<String, AuditEntry>()
    private val housekeeping = LinkedHashMap<String, HousekeepingTask>()
    private val maintenance = LinkedHashMap<String, MaintenanceTicket>()
    private val orders = LinkedHashMap<String, RestaurantOrder>()
    private val inventory = LinkedHashMap<String, InventoryItem>()
    private val suppliers = LinkedHashMap<String, Supplier>()
    private val purchases = LinkedHashMap<String, PurchaseOrder>()

    suspend fun <T> transaction(block: suspend () -> T): T = synchronized(this) {
        kotlinx.coroutines.runBlocking { block() }
    }

    fun addGuest(g: Guest) { guests[g.id] = g }
    fun addRoom(r: Room) { rooms[r.id] = r }
    fun addBooking(b: Booking) { bookings[b.id] = b }
    fun addFolio(f: Folio) { folios[f.id] = f }
    fun addCharge(c: FolioCharge) { charges[c.id] = c }
    fun addHousekeeping(t: HousekeepingTask) { housekeeping[t.id] = t }
    fun addMaintenance(t: MaintenanceTicket) { maintenance[t.id] = t }
    fun addOrder(o: RestaurantOrder) { orders[o.id] = o }
    fun addInventory(i: InventoryItem) { inventory[i.id] = i }
    fun addSupplier(s: Supplier) { suppliers[s.id] = s }
    fun addPurchase(p: PurchaseOrder) { purchases[p.id] = p }

    fun paymentExists(key: String) = payments.values.any { it.idempotencyKey == key }
    fun insertPayment(p: Payment) { payments[p.id] = p }
    fun insertLedger(e: LedgerEntry) { ledger[e.id] = e }
    fun insertAudit(a: AuditEntry) { audits[a.id] = a }
    fun audit(a: AuditEntry) = insertAudit(a)

    fun sumCharges(folioId: String) =
        charges.values.filter { it.folioId == folioId }.sumOf { it.amountPaise }
    fun sumPayments(folioId: String) =
        payments.values.filter { it.folioId == folioId }.sumOf { it.amountPaise }

    fun getBooking(id: String) = bookings[id]
    fun getFolioForBooking(bookingId: String) = folios.values.firstOrNull { it.bookingId == bookingId }
    fun checkoutBooking(id: String, expectedVersion: Long) {
        val b = bookings[id] ?: throw Pms34Exception("Booking not found")
        if (b.version != expectedVersion) throw Pms34Exception("Booking changed; retry")
        bookings[id] = b.copy(status = BookingStatus.CHECKED_OUT, version = b.version + 1)
    }
    fun setRoomStatus(id: String, status: RoomStatus) {
        val r = rooms[id] ?: throw Pms34Exception("Room not found")
        rooms[id] = r.copy(status = status, version = r.version + 1)
    }
    fun getHousekeepingTask(id: String) = housekeeping[id]
    fun updateHousekeeping(t: HousekeepingTask) { housekeeping[t.id] = t }
    fun rooms() = rooms.values.toList()
    fun arrivalsToday() = 0
    fun departuresToday() = 0
    fun openMaintenanceCount() = maintenance.values.count { it.status == MaintenanceStatus.OPEN || it.status == MaintenanceStatus.IN_PROGRESS }
    fun openRestaurantOrders() = orders.values.count { it.status !in setOf(OrderStatus.SERVED, OrderStatus.CANCELLED) }
    fun totalOutstanding() = folios.keys.sumOf { GetLedgerBalanceUnsafe(this, it) }
    fun createInvoiceSnapshot(bookingId: String): Invoice {
        val folio = getFolioForBooking(bookingId) ?: throw Pms34Exception("Folio not found")
        val subtotal = sumCharges(folio.id)
        val paid = sumPayments(folio.id)
        val tax = 0L
        return Invoice(
            displayId = DisplayIdFactory.invoice(
                java.time.Year.now().value,
                (payments.size + charges.size + 1).toLong()
            ),
            bookingId = bookingId,
            folioId = folio.id,
            subtotalPaise = subtotal,
            taxPaise = tax,
            grandTotalPaise = subtotal + tax,
            paidPaise = paid
        )
    }
}

private fun GetLedgerBalanceUnsafe(repo: Pms34Repository, folioId: String): Long =
    repo.sumCharges(folioId) - repo.sumPayments(folioId)
KOT

cat > "$BASE/data/Pms34RoomDatabase.kt" <<'KOT'
package com.hotel.aureliapms.pms34.data

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity(tableName = "pms34_guest")
data class GuestRow(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String
)

@Entity(tableName = "pms34_room")
data class RoomRow(
    @PrimaryKey val id: String,
    val number: String,
    val type: String,
    val floor: Int,
    val ratePaise: Long,
    val status: String,
    val version: Long
)

@Entity(tableName = "pms34_booking")
data class BookingRow(
    @PrimaryKey val id: String,
    val displayId: String,
    val guestId: String,
    val roomId: String?,
    val checkInEpoch: Long,
    val checkOutEpoch: Long,
    val adults: Int,
    val children: Int,
    val status: String,
    val version: Long
)

@Entity(tableName = "pms34_folio")
data class FolioRow(
    @PrimaryKey val id: String,
    val bookingId: String,
    val version: Long
)

@Entity(tableName = "pms34_folio_charge")
data class FolioChargeRow(
    @PrimaryKey val id: String,
    val folioId: String,
    val description: String,
    val amountPaise: Long,
    val source: String,
    val createdAtEpoch: Long
)

@Entity(tableName = "pms34_payment", indices = [androidx.room.Index(value = ["idempotencyKey"], unique = true)])
data class PaymentRow(
    @PrimaryKey val id: String,
    val bookingId: String,
    val folioId: String,
    val amountPaise: Long,
    val method: String,
    val reference: String,
    val idempotencyKey: String,
    val createdAtEpoch: Long,
    val version: Long
)

@Entity(tableName = "pms34_ledger")
data class LedgerRow(
    @PrimaryKey val id: String,
    val bookingId: String?,
    val folioId: String?,
    val account: String,
    val debitPaise: Long,
    val creditPaise: Long,
    val reference: String,
    val createdAtEpoch: Long
)

@Entity(tableName = "pms34_housekeeping")
data class HousekeepingRow(
    @PrimaryKey val id: String,
    val roomId: String,
    val bookingId: String?,
    val status: String,
    val assignedStaffId: String?,
    val dueAtEpoch: Long?
)

@Entity(tableName = "pms34_maintenance")
data class MaintenanceRow(
    @PrimaryKey val id: String,
    val roomId: String?,
    val title: String,
    val description: String,
    val status: String,
    val priority: Int
)

@Entity(tableName = "pms34_audit")
data class AuditRow(
    @PrimaryKey val id: String,
    val actorId: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val beforeJson: String?,
    val afterJson: String?,
    val createdAtEpoch: Long
)

@Database(
    entities = [
        GuestRow::class, RoomRow::class, BookingRow::class, FolioRow::class,
        FolioChargeRow::class, PaymentRow::class, LedgerRow::class,
        HousekeepingRow::class, MaintenanceRow::class, AuditRow::class
    ],
    version = 1,
    exportSchema = true
)
abstract class Pms34RoomDatabase : RoomDatabase()
KOT

cat > "$BASE/data/Pms34RepositoryAdapter.kt" <<'KOT'
package com.hotel.aureliapms.pms34.data

import com.hotel.aureliapms.pms34.domain.*

/**
 * Adapter boundary for wiring the 34-point domain into the existing HotelDatabase.
 *
 * Keep the current production repository/announcement wiring intact. This class
 * deliberately exposes the domain contracts without replacing the existing PMS.
 */
interface Pms34ProductionRepository {
    suspend fun dashboard(): DashboardSnapshot
    suspend fun ledgerBalance(folioId: String): Long
    suspend fun recordPayment(
        bookingId: String,
        folioId: String,
        amountPaise: Long,
        method: PaymentMethod,
        reference: String,
        requestUuid: String,
        verified: Boolean
    ): Result<Payment>
    suspend fun checkout(bookingId: String): Result<Unit>
    suspend fun createInvoice(bookingId: String): Result<Invoice>
}
KOT

cat > "$BASE/validation/Pms34Validation.kt" <<'KOT'
package com.hotel.aureliapms.pms34.validation

object Pms34Validation {
    fun required(value: String, field: String): String {
        require(value.isNotBlank()) { "$field is required" }
        return value.trim()
    }

    fun phone(value: String): String {
        require(value.length in 7..20) { "Invalid phone number" }
        return value
    }

    fun adults(value: Int): Int {
        require(value > 0) { "At least one adult is required" }
        return value
    }

    fun dates(checkInEpoch: Long, checkOutEpoch: Long) {
        require(checkOutEpoch > checkInEpoch) { "Check-out must be after check-in" }
    }
}
KOT

cat > "$BASE/report/Pms34FeatureRegistry.kt" <<'KOT'
package com.hotel.aureliapms.pms34.report

/**
 * Single source of truth for the 34 requested PMS modules.
 * The payment announcement is intentionally NOT included here because it
 * already exists in the user's Termux implementation.
 */
object Pms34FeatureRegistry {
    val points = listOf(
        "Front Desk / PMS",
        "Reservations",
        "Rooms",
        "Room Types & Rates",
        "Guest Management",
        "ID / OCR",
        "Housekeeping",
        "Maintenance",
        "Restaurant / POS",
        "Kitchen / KDS",
        "Room Service",
        "Inventory",
        "Purchasing / Suppliers",
        "Cashier / Shifts",
        "Folio / Billing",
        "Payments",
        "Finance / Ledger",
        "Invoice",
        "Night Audit",
        "Revenue Management",
        "Loyalty / VIP",
        "Staff / Roles",
        "Audit Log",
        "Communications",
        "Automation Engine",
        "Backup / Restore",
        "Reports",
        "Import / Export",
        "Settings / Modular Toggles",
        "Relational Database",
        "Clean Architecture",
        "Transactional Checkout",
        "Transactional Cleaning",
        "Performance / Offline Reliability"
    )
}
KOT

# Add a build-time generated manifest without touching the existing app manifest.
mkdir -p app/src/main/assets
cat > app/src/main/assets/pms34-feature-registry.txt <<'TXT'
AURELIA PMS — 34 POINT IMPLEMENTATION TARGET
Payment announcement is pre-existing and intentionally preserved.

01 Front Desk / PMS
02 Reservations
03 Rooms
04 Room Types & Rates
05 Guest Management
06 ID / OCR
07 Housekeeping
08 Maintenance
09 Restaurant / POS
10 Kitchen / KDS
11 Room Service
12 Inventory
13 Purchasing / Suppliers
14 Cashier / Shifts
15 Folio / Billing
16 Payments
17 Finance / Ledger
18 Invoice
19 Night Audit
20 Revenue Management
21 Loyalty / VIP
22 Staff / Roles
23 Audit Log
24 Communications
25 Automation Engine
26 Backup / Restore
27 Reports
28 Import / Export
29 Settings / Modular Toggles
30 Relational Database
31 Clean Architecture
32 Transactional Checkout
33 Transactional Cleaning
34 Performance / Offline Reliability
TXT

echo
echo "34-point PMS implementation layer written."
echo "Payment announcement was NOT changed."
echo "Backup: $BACKUP"
echo
echo "Important: this is an additive domain/data layer. Existing app screens must be wired"
echo "to these contracts and the project's existing HotelDatabase before claiming 34/34 complete."
echo "No fake local.properties or Android SDK paths were created."
