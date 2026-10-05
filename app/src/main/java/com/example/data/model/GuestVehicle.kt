package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "guest_vehicles",
    indices = [
        Index(value = ["bookingId"]),
        Index(value = ["guestId"]),
        Index(value = ["registrationNumber"])
    ]
)
data class GuestVehicle(
    @PrimaryKey
    val vehicleId: String,
    val bookingId: String,
    val guestId: String?,
    val registrationNumber: String,
    val vehicleType: String = "",
    val makeModel: String = "",
    val color: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
