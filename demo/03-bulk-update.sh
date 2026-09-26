#!/usr/bin/env bash
# Demo scenario 3: Bulk update activities to DONE/BLOCKED status in one call.
#
# Transitions all 4 activities from 02-create-activities.sh in a single
# PATCH /api/activities/bulk-status request — the shift handover close-out flow — then lists
# the programme's activities to show the final state. Prints each curl command followed by
# its pretty-printed response.
#
# Usage: ./demo/01-create-programme.sh and ./demo/02-create-activities.sh must run first.
#        BASE_URL=http://localhost:8080 ./demo/03-bulk-update.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
STORE_ID="store-1"
STATE_FILE="$(dirname "$0")/.demo-state"

if [ ! -f "${STATE_FILE}" ]; then
  echo "ERROR: ${STATE_FILE} not found — run 01-create-programme.sh and 02-create-activities.sh first." >&2
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

echo "Scenario 3: Bulk update to DONE/BLOCKED in one call — programme ${PROGRAMME_ID}"
echo "Activities: ${ACT_1_ID} (-> DONE), ${ACT_2_ID} (-> BLOCKED), ${ACT_3_ID} (-> DONE), ${ACT_4_ID} (-> BLOCKED)"

announce "3a. PATCH /api/activities/bulk-status (4 activities, one call)"
BULK_RESULT=$(req PATCH "/api/activities/bulk-status" "staff-2" "SUPERVISOR" \
  "{\"updates\": [
     {\"id\": \"${ACT_1_ID}\", \"status\": \"DONE\"},
     {\"id\": \"${ACT_2_ID}\", \"status\": \"BLOCKED\"},
     {\"id\": \"${ACT_3_ID}\", \"status\": \"DONE\"},
     {\"id\": \"${ACT_4_ID}\", \"status\": \"BLOCKED\"}
   ]}")
printf '%s' "${BULK_RESULT}" | pretty

announce "3b. GET /api/activities?programme=${PROGRAMME_ID} (final state)"
FINAL_LIST=$(req GET "/api/activities?programme=${PROGRAMME_ID}" "staff-2" "SUPERVISOR")
printf '%s' "${FINAL_LIST}" | pretty

echo
echo "Done. All 4 activities transitioned in a single request."
