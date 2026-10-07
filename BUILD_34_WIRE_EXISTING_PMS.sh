#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

# AURELIA PMS — 34-point wiring pass
# Run from ~/HotelApp after BUILD_34_PMS_TERMUX.sh.
# Preserves the existing payment-announcement implementation.
# Creates a backup before modifying anything.

ROOT="${1:-$PWD}"
cd "$ROOT"
[ -d app ] || { echo "ERROR: app/ not found"; exit 1; }

STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP="$HOME/AureliaPMS-before-34-wire-$STAMP.tar.gz"
tar -czf "$BACKUP" app 2>/dev/null || true

SRC="app/src/main/java"
PKGROOT="$SRC/com/hotel/aureliapms/pms34"
mkdir -p "$PKGROOT/integration" "$PKGROOT/ui" "$PKGROOT/worker"

cat > "$PKGROOT/integration/Pms34ModuleCatalog.kt" <<'KOT'
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
KOT

cat > "$PKGROOT/integration/Pms34IntegrationContract.kt" <<'KOT'
package com.hotel.aureliapms.pms34.integration

import com.hotel.aureliapms.pms34.domain.*

/**
 * Adapter contract. Implementations must delegate to the existing application's
 * HotelDatabase/Repository instead of keeping a second in-memory source of truth.
 */
interface Pms34IntegrationContract {
    suspend fun dashboard(): DashboardSnapshot
    suspend fun ledgerBalance(folioId: String): Long
    suspend fun recordVerifiedPayment(
        bookingId: String,
        folioId: String,
        amountPaise: Long,
        method: PaymentMethod,
        reference: String,
        requestUuid: String
    ): Result<Payment>
    suspend fun checkout(bookingId: String): Result<Unit>
    suspend fun createInvoice(bookingId: String): Result<Invoice>
    suspend fun transitionHousekeeping(
        taskId: String,
        next: HousekeepingStatus
    ): Result<Unit>
}
KOT

cat > "$PKGROOT/integration/Pms34ProductionAdapter.kt" <<'KOT'
package com.hotel.aureliapms.pms34.integration

import com.hotel.aureliapms.pms34.domain.*

/**
 * Production adapter boundary.
 *
 * This class intentionally does not fabricate a second database. Wire its
 * constructor to the existing HotelRepository/use-cases in the host project.
 */
class Pms34ProductionAdapter(
    private val delegate: ExistingPms34Delegate
) : Pms34IntegrationContract {

    override suspend fun dashboard() = delegate.dashboard()

    override suspend fun ledgerBalance(folioId: String) =
        delegate.ledgerBalance(folioId)

    override suspend fun recordVerifiedPayment(
        bookingId: String,
        folioId: String,
        amountPaise: Long,
        method: PaymentMethod,
        reference: String,
        requestUuid: String
    ) = delegate.recordVerifiedPayment(
        bookingId, folioId, amountPaise, method, reference, requestUuid
    )

    override suspend fun checkout(bookingId: String) =
        delegate.checkout(bookingId)

    override suspend fun createInvoice(bookingId: String) =
        delegate.createInvoice(bookingId)

    override suspend fun transitionHousekeeping(
        taskId: String,
        next: HousekeepingStatus
    ) = delegate.transitionHousekeeping(taskId, next)
}

interface ExistingPms34Delegate {
    suspend fun dashboard(): DashboardSnapshot
    suspend fun ledgerBalance(folioId: String): Long
    suspend fun recordVerifiedPayment(
        bookingId: String,
        folioId: String,
        amountPaise: Long,
        method: PaymentMethod,
        reference: String,
        requestUuid: String
    ): Result<Payment>
    suspend fun checkout(bookingId: String): Result<Unit>
    suspend fun createInvoice(bookingId: String): Result<Invoice>
    suspend fun transitionHousekeeping(
        taskId: String,
        next: HousekeepingStatus
    ): Result<Unit>
}
KOT

cat > "$PKGROOT/ui/Pms34State.kt" <<'KOT'
package com.hotel.aureliapms.pms34.ui

import com.hotel.aureliapms.pms34.domain.DashboardSnapshot

data class Pms34State(
    val loading: Boolean = true,
    val dashboard: DashboardSnapshot? = null,
    val error: String? = null
)
KOT

cat > "$PKGROOT/ui/Pms34ViewModel.kt" <<'KOT'
package com.hotel.aureliapms.pms34.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hotel.aureliapms.pms34.integration.Pms34IntegrationContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Pms34ViewModel(
    private val integration: Pms34IntegrationContract
) : ViewModel() {

    private val _state = MutableStateFlow(Pms34State())
    val state: StateFlow<Pms34State> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { integration.dashboard() }
                .onSuccess {
                    _state.value = Pms34State(
                        loading = false,
                        dashboard = it
                    )
                }
                .onFailure {
                    _state.value = Pms34State(
                        loading = false,
                        error = it.message ?: "Unable to load PMS dashboard"
                    )
                }
        }
    }
}
KOT

cat > "$PKGROOT/worker/Pms34WorkerContracts.kt" <<'KOT'
package com.hotel.aureliapms.pms34.worker

/**
 * Scheduling contract for the automation/night-audit/housekeeping/reporting
 * areas. The host WorkManager implementation should call these jobs.
 */
enum class Pms34Job {
    HOUSEKEEPING_ALERTS,
    NIGHT_AUDIT,
    REVENUE_REFRESH,
    BACKUP,
    REPORT_REFRESH,
    AUTOMATION
}
KOT

# Produce a machine-readable checklist showing what is wired at the boundary.
mkdir -p app/src/main/assets
cat > app/src/main/assets/pms34-wiring-checklist.txt <<'TXT'
AURELIA PMS 34-POINT WIRING CHECKLIST

The following integration boundaries are present:
01 front_desk
02 reservations
03 rooms
04 room_types_rates
05 guests
06 id_ocr
07 housekeeping
08 maintenance
09 pos
10 kds
11 room_service
12 inventory
13 purchasing
14 cashier
15 folio
16 payments
17 finance
18 invoice
19 night_audit
20 revenue
21 loyalty
22 staff
23 audit
24 communications
25 automation
26 backup
27 reports
28 import_export
29 settings
30 database
31 architecture
32 checkout
33 cleaning
34 offline_performance

PAYMENT ANNOUNCEMENT:
Existing implementation is preserved and is NOT replaced by this pass.

IMPORTANT:
The integration contract is intentionally an adapter boundary. It must be bound
to the existing HotelRepository/HotelDatabase rather than creating a duplicate
in-memory database. Existing UI modules should invoke the adapter/use-cases.
TXT

# Do not automatically edit existing app files or announcement code.
# This makes the operation reversible and prevents a second competing PMS stack.

echo
echo "34-point wiring layer created."
echo "Existing payment announcement untouched."
echo "Backup: $BACKUP"
echo
echo "This pass creates the production integration contracts and ViewModel boundary."
echo "It deliberately does NOT claim 34/34 until the existing HotelRepository,"
echo "HotelDatabase and Compose screens are actually connected and the project builds."
