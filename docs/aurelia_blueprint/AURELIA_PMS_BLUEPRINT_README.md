# AURELIA 5★ HOTEL PMS — IMPLEMENTATION BLUEPRINT

## 1. Purpose

This ZIP contains the current Aurelia 5★ Hotel PMS HTML prototype plus this blueprint.

The HTML is a functional prototype/reference implementation. It demonstrates the intended PMS workflow, data relationships, calculations, export workflow, settings philosophy, and offline-first behavior.

It is **not** the final native Android implementation. The production APK should reproduce these workflows in Flutter/Dart with a local database and native Android integrations.

---

## 2. Included file

- `AURELIA_5STAR_PMS_AUTOMATION_SMART_FIXED.html`
  - Single-file interactive PMS prototype.
  - Intended as the master workflow/reference for the Flutter implementation.

Prototype SHA-256:
`469118e28f225c2445b067943d2e0a6f8dc33baff7f24e34b88b22c3b42242b1`

Prototype size:
`110,552 bytes`

---

## 3. High-level architecture

The intended production architecture is:

```text
Flutter UI / Aurelia Design
        |
        v
Feature Modules
        |
        v
Application Services
        |
        +--------------------+
        |                    |
        v                    v
Local Database          File / Media Store
        |                    |
        +---------+----------+
                  |
                  v
        Sync / Backup Layer
          /              \
         v                v
   Local backup       Google Drive
                         (optional)

External/native adapters:
- Camera
- Gallery / file picker
- Android OCR
- QR generation/scanning
- WhatsApp/share intents
- Phone call intent
- PDF/image/XLSX generation
- Verified payment callback integration
```

### Core principle

**Local database is the source of truth for normal hotel operation.**

Internet should be optional for:
- Google Drive sync/backup
- WhatsApp or remote sharing
- payment-provider callbacks
- remote services
- future cloud functionality

The PMS must remain useful when the device is offline.

---

# 4. Major functional modules

## Dashboard

Responsibilities:
- Room/stay overview
- Active check-ins
- Upcoming check-outs
- Occupied rooms
- Available rooms
- Reserved rooms
- Cleaning-required rooms
- Current guest count
- Universal search
- Booking/reservation counts
- Alerts

Automation:
- Counts are calculated from stored records.
- Room status is derived from reservations/stays/housekeeping state.
- Alerts should update without manual recalculation.

---

## Reservations

Data:
- Reservation ID
- SR number
- Guest
- Room
- Room type
- Arrival date
- Departure date
- Guest count
- Rate
- Status

Rules:
- Reservation remains a reservation until actual check-in.
- Date arrival alone must not automatically make a room occupied.
- Room availability must be checked against overlapping stays/reservations.
- Conversion to active stay happens through check-in.

---

## Bookings / Stays

A booking represents the actual hotel stay.

Important fields:
- Booking ID
- SR number
- Primary guest
- Occupants
- Room
- Check-in
- Check-out
- Guest count
- Room rate
- Nights
- Discount
- Charges
- Payments
- Balance
- Status

### Room-rate automation

When a room is selected:

```text
selected room
     |
     +--> room type
     +--> configured rate
     +--> availability
     |
     v
booking rate auto-filled
```

The user should not have to manually type the normal room rate.

Total room charge:

```text
nights = checkoutDate - checkinDate

roomAmount = nights × roomRate

netRoomAmount = roomAmount - discount
```

Additional folio charges are then added separately.

---

# 5. Guest model

A primary guest may have:

- Name
- Phone
- Email
- Address
- Photo
- Government ID type
- Government ID number
- Original ID image
- Occupants
- Vehicles
- Booking history

## Occupants

Guest count is automatic:

```text
guestCount = 1 primary guest + number of occupants
```

Adding/removing an occupant immediately recalculates the booking guest count.

Each occupant should support its own identity details where required.

---

# 6. ID/OCR workflow

Production Flutter implementation:

```text
Camera capture ──────┐
                     ├──> common OCR pipeline
Gallery/File upload ─┘
                            |
                            v
                     OCR recognition
                            |
                            +--> ID type
                            +--> full name
                            +--> ID number
                            +--> DOB/address where available
                            |
                            v
                     editable form
                            |
                            v
                       user verifies
                            |
                            v
                         save guest
```

Important:
- Camera and uploaded image must use the same OCR service.
- Never silently trust OCR.
- User verification is required before saving.
- Original ID image must be retained with the guest record.
- ID images must be included in backup/export according to privacy/security settings.

---

# 7. Vehicles

One guest/group may have multiple vehicles.

Vehicle fields:
- Registration number
- Vehicle type
- Photo
- OCR result where supported
- Notes

Examples:
- Bike
- Car
- Bus
- Van
- Other

---

# 8. Folio

