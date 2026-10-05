package com.hotel.aureliapms.pms34.ui

import com.hotel.aureliapms.pms34.domain.DashboardSnapshot

data class Pms34State(
    val loading: Boolean = true,
    val dashboard: DashboardSnapshot? = null,
    val error: String? = null
)
