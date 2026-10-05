# Updated Hotel Billing App

Implemented in this update:
- Unlimited guests per booking/room with separate name/contact/WhatsApp/ID/ID-photo records.
- Add Guest button below guest ID photo section.
- Multiple vehicle registration numbers per guest.
- Cash + Online/UPI split payments and corrected total-paid/pending calculation.
- Reliable UPI amount QR generation using ZXing.
- Manual fixed QR image setting.
- Hotel logo and slogan settings; uploaded hotel logo included as the default app logo.
- Tap-to-call and WhatsApp actions.
- Room PDF with all guests and single-person PDF.
- Invoice PDF sharing for the main guest.
- Pending-balance list with call/WhatsApp/PDF actions.
- Automatic local backup and optional Google Drive-folder backup toggles.
- Sensitive ID backup toggle.
- Selected/all Excel, Word and PDF export in one row/column-oriented sheet/table.
- Room/guest records included in backup/export.
- Room database migration 2 -> 3 for multi-guest records.

Important:
- Enter the hotel's real UPI ID in Settings for amount-specific QR payments.
- The Google Drive backup option uses Android's folder picker; choose a folder in the desired Google Drive account.
- The debug APK workflow remains configured for Gradle 9.3.1 / JDK 17.


## Workbook import/export and future-proof billing updates

- Settings now contains an Import / Export Workbook section.
- Excel import is header-driven and reads the supplied Hari Om workbook's Booking sheet.
- Optional grouping can combine rows with the same room, stay dates, contact and WhatsApp into one booking with multiple guests.
- Existing booking IDs are skipped during import to protect existing records.
- Excel export uses the hotel's familiar row/column layout and includes Booking, Payments and Reservation sheets.
- Export supports all matching records, one booking ID, multiple comma-separated booking IDs, and From/To date ranges.
- Room charges auto-calculate from room rate × nights for new bookings (and remain editable).
- Booking payment summary explicitly shows Advance, Total Paid, Discount, Grand Total and Pending Balance.
- Booking list shows check-in/check-out dates with star markers.
- Manual QR and hotel logo upload remain configurable from Settings.
