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
