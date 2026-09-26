#!/usr/bin/env bash
# Demo scenario 1: Create Programmes.
#
# Creates a programme and adds staff-2 and staff-3 to it, printing the exact curl command for
# each call followed by its pretty-printed response — so each step is a clean "input then
# output" screenshot. Writes the resulting ids to demo/.demo-state so 02-create-activities.sh
# and 03-bulk-update.sh can reuse them.
#
# Usage: BASE_URL=http://localhost:8080 ./demo/01-create-programme.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
STORE_ID="store-1"
MANAGER_ID="staff-1"       # STORE_MANAGER — creates the programme
STATE_FILE="$(dirname "$0")/.demo-state"

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

echo "Scenario 1: Create Programmes — against ${BASE_URL}"

announce "1a. POST /api/programmes"
PROGRAMME=$(req POST "/api/programmes" "${MANAGER_ID}" "STORE_MANAGER" \
  '{"name": "Spring Planogram Refresh", "status": "ACTIVE"}')
printf '%s' "${PROGRAMME}" | pretty
PROGRAMME_ID=$(printf '%s' "${PROGRAMME}" | json_field id)

announce "1b. POST /api/programmes/${PROGRAMME_ID}/members (staff-2, LEAD)"
MEMBER_2=$(req POST "/api/programmes/${PROGRAMME_ID}/members" "${MANAGER_ID}" "STORE_MANAGER" \
  '{"staffId": "staff-2", "role": "LEAD"}')
printf '%s' "${MEMBER_2}" | pretty

announce "1c. POST /api/programmes/${PROGRAMME_ID}/members (staff-3, CONTRIBUTOR)"
MEMBER_3=$(req POST "/api/programmes/${PROGRAMME_ID}/members" "${MANAGER_ID}" "STORE_MANAGER" \
  '{"staffId": "staff-3", "role": "CONTRIBUTOR"}')
printf '%s' "${MEMBER_3}" | pretty

echo "PROGRAMME_ID=${PROGRAMME_ID}" > "${STATE_FILE}"
echo
echo "Saved PROGRAMME_ID=${PROGRAMME_ID} to ${STATE_FILE}"
echo "Next: ./demo/02-create-activities.sh"
