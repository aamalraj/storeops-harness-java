#!/usr/bin/env bash
# Seeds a small demo scenario against a freshly (re)started StoreOps API:
#
#   1. Create a programme
#   2. Add staff-2 and staff-3 to it
#   3. Create two activities under the programme — one assigned to staff-2, one to staff-3
#   4. Reassign the staff-2 activity to staff-3
#
# Every response is echoed pretty-printed. Storage is in-memory (see InMemoryActivityRepository /
# InMemoryProgrammeRepository), so this only makes sense right after the server has (re)started —
# run it again later and it just creates a second, independent set of records.
#
# Usage: BASE_URL=http://localhost:8080 ./seed-demo.sh   (BASE_URL defaults to localhost:8080)

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
STORE_ID="store-1"

# Seeded identities (see InMemoryStaffRepository.seed()).
MANAGER_ID="staff-1"      # STORE_MANAGER — creates the programme
SUPERVISOR_ID="staff-2"   # SUPERVISOR
ASSOCIATE_ID="staff-3"    # ASSOCIATE

# ---------------------------------------------------------------------------
# Pick a working JSON tool once. `command -v` isn't enough on Windows — a
# `python3` on PATH can be a Microsoft Store alias stub that exists but fails
# when actually run — so each candidate is tried against real input.
# ---------------------------------------------------------------------------
JSON_TOOL=""
for candidate in "jq ." "python -m json.tool" "python3 -m json.tool"; do
  if printf '{}' | ${candidate} >/dev/null 2>&1; then
    JSON_TOOL="${candidate}"
    break
  fi
done

pretty() {
  if [ -n "${JSON_TOOL}" ]; then
    ${JSON_TOOL}
  else
    cat
  fi
}

# Extracts a top-level string field from a JSON response, using whichever
# python binary actually works (see JSON_TOOL detection above); falls back to
# a plain grep if neither python nor jq is usable.
json_field() {
  local field="$1"
  case "${JSON_TOOL}" in
    "python -m json.tool")
      python -c "import json,sys; print(json.load(sys.stdin)['${field}'])" ;;
    "python3 -m json.tool")
      python3 -c "import json,sys; print(json.load(sys.stdin)['${field}'])" ;;
    "jq .")
      jq -r ".${field}" ;;
    *)
      grep -o "\"${field}\":\"[^\"]*\"" | head -1 | cut -d'"' -f4 ;;
  esac
}

# req METHOD PATH USER_ID ROLE [BODY] — performs the call, prints nothing, returns raw JSON.
req() {
  local method="$1" path="$2" user_id="$3" role="$4" body="${5:-}"
  if [ -n "$body" ]; then
    curl -s -X "$method" "${BASE_URL}${path}" \
      -H "X-User-Id: ${user_id}" -H "X-Store-Id: ${STORE_ID}" -H "X-User-Role: ${role}" \
      -H "Content-Type: application/json" -d "$body"
  else
    curl -s -X "$method" "${BASE_URL}${path}" \
      -H "X-User-Id: ${user_id}" -H "X-Store-Id: ${STORE_ID}" -H "X-User-Role: ${role}"
  fi
}

# show LABEL RAW_JSON — prints a labeled, pretty-printed block.
show() {
  local label="$1" raw="$2"
  echo
  echo "== ${label} =="
  printf '%s' "$raw" | pretty
}

# require_id ID STEP_NAME — aborts with a clear message if a create/lookup step didn't return an id.
require_id() {
  local id="$1" step="$2"
  if [ -z "${id}" ]; then
    echo "ERROR: ${step} did not return an id — see the response above for the actual error." >&2
    exit 1
  fi
}

echo "Seeding demo data against ${BASE_URL} ..."

# 1. Create a programme (as the store manager).
PROGRAMME=$(req POST "/api/programmes" "${MANAGER_ID}" "STORE_MANAGER" \
  '{"name": "Spring Planogram Refresh", "status": "ACTIVE"}')
show "1. Create programme" "${PROGRAMME}"
PROGRAMME_ID=$(printf '%s' "${PROGRAMME}" | json_field id)
require_id "${PROGRAMME_ID}" "Create programme"

# 2. Add staff-2 and staff-3 to the programme.
MEMBER_2=$(req POST "/api/programmes/${PROGRAMME_ID}/members" "${MANAGER_ID}" "STORE_MANAGER" \
  '{"staffId": "staff-2", "role": "LEAD"}')
show "2a. Add staff-2 to programme (LEAD)" "${MEMBER_2}"

MEMBER_3=$(req POST "/api/programmes/${PROGRAMME_ID}/members" "${MANAGER_ID}" "STORE_MANAGER" \
  '{"staffId": "staff-3", "role": "CONTRIBUTOR"}')
show "2b. Add staff-3 to programme (CONTRIBUTOR)" "${MEMBER_3}"

# 3. Create two activities under the programme — each created by the person it's assigned to.
ACTIVITY_1=$(req POST "/api/activities" "${SUPERVISOR_ID}" "SUPERVISOR" \
  "{\"title\": \"Count backroom stock before refit\", \"programmeId\": \"${PROGRAMME_ID}\", \"category\": \"STOCKTAKE\", \"priority\": \"HIGH\", \"assigneeId\": \"staff-2\"}")
show "3a. Create activity assigned to staff-2" "${ACTIVITY_1}"
ACTIVITY_1_ID=$(printf '%s' "${ACTIVITY_1}" | json_field id)
require_id "${ACTIVITY_1_ID}" "Create activity for staff-2"

ACTIVITY_2=$(req POST "/api/activities" "${ASSOCIATE_ID}" "ASSOCIATE" \
  "{\"title\": \"Face up planogram aisle 4\", \"programmeId\": \"${PROGRAMME_ID}\", \"category\": \"MERCHANDISING\", \"priority\": \"NORMAL\", \"assigneeId\": \"staff-3\"}")
show "3b. Create activity assigned to staff-3" "${ACTIVITY_2}"
ACTIVITY_2_ID=$(printf '%s' "${ACTIVITY_2}" | json_field id)
require_id "${ACTIVITY_2_ID}" "Create activity for staff-3"

# 4. Reassign the staff-2 activity to staff-3 (handoff performed by staff-2, the outgoing assignee).
REASSIGNED=$(req PATCH "/api/activities/${ACTIVITY_1_ID}" "${SUPERVISOR_ID}" "SUPERVISOR" \
  '{"assigneeId": "staff-3"}')
show "4. Reassign activity 3a from staff-2 to staff-3" "${REASSIGNED}"

echo
echo "Done. programmeId=${PROGRAMME_ID} activity1Id=${ACTIVITY_1_ID} activity2Id=${ACTIVITY_2_ID}"
