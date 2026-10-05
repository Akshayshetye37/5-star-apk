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
