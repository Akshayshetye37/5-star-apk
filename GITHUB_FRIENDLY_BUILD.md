# GitHub-friendly PMS source

This package is prepared as a source-control/build package:
- generated build directories are excluded
- local.properties is excluded
- IDE metadata is excluded
- local secrets/keystores are excluded
- Gradle wrapper files are retained if present
- source, resources, database code, workflows and documentation are retained

BUILD POLICY
- Build APK on GitHub Actions rather than Termux ARM64.
- Pin Gradle/AGP/Kotlin/KSP/JDK as a tested matrix.
- Never commit signing keys or passwords.
- Release signing uses GitHub Secrets when configured.
- Debug APK can be built without a private release keystore.

IMPORTANT
This is a GitHub-friendly source package, not a claim that every enterprise module is
already 34/34 implementation-complete. The enterprise acceptance documents remain in
the repository and must be satisfied before declaring the PMS complete.
