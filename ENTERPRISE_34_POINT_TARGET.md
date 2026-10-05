# 34-POINT ENTERPRISE HOTEL PMS — FINAL TARGET

The existing working PMS source is preserved as the base. The following are mandatory
for the polished final implementation:

1. Front Desk / PMS
2. Reservations
3. Rooms
4. Room Types & Rates
5. Guest Management
6. ID / OCR
7. Housekeeping
8. Maintenance
9. Restaurant / POS
10. Kitchen / KDS
11. Room Service
12. Inventory
13. Purchasing / Suppliers
14. Cashier / Shifts
15. Folio / Billing
16. Payments
17. Finance / Ledger
18. Invoice
19. Night Audit
20. Revenue Management
21. Loyalty / VIP
22. Staff / Roles
23. Audit Log
24. Communications
25. Automation Engine
26. Backup / Restore
27. Reports
28. Import / Export
29. Settings / Modular Toggles
30. Relational Room Database
31. Clean Architecture
32. Transactional Checkout
33. Transactional Cleaning
34. Performance / Offline Reliability

CORE RULES
- FULL implementation only; partial is not accepted.
- No placeholder module counts as complete.
- Room/SQLite is the core single source of truth.
- Core PMS works offline.
- Module OFF hides/disables UI and never deletes data.
- Business-critical collections are relational Room entities.
- Domain/use cases sit between UI and repositories.
- Checkout is transactional and idempotent.
- Overpayment is explicit and never silently treated as income.
- Historical invoice values are immutable snapshots.
- Audit trail covers sensitive and financial changes.
- Backup/restore includes module data.
- Google Drive is optional; local SAF backup remains available.
- WorkManager is used for durable background work.
- ML Kit OCR is local.
- PDF/CSV/XLSX processing is local.
- GitHub Actions builds the APK with a pinned toolchain.

WHATSAPP
Guest/booking data
→ generate message matter
→ show editable message text box
→ WhatsApp button
→ open the guest's saved WhatsApp number/chat
→ staff handles sending.
The PMS does not silently send.

PRIMARY GUEST / INVOICE
- Every booking has one Primary Guest.
- Pending invoice is issued under the Primary Guest.
- Check-in and checkout/final invoice PDFs contain the Primary Guest plus all
  accompanying guest names/details appropriate for the document.
- Historical line values use unitPriceAtTimeOfOrder / equivalent snapshots.

CHECKOUT
Validate → Calculate → Payment → Finance → Invoice snapshot → Close booking →
CLEANING_REQUIRED → housekeeping task → notification → audit → reactive refresh.

CLEANING
CLEANING_REQUIRED → Start Cleaning → Inspection → AVAILABLE → room inventory update →
front-desk notification → audit.

XLSX
- No hardcoded row limit.
- Process every valid row the device can safely process.
- Support workbook row iteration and associated photos.
- Extract floating/drawing images and supported in-cell representations.
- Map each image to its source Excel row and imported record.
- Store extracted images as private local files and persist the relationship.
- Never silently stop at 35 or 135 rows.
- Continue after row-level failures.
- Report rows found/imported/failed and photos found/imported/failed.
- Report exact Excel row and reason for every failure.
- The user's 135-entry workbook is an acceptance test, not a maximum.
