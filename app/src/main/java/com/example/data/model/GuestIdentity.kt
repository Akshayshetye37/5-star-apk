package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "guest_identities",
    indices = [
        Index(value = ["bookingId"]),
        Index(value = ["guestId"]),
        Index(value = ["idType"]),
        Index(value = ["idNumber"])
    ]
)
data class GuestIdentity(
    @PrimaryKey
    val identityId: String,
    val bookingId: String,
    val guestId: String,

    val idType: String,
    val idNumber: String,

    val frontPhotoUri: String = "",
    val backPhotoUri: String = "",

    val ocrName: String = "",
    val ocrDateOfBirth: String = "",
    val ocrGender: String = "",
    val ocrAddress: String = "",
    val ocrFatherName: String = "",
    val ocrNationality: String = "",
    val ocrExpiryDate: String = "",

    val ocrRawText: String = "",
    val ocrConfidence: Double = 0.0,

    val isOcrVerified: Boolean = false,
    val verifiedBy: String = "",
    val verifiedAt: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
