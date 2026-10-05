# V2.1 product update

## Performance / design
- Clean Material 3 typography with compact mode preference.
- Dark mode and compact UI are user-controlled from Settings.
- Existing offline Room database remains the source of truth.

## Smart ID OCR
- On-device ML Kit Latin text recognition.
- Camera or uploaded image can trigger OCR.
- Recognizes common Aadhaar, PAN, Driving Licence, Voter ID and Passport patterns.
- Downsamples very large photos before OCR for faster processing.
- Shows a confirmation dialog before applying detected name/type/number.

## Reminders
- Checkout reminder approximately one hour before checkout.
- Pending-balance reminder at checkout time when an amount remains.
- Android notification permission is requested on Android 13+.

## Workbook import
- Preserves every original workbook column/value in imported booking notes.
- Extracts embedded worksheet images and attaches the matching image to the guest ID photo when the XLSX drawing anchors contain row information.

## Invoices
- Classic / Tax Invoice, Payment Receipt, Proforma Invoice and Estimate document styles.
- Style can be selected from Invoice Generator or Settings.
- Existing itemized totals and UPI QR remain available.

## Pending payments / WhatsApp
- Dashboard lists guest name, room and outstanding amount.
- WhatsApp check-in message is no longer blank.
- Pending-payment WhatsApp text explains the bill components and reason for the requested amount.
- Guest/room billing PDF includes an amount-specific UPI QR when a pending balance exists.

## Reservations / export
- Room selection uses a dedicated dialog so it cannot render behind the reservation form.
- Workbook export supports guest-name filtering in addition to dates and booking IDs.
