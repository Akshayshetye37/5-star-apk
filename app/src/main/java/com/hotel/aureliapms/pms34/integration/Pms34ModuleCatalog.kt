package com.hotel.aureliapms.pms34.integration

/**
 * The existing application remains the UI owner.
 * This catalog gives every requested PMS area a stable integration key.
 *
 * Payment announcement is deliberately external: the existing implementation
 * in the host application remains untouched.
 */
enum class Pms34Module(val key: String, val title: String) {
    FRONT_DESK("front_desk", "Front Desk / PMS"),
    RESERVATIONS("reservations", "Reservations"),
    ROOMS("rooms", "Rooms"),
    ROOM_TYPES_RATES("room_types_rates", "Room Types & Rates"),
    GUESTS("guests", "Guest Management"),
    ID_OCR("id_ocr", "ID / OCR"),
    HOUSEKEEPING("housekeeping", "Housekeeping"),
    MAINTENANCE("maintenance", "Maintenance"),
    POS("pos", "Restaurant / POS"),
    KDS("kds", "Kitchen / KDS"),
    ROOM_SERVICE("room_service", "Room Service"),
    INVENTORY("inventory", "Inventory"),
    PURCHASING("purchasing", "Purchasing / Suppliers"),
    CASHIER("cashier", "Cashier / Shifts"),
    FOLIO("folio", "Folio / Billing"),
    PAYMENTS("payments", "Payments"),
    FINANCE("finance", "Finance / Ledger"),
    INVOICE("invoice", "Invoice"),
    NIGHT_AUDIT("night_audit", "Night Audit"),
    REVENUE("revenue", "Revenue Management"),
    LOYALTY("loyalty", "Loyalty / VIP"),
    STAFF("staff", "Staff / Roles"),
    AUDIT("audit", "Audit Log"),
    COMMUNICATIONS("communications", "Communications"),
    AUTOMATION("automation", "Automation Engine"),
    BACKUP("backup", "Backup / Restore"),
    REPORTS("reports", "Reports"),
    IMPORT_EXPORT("import_export", "Import / Export"),
    SETTINGS("settings", "Settings / Modular Toggles"),
    DATABASE("database", "Relational Database"),
    ARCHITECTURE("architecture", "Clean Architecture"),
    CHECKOUT("checkout", "Transactional Checkout"),
    CLEANING("cleaning", "Transactional Cleaning"),
    OFFLINE_PERFORMANCE("offline_performance", "Performance / Offline Reliability")
}
