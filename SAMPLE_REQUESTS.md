# Sample Requests — StoreOps API

Working `curl` examples for the endpoints an Activity or Programme depends on, and for creating
and updating Activities themselves. Run against the app started locally
(`./mvnw spring-boot:run`, listening on `http://localhost:8080`).

## Auth headers

There is no login endpoint — an upstream gateway is assumed to have already authenticated the
caller and forwards identity as headers (`ActorArgumentResolver`). Every request needs:

| Header | Required | Notes |
|---|---|---|
| `X-User-Id` | yes | must be a seeded staff id (see below) |
| `X-Store-Id` | yes | must match the caller's actual store to avoid 404s from store-scoping |
| `X-Region-Id` | no | defaults to `region-unknown` |
| `X-User-Role` | no | defaults to `ASSOCIATE`; one of `ASSOCIATE`, `SUPERVISOR`, `STORE_MANAGER`, `REGION_MANAGER` |

## 0. Staff — the underlying dependency (seeded, not created via the API)

`staff` is deliberately **read-only for every caller** (Section 3.6 of the capstone spec: "staff
is auth-only, no CRUD surface"). There is no `POST /api/staff` — staff members are seeded at
startup by `InMemoryStaffRepository.seed()`:

| id | store | region | name | role |
|---|---|---|---|---|
| `staff-1` | `store-1` | `region-north` | Ada Okafor | STORE_MANAGER |
| `staff-2` | `store-1` | `region-north` | Ben Sato | SUPERVISOR |
| `staff-3` | `store-1` | `region-north` | Cleo Marsh | ASSOCIATE |
| `staff-4` | `store-2` | `region-north` | Dev Patel | ASSOCIATE |

Use these ids as `X-User-Id` (caller identity) or as `assigneeId`/`staffId` values in the requests
below — there's nothing to "create" first.

```bash
# List staff at the caller's own store (store-1)
curl -s http://localhost:8080/api/staff \
  -H "X-User-Id: staff-1" -H "X-Store-Id: store-1" -H "X-User-Role: STORE_MANAGER"
```

## 1. Programmes — the other dependency Activities can reference

### Create a programme

```bash
curl -s -X POST http://localhost:8080/api/programmes \
  -H "X-User-Id: staff-1" -H "X-Store-Id: store-1" -H "X-User-Role: STORE_MANAGER" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Spring Planogram Refresh",
    "status": "ACTIVE"
  }'
```

`status` is optional (defaults to `DRAFT`). Valid values: `DRAFT`, `ACTIVE`, `PAUSED`,
`COMPLETED`, `ARCHIVED`. The response body's `id` is the `programmeId` used below.

### Add a staff member to the programme

```bash
curl -s -X POST http://localhost:8080/api/programmes/<programmeId>/members \
  -H "X-User-Id: staff-1" -H "X-Store-Id: store-1" -H "X-User-Role: STORE_MANAGER" \
  -H "Content-Type: application/json" \
  -d '{
    "staffId": "staff-3",
    "role": "CONTRIBUTOR"
  }'
```

`role` is optional (defaults to `CONTRIBUTOR`). Valid values: `LEAD`, `CONTRIBUTOR`, `OBSERVER`.
`staffId` must belong to the same store as the programme.

### List programmes for the caller's store

```bash
curl -s http://localhost:8080/api/programmes \
  -H "X-User-Id: staff-1" -H "X-Store-Id: store-1" -H "X-User-Role: STORE_MANAGER"
```

## 2. Activities — create

### Minimal — required fields only

```bash
curl -s -X POST http://localhost:8080/api/activities \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" -H "X-User-Role: ASSOCIATE" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Face up aisle 4",
    "category": "MERCHANDISING"
  }'
```

`priority` defaults to `NORMAL` when omitted; the activity is created as `PLANNED`.

### Full — every optional field supplied, linked to a programme

```bash
curl -s -X POST http://localhost:8080/api/activities \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" -H "X-User-Role: ASSOCIATE" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Count backroom stock before refit",
    "programmeId": "<programmeId from step 1>",
    "category": "STOCKTAKE",
    "priority": "HIGH",
    "assigneeId": "staff-2"
  }'
```

`category` — one of `REPLENISHMENT`, `MERCHANDISING`, `STOCKTAKE`, `MAINTENANCE`, `COMPLIANCE`,
`TRAINING` (required). `priority` — one of `LOW`, `NORMAL`, `HIGH`, `CRITICAL` (optional).
`assigneeId` — must be a staff id in the caller's own store, e.g. `staff-2` (optional).

### Invalid input, to see the error contract in action

```bash
# Blank title -> 400 VALIDATION_FAILED naming the field
curl -s -X POST http://localhost:8080/api/activities \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" \
  -d '{"title": "   ", "category": "MERCHANDISING"}'

# Unknown category -> 400 VALIDATION_FAILED naming the accepted values
curl -s -X POST http://localhost:8080/api/activities \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" \
  -d '{"title": "Reset end cap", "category": "KNITTING"}'

# Assignee from another store -> 400 VALIDATION_FAILED
curl -s -X POST http://localhost:8080/api/activities \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" \
  -d '{"title": "Reset end cap", "category": "MERCHANDISING", "assigneeId": "staff-4"}'
```

## 3. Activities — update (`PATCH /api/activities/{id}`)

Every field is optional but at least one must be supplied. Only the fields you send are changed.

### Change status only

```bash
curl -s -X PATCH http://localhost:8080/api/activities/<id> \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'
```

Valid `status` values: `PLANNED`, `IN_PROGRESS`, `BLOCKED`, `DONE`, `CANCELLED`.

### Reassign an activity to someone else

Only `assigneeId` needs to be sent — every other field is left as-is. The new assignee must
belong to the same store as the activity (see Section 3.5, "Error contract", and the walkthrough
below for what happens when it doesn't).

```bash
curl -s -i --location --request PATCH http://localhost:8080/api/activities/<id> \
  --header 'Content-Type: application/json' \
  --header 'X-User-Id: staff-2' \
  --header 'X-Store-Id: store-1' \
  --header 'X-User-Role: SUPERVISOR' \
  --data '{
    "assigneeId": "staff-3"
  }'
```

`X-User-Role` should match the caller's real role (`staff-2` is seeded as `SUPERVISOR` — see
Section 0's staff table) even though, for this endpoint specifically, it makes no functional
difference: `update` has no ownership restriction (only `delete` does — see `ActivityService.delete`), so any
caller at the activity's store can reassign it; `X-User-Id` here is just whoever is handing the
activity off, not an authorization requirement. Verified against a running instance: reassigning
activity `9ab2e17c-05c5-42c3-ae60-535bf623e01d` from `staff-2` to `staff-3` returned `200 OK` with
`"assigneeId":"staff-3"` in the response and an updated `updatedAt`.

**Common failure**: assigning a staff id from a different store returns `400 VALIDATION_FAILED`:

```json
{"code":"VALIDATION_FAILED","message":"Assignee 'staff-4' is not a member of this store", "status":400, ...}
```

(`staff-4` is seeded to `store-2` — see Section 0. Use `GET /api/staff` to list who's actually
valid for the caller's store before assigning.)

### Change priority, category, and reassign in one call

```bash
curl -s -X PATCH http://localhost:8080/api/activities/<id> \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" \
  -d '{
    "priority": "CRITICAL",
    "category": "COMPLIANCE",
    "assigneeId": "staff-2"
  }'
```

### Edge cases worth trying

```bash
# Empty body -> 400 VALIDATION_FAILED ("At least one of status, priority, category or
# assigneeId must be supplied")
curl -s -X PATCH http://localhost:8080/api/activities/<id> \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" -d '{}'

# Patching a DONE/CANCELLED (closed) activity -> 409 CONFLICT
curl -s -X PATCH http://localhost:8080/api/activities/<id> \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" -d '{"status": "DONE"}'
curl -s -X PATCH http://localhost:8080/api/activities/<id> \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" -d '{"priority": "LOW"}'
```

## 4. End-to-end walkthrough

```bash
# 1. Create a programme
PROG=$(curl -s -X POST http://localhost:8080/api/programmes \
  -H "X-User-Id: staff-1" -H "X-Store-Id: store-1" -H "X-User-Role: STORE_MANAGER" \
  -H "Content-Type: application/json" \
  -d '{"name": "Q4 Compliance Drive", "status": "ACTIVE"}' | jq -r '.id')

# 2. Add a contributor to it
curl -s -X POST http://localhost:8080/api/programmes/$PROG/members \
  -H "X-User-Id: staff-1" -H "X-Store-Id: store-1" -H "X-User-Role: STORE_MANAGER" \
  -H "Content-Type: application/json" \
  -d '{"staffId": "staff-3", "role": "CONTRIBUTOR"}'

# 3. Create an activity under that programme
ACT=$(curl -s -X POST http://localhost:8080/api/activities \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" -H "X-User-Role: ASSOCIATE" \
  -H "Content-Type: application/json" \
  -d "{\"title\": \"Audit backroom safety signage\", \"programmeId\": \"$PROG\", \"category\": \"COMPLIANCE\", \"priority\": \"HIGH\"}" \
  | jq -r '.id')

# 4. Move it through its lifecycle
curl -s -X PATCH http://localhost:8080/api/activities/$ACT \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" -d '{"status": "IN_PROGRESS"}'

curl -s -X PATCH http://localhost:8080/api/activities/$ACT \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1" \
  -H "Content-Type: application/json" -d '{"status": "DONE"}'

# 5. Confirm it shows up filtered by programme and status
curl -s "http://localhost:8080/api/activities?programme=$PROG&status=DONE" \
  -H "X-User-Id: staff-3" -H "X-Store-Id: store-1"
```

`jq` is only used to pull `id` out of the JSON response — drop it and read the id manually if you
don't have `jq` installed.
