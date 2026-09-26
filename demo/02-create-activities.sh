#!/usr/bin/env bash
# Demo scenario 2: Create multiple activities and assign them to staff.
#
# Creates 4 activities under the programme from 01-create-programme.sh — two assigned to
# staff-2, two to staff-3 — each created by the person it's assigned to, with all relevant
# parameters (title, category, priority, programmeId, assigneeId). Prints each curl command
# followed by its pretty-printed response. Appends the resulting ids to demo/.demo-state for
# 03-bulk-update.sh.
#
# Usage: ./demo/01-create-programme.sh must be run first.
#        BASE_URL=http://localhost:8080 ./demo/02-create-activities.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
STORE_ID="store-1"
STATE_FILE="$(dirname "$0")/.demo-state"

if [ ! -f "${STATE_FILE}" ]; then
  echo "ERROR: ${STATE_FILE} not found — run ./demo/01-create-programme.sh first." >&2
  exit 1
fi
# shellcheck source=/dev/null
source "${STATE_FILE}"

JSON_TOOL=""
for candidate in "jq ." "python -m json.tool" "python3 -m json.tool"; do
  if printf '{}' | ${candidate} >/dev/null 2>&1; then
    JSON_TOOL="${candidate}"
    break
  fi
done
pretty() { if [ -n "${JSON_TOOL}" ]; then ${JSON_TOOL}; else cat; fi; }

json_field() {
  local field="$1"
  case "${JSON_TOOL}" in
    "python -m json.tool") python -c "import json,sys; print(json.load(sys.stdin)['${field}'])" ;;
    "python3 -m json.tool") python3 -c "import json,sys; print(json.load(sys.stdin)['${field}'])" ;;
    "jq .") jq -r ".${field}" ;;
    *) grep -o "\"${field}\":\"[^\"]*\"" | head -1 | cut -d'"' -f4 ;;
  esac
}

# req METHOD PATH USER_ID ROLE [BODY] — prints the curl invocation, then performs it and
# returns the raw JSON response on stdout (the printed command goes to stderr so it never ends
# up captured inside a `VAR=$(req ...)` assignment).
req() {
  local method="$1" path="$2" user_id="$3" role="$4" body="${5:-}"
  {
    echo "\$ curl -X ${method} \"${BASE_URL}${path}\" \\"
    echo "    -H \"X-User-Id: ${user_id}\" -H \"X-Store-Id: ${STORE_ID}\" -H \"X-User-Role: ${role}\" \\"
    if [ -n "${body}" ]; then
      echo "    -H \"Content-Type: application/json\" \\"
      echo "    -d '${body}'"
    fi
  } >&2
  if [ -n "${body}" ]; then
    curl -s -X "${method}" "${BASE_URL}${path}" \
      -H "X-User-Id: ${user_id}" -H "X-Store-Id: ${STORE_ID}" -H "X-User-Role: ${role}" \
      -H "Content-Type: application/json" -d "${body}"
  else
    curl -s -X "${method}" "${BASE_URL}${path}" \
      -H "X-User-Id: ${user_id}" -H "X-Store-Id: ${STORE_ID}" -H "X-User-Role: ${role}"
  fi
}

announce() { echo; echo "== $1 =="; }

echo "Scenario 2: Create activities and assign to staff — programme ${PROGRAMME_ID}"

announce "2a. POST /api/activities (assigned to staff-2, HIGH/STOCKTAKE)"
ACT_1=$(req POST "/api/activities" "staff-2" "SUPERVISOR" \
  "{\"title\": \"Count backroom stock before refit\", \"programmeId\": \"${PROGRAMME_ID}\", \"category\": \"STOCKTAKE\", \"priority\": \"HIGH\", \"assigneeId\": \"staff-2\"}")
printf '%s' "${ACT_1}" | pretty
ACT_1_ID=$(printf '%s' "${ACT_1}" | json_field id)

announce "2b. POST /api/activities (assigned to staff-2, NORMAL/MERCHANDISING)"
ACT_2=$(req POST "/api/activities" "staff-2" "SUPERVISOR" \
  "{\"title\": \"Reset promotional end cap\", \"programmeId\": \"${PROGRAMME_ID}\", \"category\": \"MERCHANDISING\", \"priority\": \"NORMAL\", \"assigneeId\": \"staff-2\"}")
printf '%s' "${ACT_2}" | pretty
ACT_2_ID=$(printf '%s' "${ACT_2}" | json_field id)

announce "2c. POST /api/activities (assigned to staff-3, HIGH/COMPLIANCE)"
ACT_3=$(req POST "/api/activities" "staff-3" "ASSOCIATE" \
  "{\"title\": \"Verify fire exit signage\", \"programmeId\": \"${PROGRAMME_ID}\", \"category\": \"COMPLIANCE\", \"priority\": \"HIGH\", \"assigneeId\": \"staff-3\"}")
printf '%s' "${ACT_3}" | pretty
ACT_3_ID=$(printf '%s' "${ACT_3}" | json_field id)

announce "2d. POST /api/activities (assigned to staff-3, NORMAL/MAINTENANCE)"
ACT_4=$(req POST "/api/activities" "staff-3" "ASSOCIATE" \
  "{\"title\": \"Repair backroom shelving unit\", \"programmeId\": \"${PROGRAMME_ID}\", \"category\": \"MAINTENANCE\", \"priority\": \"NORMAL\", \"assigneeId\": \"staff-3\"}")
printf '%s' "${ACT_4}" | pretty
ACT_4_ID=$(printf '%s' "${ACT_4}" | json_field id)

{
  echo "ACT_1_ID=${ACT_1_ID}"
  echo "ACT_2_ID=${ACT_2_ID}"
  echo "ACT_3_ID=${ACT_3_ID}"
  echo "ACT_4_ID=${ACT_4_ID}"
} >> "${STATE_FILE}"

echo
echo "Saved 4 activity ids to ${STATE_FILE}"
echo "Next: ./demo/03-bulk-update.sh"