The folio is the financial ledger for a stay.

It should contain actual line items, not generic labels.

Example:

```text
Room — 2 nights
Breakfast — 2
Water Bottle — 3
Soap — 2
Laundry — 1
POS item — 1
Discount — ...
Payment — ...
```

The folio must preserve:
- Quantity
- Unit price
- Tax if enabled
- Discount
- Total
- Timestamp
- Source/module
- Staff/device
- Payment linkage

---

# 9. Payment calculation

The financial engine should derive values rather than rely on manually typed totals.

Conceptually:

```text
grossCharges
  = roomCharges
  + folioItems
  + taxes
  + otherCharges

discounts
  = bookingDiscount
  + itemDiscounts
  + approvedAdjustments

netCharges
  = grossCharges - discounts

paid
  = verifiedPayments

balance
  = netCharges - paid
```

The balance must be calculated from the ledger.

Do not mark a payment as verified merely because a QR code or payment intent was generated.

Production payment flow:

```text
payment initiated
      |
      v
provider/payment gateway
      |
      v
verified callback / verified transaction
      |
      v
database transaction
      |
      v
payment ledger entry
      |
      v
folio balance recalculated
      |
      v
payment announcement (if enabled)
```

---

# 10. POS + Inventory

Inventory must be a ledger, not a single editable stock number.

## Stock equation

```text
openingStock
+ purchases
+ approvedAdjustmentsIn
- sold
- waste
- approvedAdjustmentsOut
= remainingStock
```

Purchasing a new batch must **add** to existing stock.

It must never overwrite historical purchases.

## Sale

A POS sale should atomically create:

```text
InventorySale
+
FolioCharge
```

This keeps inventory and guest billing synchronized.

---

# 11. Inventory reset

Reset means:

**Start a new operating cycle without destroying history.**

Correct behavior:

```text
old historical ledger
        |
        +--> remains permanently available
        |
        v
new opening stock
        |
        v
new cycle
```

A reset must never make old sales disappear.

Historical balance sheets remain available.

---

# 12. Inventory balance sheets

Required periods:

- Daily
- Weekly
- Monthly
- All-time
- Custom date range

Each report should calculate:

```text
Opening stock/value
Purchases
Purchase expense
Sold quantity
Sales income
Waste
Adjustments
Closing stock/value
Gross inventory income
Inventory expense
Net inventory result
```

Every inventory sale/purchase should have:
- Date/time
- Item
- Quantity
- Unit cost
- Unit sale price
- Total
- Guest/booking when applicable
- Staff/device
- Reference ID

Reports should be exportable.

---

# 13. Expenses vs inventory income

Inventory reporting must distinguish:

```text
Inventory purchase expense
Inventory sales income
Other hotel expenses
```

Do not mix them into a single unexplained number.

The dashboard/reporting layer should make it possible to see:

```text
Inventory income
- Inventory purchase expense
= Inventory operating result
```

and separately:

```text
Hotel expenses
Room income
POS/Folio income
Inventory income
Net operating figures
```

---

# 14. Housekeeping automation

After checkout:

```text
checkout completed
      |
      v
room -> CLEANING_REQUIRED
      |
      v
housekeeping checklist
      |
      +--> completed
      |
      v
room -> AVAILABLE
```

Checklist must be editable.

Examples:
- Room dusting
- Bathroom
- Toilet
- Basin
- Mirror
- Window
- Curtain
- Bed freshener

---

# 15. Due alerts

Before checkout:

```text
checkout time - current time <= configured alert window
AND
balance > 0
```

Then:
- hotel/device alert
- room alert/blink
- staff notification

Do not send a guest-facing notification unless explicitly configured.

---

# 16. Invoice system

Invoices should support:

- Full invoice
- Pending invoice
- Receipt
- Payment receipt
- Guest-specific invoice
- Booking-specific invoice
- Folio invoice

Branding:
- Hotel name
- Address
- Phone
- Logo
- Tax details
- UPI ID
- Payment QR
- Checkout message

Pending invoice:
- Exact guest
- Exact pending amount
- Large payment QR
- PDF
- JPG
- PNG
- Share to WhatsApp

The QR must be generated locally in the production Android app where possible.

---

# 17. Export architecture

Exports should use a common report model:

```text
PMS record
    |
    v
ReportBuilder
    |
    +--> PDF renderer
    +--> XLSX renderer
    +--> CSV renderer
    +--> JSON serializer
    +--> JPG renderer
    +--> PNG renderer
    +--> printable/handwriting renderer
```

Do **not** create each export format directly from arbitrary database objects.

This prevents the raw-JSON problem shown in the screenshots.

### Example

Bad:

```text
export(folio)
 -> JSON.stringify(folio)
 -> put JSON inside PDF
```

Correct:

