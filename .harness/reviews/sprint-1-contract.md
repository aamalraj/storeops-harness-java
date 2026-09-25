# Sprint 1 Contract — Shift Handover Bulk Update

## Sprint goal

Add `PATCH /api/activities/bulk-status`, letting a caller close out or flag multiple activities
(`DONE` or `BLOCKED` only) in one request, with per-item partial-failure reporting and one
`ACTIVITY_UPDATED` event published per item that succeeds.

## Scope

| File | Layer | Change |
|---|---|---|
| `src/main/java/com/storeops/activities/dto/BulkStatusUpdateRequest.java` | dto | New. Request body: list of `{id, status}` items. |
| `src/main/java/com/storeops/activities/dto/BulkStatusUpdateResponse.java` | dto | New. Response body: list of per-item outcomes (`id`, success flag, error `code`/`message` on failure). |
| `src/main/java/com/storeops/activities/service/ActivityService.java` | service | Add `bulkUpdateStatus(Actor actor, BulkStatusUpdateRequest request)`. Processes each item independently; catches the per-item `AppError`, records the outcome, continues to the next item — never lets one failure abort the batch. Publishes `ACTIVITY_UPDATED` only for items that succeed. |
| `src/main/java/com/storeops/activities/web/ActivityRoutes.java` | web | Add `@PatchMapping("/bulk-status")` binding only — no business logic. Must be mapped **before** `@PatchMapping("/{id}")` is reachable for the literal path `/bulk-status` (Spring's path matching already handles this correctly since `/bulk-status` is not a valid UUID-shaped `{id}`, but the route method itself must still live in this file per module layout). |
| `src/test/java/com/storeops/activities/web/ActivityRoutesTest.java` | test | Add tests for: all-succeed, all-fail, and mixed-outcome batches (per `how-to-test/SKILL.md`'s partial-failure testing rule), plus an invalid-target-status case and a cross-store id case. |

## Acceptance criteria

### AC-1: Mixed-outcome bulk update reports per-item results, not an all-or-nothing status

```
GIVEN outgoing shift staff hold activities "A1" (BLOCKED, this store), "A2" (IN_PROGRESS, this
store), and "A3" (DONE, this store, already closed)
WHEN they PATCH /api/activities/bulk-status with
  {"updates": [{"id": "A1", "status": "DONE"}, {"id": "A2", "status": "BLOCKED"},
               {"id": "A3", "status": "DONE"}]}
THEN the response is 200 OK with:
  - A1: succeeded, status DONE
  - A2: succeeded, status BLOCKED
  - A3: failed, AppError code CONFLICT ("Activity 'A3' is DONE and cannot be modified")
AND exactly two ACTIVITY_UPDATED events are published (for A1 and A2, not A3)
AND no exception propagates past the route — the response is 200, not 409, because the request
    as a whole is a partial success
```

### AC-2: Only DONE or BLOCKED are valid target statuses for this endpoint

```
GIVEN an activity "A4" (IN_PROGRESS, this store)
WHEN a PATCH /api/activities/bulk-status request includes {"id": "A4", "status": "IN_PROGRESS"}
THEN that item's outcome is failed, AppError code VALIDATION_FAILED, message naming that only
     DONE or BLOCKED are accepted by this endpoint
AND no ACTIVITY_UPDATED event is published for A4
```

### AC-3: An id from another store is reported as not-found, not leaked as a different error

```
GIVEN the caller is at store-1 and activity "A5" belongs to store-2
WHEN a PATCH /api/activities/bulk-status request includes {"id": "A5", "status": "DONE"}
THEN that item's outcome is failed, AppError code NOT_FOUND
AND no ACTIVITY_UPDATED event is published for A5
```

### AC-4: An all-success batch publishes one event per item and returns 200

```
GIVEN activities "A6" and "A7" (both BLOCKED, this store)
WHEN a PATCH /api/activities/bulk-status request marks both DONE
THEN the response is 200 OK with both items succeeded, status DONE
AND exactly two ACTIVITY_UPDATED events are published, one per activity
```

### AC-5: An empty items list is rejected before any item is processed

```
GIVEN any caller
WHEN they PATCH /api/activities/bulk-status with {"updates": []}
THEN the response is 400 VALIDATION_FAILED naming that at least one update must be supplied
AND no ACTIVITY_UPDATED event is published
```

## Out of scope (see spec.md)

- No new `DomainEventType`.
- No `priority`/`category`/`assigneeId` changes via this endpoint.
- No change to `PATCH /api/activities/{id}`.
- No request size limit / pagination.

## Evaluator focus for this sprint

Per `evaluation-criteria/SKILL.md`: automated hard gates (`mvn checkstyle:check`, `mvn test`
including `ModuleBoundaryTest`, `mvn verify` JaCoCo thresholds) apply as usual. LLM-assessed
checks to pay particular attention to for this sprint: partial-failure handling is genuinely
per-item (not a try/catch around the whole loop that silently drops later items on the first
failure), and the event-bus publish call sits inside the per-item success path, not outside the
loop or unconditional.
