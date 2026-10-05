# Hotel Billing System — Search & Guest Sharing Update

This project is a native Android / Kotlin + Jetpack Compose application using Room for offline storage.

## Added features
- Global search bar at the top of the main app.
- Room-backed partial search for guest name, phone, WhatsApp, booking ID and room number.
- Dedicated Guest / Person Details screen.
- Local ID photo capture/select/replace/delete.
- Single selected guest + room PDF generation.
- Native Android PDF sharing, opening and printing.
- Room screen actions for View Guest, Invoice and Share Guest PDF.
- Booking/payment history on the selected guest.
- Room database migration from v1 to v2 with searchable indexes.
- GitHub Actions workflow to build a debug APK.

## Privacy
Guest PDFs are generated locally. The share operation uses Android's native share sheet and does not upload guest information to an application server.

## Build in GitHub Actions
Push this repository to GitHub and run **Actions → Build Android APK**. The workflow uploads `app-debug.apk` as the `hotel-billing-debug-apk` artifact.

## Important
The uploaded source was a native Android/Kotlin project, not a Flutter/Dart project, so the implementation preserves the existing native architecture and Room database rather than converting it.
