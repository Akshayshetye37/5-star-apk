# Enterprise scope retained in this source package

Core:
34-point enterprise PMS target, offline-first Room/SQLite, Compose, MVVM/Clean
Architecture, WorkManager, ML Kit OCR, PDF/QR, backup/restore, import/export,
modular Settings toggles, transactional checkout/cleaning, finance ledger,
housekeeping, POS/KDS/room service, inventory/purchasing, cashier, night audit,
revenue, loyalty, staff/roles, audit, communications and automation.

Advanced target:
- Drag/drop reservation dashboard
- Digital signature
- Bulk check-in/out
- Live room assignment
- Multi-currency
- POS/spa/bar charge routing
- City ledger/corporate accounts
- Payment gateway adapter layer
- Split billing
- OTA/channel sync adapter layer
- Yield management
- Rate parity
- Direct booking integration adapter
- Waitlist/overbooking controls
- Email queue/integration adapter
- Preventive maintenance
- Housekeeping performance
- Hardware integration adapter layer

XLSX:
- Unlimited valid rows (no hardcoded limit)
- Embedded/floating and supported in-cell photo extraction
- Excel-row-to-photo-to-guest mapping
- Private local photo storage
- Row/photo-level errors
- Import summary
- 135-entry workbook is a test case, not a limit

Invoice:
- Pending invoice under Primary Guest
- Check-in and checkout PDFs include Primary Guest plus all guest details
- Historical invoice snapshots

WhatsApp:
- Generate message matter
- Editable text box
- Button opens saved guest WhatsApp number/chat
- No silent sending
