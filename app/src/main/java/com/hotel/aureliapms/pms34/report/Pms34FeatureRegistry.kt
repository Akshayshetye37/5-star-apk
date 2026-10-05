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