```text
folio
 -> buildFolioReport()
 -> report sections/table/summary
 -> PDF/JPG/PNG/XLSX/CSV
```

JSON remains a true machine-readable JSON export.

---

# 18. Single-record export

Every major entity should have an export action.

Examples:

- Single guest
- Single booking
- Single room
- Single folio
- Single reservation
- Single invoice
- Inventory transaction
- Inventory balance sheet

The export must contain the relevant information for that entity rather than the entire database.

---

# 19. Import

Supported conceptual formats:

- JSON
- CSV
- XLSX
- PDF
- JPG
- PNG
- Images
- Other supported document formats

Important distinction:

### Structured imports

JSON / CSV / XLSX can be mapped into database records.

### Visual/document imports

PDF/JPG/PNG require extraction/OCR/document parsing before records can be created.

Production Flutter should therefore use native document/file adapters.

---

# 20. Universal search

Search should index:

- Guest name
- Phone
- ID number
- Booking SR
- Booking ID
- Room
- Vehicle registration
- Invoice
- Folio
- Reservation
- Inventory item
- Payment reference

Search result should open the relevant entity directly.

---

# 21. Settings / Master control

Every major feature should be configurable.

Examples:

```text
Dashboard
Universal Search
Reservations
Bookings
Guests
Guest OCR
Guest Photos
Vehicles
Rooms
Folio
Invoices
Terms
Housekeeping
POS
Inventory
KDS
Expenses
Reports
Balance Sheets
Audit
Notifications
Payment Announcement
Local Backup
Google Drive Sync
Import / Export
Room Export
Guest Export
WhatsApp
Call
Payment QR
Payment Verification
Invoice JPG/PNG
Invoice PDF
Offline Mode
```

Settings should control feature availability without deleting data.

---

# 22. Offline-first storage

Recommended Flutter production stack:

```text
Flutter
Dart
    |
Local database
    |
Repository layer
    |
Application services
    |
UI
```

The exact database package can be selected during implementation, but the important rule is:

**Never make the PMS depend on a network request for normal room, guest, booking, folio or inventory operations.**

---

# 23. Google Drive

Drive should be treated as:

- backup destination
- synchronization transport
- recovery source

It should **not** be treated as a transactional database.

Use metadata such as:

```text
recordId
updatedAt
createdAt
deviceId
revision
deletedAt
checksum
```

for conflict detection.

Recommended sync strategy:

```text
local transaction
      |
      v
change journal
      |
      v
backup/sync queue
      |
      v
Google Drive
```

If internet is unavailable:

```text
local transaction succeeds
sync queue waits
network returns
sync resumes
```

---

# 24. Autosave

Every mutation should follow:

```text
validate
  ↓
database transaction
  ↓
audit record
  ↓
change journal
  ↓
UI refresh
  ↓
backup/sync queue
```

Do not rely on periodic UI autosave alone.

The database transaction is the actual save.

---

# 25. Audit trail

Track:

- Create
- Edit
- Delete
- Payment
- Checkout
- Inventory purchase
- Inventory sale
- Inventory reset
- Room status change
- Reservation cancellation
- Backup
- Restore
- Import
- Export
- Settings change

Each audit event should have:

```text
timestamp
deviceId
user/staff
entity
entityId
action
before
after
```

---

# 26. Android production project structure

Recommended structure:

```text
lib/
├── main.dart
│
├── core/
│   ├── constants/
│   ├── errors/
│   ├── utils/
│   ├── security/
│   └── result/
│
├── data/
│   ├── database/
│   ├── models/
│   ├── repositories/
│   ├── migrations/
│   └── storage/
│
├── domain/
│   ├── entities/
│   ├── services/
│   ├── calculations/
│   └── rules/
│
├── features/
│   ├── dashboard/
│   ├── reservations/
│   ├── bookings/
│   ├── guests/
│   ├── rooms/
│   ├── folio/
│   ├── payments/
│   ├── invoices/
│   ├── inventory/
│   ├── pos/
│   ├── housekeeping/
│   ├── expenses/
│   ├── reports/
│   ├── audit/
│   ├── import_export/
│   ├── backup/
│   ├── sync/
│   └── settings/
│
├── integrations/
│   ├── ocr/
│   ├── camera/
│   ├── qr/
│   ├── whatsapp/
│   ├── phone/
│   ├── payments/
│   ├── google_drive/
│   └── documents/
│
└── ui/
    ├── aurelia_theme/
    ├── widgets/
    └── navigation/
```

---

# 27. Core database entities

Recommended minimum entities:

