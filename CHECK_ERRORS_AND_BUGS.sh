#!/data/data/com.termux/files/usr/bin/bash
set +e

ROOT="$HOME/HotelApp"
cd "$ROOT"

REPORT="ERROR_BUG_REPORT.txt"
: > "$REPORT"

echo "========================================" | tee -a "$REPORT"
echo " AURELIA PMS — ERROR & BUG CHECK" | tee -a "$REPORT"
echo " Date: $(date)" | tee -a "$REPORT"
echo "========================================" | tee -a "$REPORT"
echo | tee -a "$REPORT"

section() {
  echo | tee -a "$REPORT"
  echo "### $1" | tee -a "$REPORT"
  echo "----------------------------------------" | tee -a "$REPORT"
}

section "1. Git status"
git status --short 2>&1 | tee -a "$REPORT"

section "2. Current branch"
git branch --show-current 2>&1 | tee -a "$REPORT"

section "3. Git remote"
git remote -v 2>&1 | tee -a "$REPORT"

section "4. Kotlin/Java source count"
find app/src/main/java -type f \( -name "*.kt" -o -name "*.java" \) | wc -l | tee -a "$REPORT"

section "5. TODO / FIXME / placeholder scan"
grep -RniE "TODO|FIXME|IMPLEMENT|NOT IMPLEMENTED|PLACEHOLDER|throw NotImplementedError|TODO\\(" \
  app/src/main/java \
  --include="*.kt" --include="*.java" 2>/dev/null | tee -a "$REPORT"

section "6. Dangerous empty implementations"
grep -RniE '\{\s*\}|return null|return emptyList\(\)|return false|return 0' \
  app/src/main/java \
  --include="*.kt" --include="*.java" 2>/dev/null | tee -a "$REPORT"

section "7. Duplicate class names"
find app/src/main/java -type f \( -name "*.kt" -o -name "*.java" \) \
  | sed 's#.*/##' \
  | sort \
  | uniq -d | tee -a "$REPORT"

section "8. Duplicate important domain/use-case classes"
grep -RnlE "class (GetLedgerBalanceUseCase|RecordVerifiedPaymentUseCase|HotelRepository|HotelViewModel)" \
  app/src/main/java --include="*.kt" 2>/dev/null | tee -a "$REPORT"

section "9. Payment announcement files"
find app/src/main/java -type f \
  | grep -E 'UpiPaymentAnnouncement|Payment.*Announcement' \
  | tee -a "$REPORT"

section "10. Room database / migration references"
grep -RniE "RoomDatabase|@Database|Migration|fallbackToDestructiveMigration|version\s*=" \
  app/src/main/java --include="*.kt" 2>/dev/null | tee -a "$REPORT"

section "11. Hard-coded Android SDK / build configuration"
grep -RniE "compileSdk|minSdk|targetSdk|kotlin|agp|com.android.application" \
  build.gradle* settings.gradle* gradle.properties app/build.gradle* \
  2>/dev/null | tee -a "$REPORT"

section "12. Gradle wrapper"
if [ -x "./gradlew" ]; then
  ./gradlew --version 2>&1 | tee -a "$REPORT"
else
  echo "ERROR: gradlew missing or not executable" | tee -a "$REPORT"
fi

section "13. Gradle dependency resolution / configuration check"
if [ -x "./gradlew" ]; then
  ./gradlew :app:tasks --all --no-daemon 2>&1 | tee /tmp/hotel_gradle_check.log
  RC=${PIPESTATUS[0]}
  echo "Gradle configuration exit code: $RC" | tee -a "$REPORT"

  if [ "$RC" -ne 0 ]; then
    echo "!!! GRADLE CONFIGURATION ERROR !!!" | tee -a "$REPORT"
    tail -n 100 /tmp/hotel_gradle_check.log | tee -a "$REPORT"
  fi
else
  echo "SKIPPED: gradlew unavailable" | tee -a "$REPORT"
fi

section "14. Kotlin compiler/build check"
if [ -x "./gradlew" ]; then
  ./gradlew :app:compileDebugKotlin --no-daemon 2>&1 | tee /tmp/hotel_kotlin_check.log
  RC=${PIPESTATUS[0]}
  echo "Kotlin compile exit code: $RC" | tee -a "$REPORT"

  if [ "$RC" -ne 0 ]; then
    echo "!!! KOTLIN COMPILE ERRORS !!!" | tee -a "$REPORT"
    grep -nE "e: |error: |FAILURE:|Caused by:" /tmp/hotel_kotlin_check.log \
      | tail -n 150 | tee -a "$REPORT"
  fi
else
  echo "SKIPPED: gradlew unavailable" | tee -a "$REPORT"
fi

section "15. Java compiler/build check"
if [ -x "./gradlew" ]; then
  ./gradlew :app:compileDebugJavaWithJavac --no-daemon 2>&1 | tee /tmp/hotel_java_check.log
  RC=${PIPESTATUS[0]}
  echo "Java compile exit code: $RC" | tee -a "$REPORT"

  if [ "$RC" -ne 0 ]; then
    echo "!!! JAVA COMPILE ERRORS !!!" | tee -a "$REPORT"
    grep -nE "error: |FAILURE:|Caused by:" /tmp/hotel_java_check.log \
      | tail -n 150 | tee -a "$REPORT"
  fi
else
  echo "SKIPPED: gradlew unavailable" | tee -a "$REPORT"
fi

section "16. Full debug APK build check"
if [ -x "./gradlew" ]; then
  ./gradlew :app:assembleDebug --no-daemon 2>&1 | tee /tmp/hotel_apk_check.log
  RC=${PIPESTATUS[0]}
  echo "APK build exit code: $RC" | tee -a "$REPORT"

  if [ "$RC" -ne 0 ]; then
    echo "!!! APK BUILD FAILED !!!" | tee -a "$REPORT"
    grep -nE "e: |error: |FAILURE:|Caused by:|SDK location|Could not determine" \
      /tmp/hotel_apk_check.log \
      | tail -n 200 | tee -a "$REPORT"
  fi
else
  echo "SKIPPED: gradlew unavailable" | tee -a "$REPORT"
fi

section "17. Existing payment announcement protection check"
grep -RniE "Payment received|UpiPaymentAnnouncementManager|UpiPaymentAnnouncementService" \
  app/src/main/java/com/example \
  --include="*.kt" 2>/dev/null | tee -a "$REPORT"

section "18. Suspicious payment creation"
grep -RniE "Payment\(" app/src/main/java/com/example \
  --include="*.kt" 2>/dev/null | tee -a "$REPORT"

section "19. Final summary"
echo "Report saved to: $ROOT/$REPORT" | tee -a "$REPORT"

echo | tee -a "$REPORT"
echo "IMPORTANT:" | tee -a "$REPORT"
echo "- Android SDK errors in Termux are environment errors, not necessarily source-code errors." | tee -a "$REPORT"
echo "- Do NOT create a fake local.properties just to bypass the SDK error." | tee -a "$REPORT"
echo "- Payment announcement files are only inspected; this checker does not modify them." | tee -a "$REPORT"

echo
echo "========================================"
echo "CHECK FINISHED"
echo "========================================"
echo "Report: $REPORT"
