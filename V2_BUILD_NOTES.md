# Hotel Billing V2 build notes

This source package is intended for the separate `Hotel-apk-v2` repository.

Build stack:
- Android Gradle Plugin 9.1.1
- Gradle wrapper 9.3.1
- Kotlin 2.2.10
- KSP 2.3.6
- JDK 17 on GitHub Actions

Important:
- The Google Services Gradle plugin was removed because the app source does not currently use Firebase APIs and no `google-services.json` was supplied. This removes the missing-file build warning.
- Google Drive backup/authentication should be configured separately before enabling a real Google account flow.
- Debug builds use normal Android debug signing.
