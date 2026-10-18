#!/usr/bin/env bash
# Captures Play Store screenshots and the promo-tour recording on the emulator.
# Output goes to $OUT (screenshots/, video/ and timeline.tsv).
set -u
OUT="${OUT:-$RUNNER_TEMP/captures}"
APP=com.sippo.app.debug
RUNNER="$APP.test/androidx.test.runner.AndroidJUnitRunner"
mkdir -p "$OUT/screenshots" "$OUT/video"

adb shell settings put global hide_error_dialogs 1 || true
adb shell input keyevent 82 || true
sleep 20

# Clean status bar: 9:41, full battery and signal, no notification icons.
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" > /dev/null; }
demo enter
demo clock -e hhmm 0941
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4
demo notifications -e visible false

./gradlew --quiet installDebug installDebugAndroidTest || exit 1
adb shell pm grant $APP android.permission.POST_NOTIFICATIONS || true

run() {
  local res
  res=$(adb shell am instrument -w -e class "com.sippo.app.StoreCapture#$1" $RUNNER)
  echo "$res"
  if ! echo "$res" | grep -q "OK (1 test)"; then
    msg=$(echo "$res" | grep -v '^$' | head -n 30 | cut -c1-300)
    msg=${msg//'%'/'%25'}; msg=${msg//$'\r'/''}; msg=${msg//$'\n'/'%0A'}
    echo "::error title=StoreCapture#$1 failed::$msg"
    return 1
  fi
}

# 1) Screenshots at the device's full 1080×2400.
WHAT="${WHAT:-both}"
status=0
if [ "$WHAT" != "tour" ]; then run screenshots || status=1; fi
adb pull /sdcard/Android/data/$APP/files/store/. "$OUT/screenshots/" > /dev/null

if [ "$WHAT" = "screenshots" ]; then ls -laR "$OUT"; exit $status; fi

# 2) Tour recording. The software-rendered emulator is too slow for smooth video at full size,
#    so drop to 540×1200 at the same dp width. screenrecord stops after 180 s: record segments.
adb shell rm -rf /sdcard/Android/data/$APP/files/store
adb shell wm size 540x1200
adb shell wm density 210
sleep 3
(
  for i in 1 2 3; do
    echo "seg$i $(adb shell date +%s%3N | tr -d '\r')" >> "$OUT/video/segments.txt"
    adb shell screenrecord --bit-rate 6000000 --time-limit 180 /sdcard/tour$i.mp4
  done
) &
rec=$!
sleep 2
run tour || status=1
sleep 2
adb shell pkill -INT screenrecord || true
sleep 3
kill $rec 2> /dev/null || true
for i in 1 2 3; do adb pull /sdcard/tour$i.mp4 "$OUT/video/" > /dev/null 2>&1 || true; done
adb pull /sdcard/Android/data/$APP/files/store/. "$OUT/video/" > /dev/null 2>&1 || true
adb shell wm size reset; adb shell wm density reset
ls -laR "$OUT"
exit $status
