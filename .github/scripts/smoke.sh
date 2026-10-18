#!/usr/bin/env bash
# Runs the emulator smoke test; on failure, reports the crash from logcat as an annotation.
# Freshly booted CI emulators often raise "Launcher isn't responding" dialogs that swallow taps.
adb shell settings put global hide_error_dialogs 1 || true
adb shell input keyevent 82 || true
sleep 20
adb logcat -c || true
.github/scripts/gradle.sh connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sippo.app.SmokeTest
status=$?
if [ $status -ne 0 ]; then
  crash=$(adb logcat -d -v brief | grep -A 40 -E "FATAL EXCEPTION|AndroidRuntime: Caused by" | head -n 80 | cut -c1-300)
  if [ -n "$crash" ]; then
    crash=${crash//'%'/'%25'}; crash=${crash//$'\r'/''}; crash=${crash//$'\n'/'%0A'}
    echo "::error title=app crash::${crash}"
  fi
  res=$(find app/build/outputs/androidTest-results -name '*.xml' -exec grep -h -A 25 '<failure' {} + 2>/dev/null | head -n 40 | cut -c1-300)
  if [ -n "$res" ]; then
    res=${res//'%'/'%25'}; res=${res//$'\r'/''}; res=${res//$'\n'/'%0A'}
    echo "::error title=smoke test failure::${res}"
  fi
fi
exit $status
