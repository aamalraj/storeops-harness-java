# Skill: sprint-decomposition

Purpose: how the Planner turns one feature prompt into sprint contracts sized so the Generator
can plausibly pass evaluation within 3 iterations, with acceptance criteria specific enough that
the Evaluator's checklist items are unambiguous.

## Sizing a sprint

- One sprint = one module's write path, or one read-only cross-module feature (a new report), or
  one new event + its subscriber handling — not a combination of unrelated modules' write paths
  in the same sprint.
- If a feature naturally requires changes in two modules' service/repository layers (e.g. a new
  endpoint in `activities` that also needs `alerts` to react to it), split it into two sprints:
  the first adds the `activities` change and the event publish; the second adds the `alerts`
  subscriber handling for the new event type. This mirrors the actual module boundary — the two
  halves are connected only by the `DomainEventType` enum value, never a direct call, so they can
  genuinely be developed, evaluated, and if necessary retried independently.
- A sprint that only adds tests to existing code (no behavior change) is too small to need a full
  contract — fold it into the sprint that introduces the behavior.
- A sprint that touches more than ~5 files across more than 2 layers in a single module is a sign
  it should be split; smaller sprints get more reliable Evaluator verdicts and cheaper retries.

## Writing a GIVEN/WHEN/THEN acceptance criterion that is testable, not subjective

A criterion is testable when a reviewer with no other context could write a single test that
either clearly passes or clearly fails against it — no interpretation required.

**Weak (subjective) example:**
> The bulk update endpoint should handle errors gracefully.

**Strong (testable) example:**
> GIVEN a request to `PATCH /api/activities/bulk-status` containing 3 activity IDs, where one ID
> does not exist and the other two belong to the caller's store and are not yet DONE,
> WHEN the request is submitted with `{"status": "DONE"}`,
> THEN the response is `200 OK` with a body listing 2 succeeded updates (each with the new status
> and `updatedAt` timestamp) and 1 failed update (with `AppError` code `NOT_FOUND` and the missing
> ID), and an `ACTIVITY_UPDATED` event is published for each of the 2 succeeded activities only.

The strong version names: the exact endpoint and payload shape, the exact starting state, the
exact response shape (including which fields must be present), the exact error code for the
failure case, and the exact event-bus behavior — every clause maps directly to one line in
`evaluator-feedback.md`'s checklist if it's ever violated.

## Example sprint contract entry (for reference — see Design Brief Section A for the full one used
in the demonstration run)

```
### AC-1: Mixed-outcome bulk update reports per-item results

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

## Out-of-scope discipline

Every sprint contract must list at least one explicit out-of-scope item if the feature could
plausibly be over-implemented (e.g. "does not add a new `DomainEventType` for bulk updates; each
item reuses `ACTIVITY_UPDATED`" or "does not add pagination to the response — the base
`GET /api/activities` filters are unchanged"). This bounds the Generator's scope creep risk and
gives the Evaluator a concrete thing to check was *not* added.
