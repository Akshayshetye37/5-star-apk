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