```text
HotelSettings
FeatureSettings
Staff
Role
Guest
GuestOccupant
GuestVehicle
GuestDocument
Room
RoomType
Floor
Reservation
Booking
Folio
FolioLine
Payment
Invoice
InvoiceLine
InventoryItem
InventoryPurchase
InventorySale
InventoryAdjustment
InventoryReset
Expense
HousekeepingTask
HousekeepingChecklistItem
AuditEvent
BackupRecord
SyncRecord
ChangeJournal
Notification
TermsAndConditions
```

---

# 28. Calculation engine

Do not put business calculations directly inside UI widgets.

Use services such as:

```text
BookingCalculator
FolioCalculator
PaymentCalculator
InventoryCalculator
RoomAvailabilityCalculator
ReservationCalculator
InvoiceCalculator
ReportCalculator
```

Example:

```text
BookingCalculator
 -> nights
 -> room charge
 -> discount
 -> tax
 -> guest count
 -> total

FolioCalculator
 -> room charges
 -> POS
 -> services
 -> discounts
 -> taxes

PaymentCalculator
 -> verified payments
 -> paid
 -> balance

InventoryCalculator
 -> opening
 -> purchases
 -> sales
 -> waste
 -> closing
```

This is the key to making the PMS automatic and reliable.

---

# 29. Transaction safety

For critical operations use database transactions.

Example POS sale:

```text
BEGIN TRANSACTION

check stock
create inventory sale
decrease calculated stock
create folio line
recalculate folio
create audit event
create change journal

COMMIT
```

If anything fails:

```text
ROLLBACK
```

This prevents inventory saying “sold” while the guest folio says nothing was purchased.

---

# 30. Production export implementation

Flutter should generate real files locally:

```text
PDF
XLSX
CSV
JSON
PNG
JPG
```

Then Android can use the system share sheet.

For WhatsApp:

```text
generate invoice
    |
    v
save temporary file
    |
    v
Android share intent
    |
    v
WhatsApp
```

For direct phone:

```text
phone number
   |
Android call intent
```

For OCR:

```text
camera/gallery
   |
native OCR
   |
editable guest form
```

---

# 31. CI/CD

The existing project should continue using GitHub Actions to build the Android APK.

Conceptual workflow:

```text
git push
   |
GitHub Actions
   |
Flutter setup
   |
dependencies
   |
analyze
   |
test
   |
assembleRelease
   |
APK artifact
```

Gradle remains responsible for the Android build.

The local Termux environment can be used for source editing/testing, while GitHub Actions can provide a clean Linux build environment when local Android toolchain/AAPT2 compatibility is an issue.

---

# 32. Important implementation rules

1. Never overwrite historical financial records.
2. Never calculate balance from UI text.
3. Never trust OCR without user verification.
4. Never treat a generated QR as proof of payment.
5. Never mark payment verified without verified payment evidence.
6. Never make core PMS operations internet-dependent.
7. Never use Google Drive as the transactional database.
8. Never destroy history during inventory reset.
9. Never export arbitrary database JSON as a visual report.
10. Every financial mutation should be auditable.
11. Every inventory sale should connect to its folio when applicable.
12. Every checkout should trigger housekeeping workflow.
13. Every room availability decision must check overlapping dates.
14. Every feature should respect its Settings toggle.
15. Preserve the Aurelia UI as the master design language.

---

# 33. Current prototype vs production

### Already represented by the prototype

- PMS navigation/workflows
- Guest/booking/reservation concepts
- Room-rate automation
- Guest count automation
- Inventory ledger concept
- POS/folio relationship
- Inventory reporting
- Export workflow
- Local persistence concept
- Master feature controls

### Must be implemented natively in Flutter for production

- Android camera integration
- Gallery/document picker
- Native OCR
- Local QR generation/scanning
- Android share intents
- Direct phone/WhatsApp integration
- Real PDF/XLSX generation
- Google Drive authentication/sync
- Verified payment callbacks
- Background backup workers
- Secure local database
- Production encryption/security
- Android permissions
- Full conflict-safe synchronization

---

# 34. Final implementation philosophy

The target is not a simple billing application.

The target is:

**AURELIA 5★ HOTEL — Offline-First Automated PMS**

The system should behave like an operational engine:

```text
Input once
   ↓
Validate
   ↓
Calculate
   ↓
Save transaction
   ↓
Update related records
   ↓
Audit
   ↓
Notify
   ↓
Backup/sync
   ↓
Refresh dashboard
   ↓
Generate reports
```

The user should not repeatedly calculate:
- room totals
- guest counts
- balances
- inventory
- POS totals
- folio totals
- pending amounts
- report totals
- room availability

The software should calculate these from authoritative records.

---

## 35. Blueprint status

This README describes the structure and implementation direction represented by the supplied HTML prototype and the requested PMS behavior.

It should be used as the handoff document when converting the prototype into the Flutter/Dart production application.

Do not replace the Aurelia UI with the UI from the reference APK. The reference APK is only a workflow/function inspiration source.

