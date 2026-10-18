#!/usr/bin/env bash
# Runs Gradle and, on failure, surfaces compiler/lint errors as a GitHub annotation
# so they show on the run summary without opening the raw log.
set -o pipefail
log="$(mktemp)"
./gradlew --stacktrace "$@" 2>&1 | tee "$log"
status=$?
if [ $status -ne 0 ]; then
  msg=$(grep -E '^e: |^w: .*(error)|error:|What went wrong|^> |FAILED|Caused by|Exception:|expected:|AssertionError|Lint found|\.kt:[0-9]+' "$log" \
    | grep -v '^> Task' | awk '!seen[$0]++' | head -n 120 | cut -c1-400)
  msg=${msg//'%'/'%25'}
  msg=${msg//$'\r'/'%0D'}
  msg=${msg//$'\n'/'%0A'}
  echo "::error title=gradle $*::${msg}"
fi
exit $status
