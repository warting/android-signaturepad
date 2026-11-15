#!/usr/bin/env bash
# Preserve the failing test status while collecting actual screenshot output.
set -uo pipefail
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=se.warting.signaturepad.app.InkSignatureTest \
  --stacktrace
ink_test_status=$?
mkdir -p app/build/ink-screenshots
adb pull /sdcard/Android/data/se.warting.signaturepad/files/ink-screenshots/. app/build/ink-screenshots/ || true
exit "$ink_test_status"
