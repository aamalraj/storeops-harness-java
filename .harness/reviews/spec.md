# Feature Spec — Shift Handover Bulk Update

## Feature prompt (verbatim, see PROMPT.md)

> Add shift handover bulk update — PATCH /api/activities/bulk-status allowing outgoing shift
> staff to mark multiple activities as DONE or BLOCKED in a single request, with partial failure
> handling and an audit trail per updated activity (an ACTIVITY_UPDATED event published for each
> activity that succeeds, none for activities that fail).

## Goal

Let outgoing shift staff close out or flag a batch of activities in one call at end of shift,
instead of one `PATCH /api/activities/{id}` call per activity — while keeping every existing
single-item guarantee (store scoping, closed-activity conflict, typed errors) intact per item,
and without letting one bad item in the batch abort the rest.

## Affected module

`activities` only. No other module's write path is touched — this is a single-sprint feature per
`sprint-decomposition/SKILL.md`'s sizing rule ("one sprint = one module's write path").

## New endpoint

`PATCH /api/activities/bulk-status`

Request body: a list of `{id, status}` pairs. `status` is restricted to `DONE` or `BLOCKED` for
this endpoint — the prompt scopes it to shift-close actions, not general status transitions
(those remain available one-at-a-time via the existing `PATCH /api/activities/{id}`).

Response body: one outcome per requested item — `id`, whether it succeeded, and on failure the
`AppError` `code`/`message` that would have been thrown by the equivalent single-item update.
`200 OK` is returned even when some items fail, since the request as a whole is a partial
success/failure, not an all-or-nothing operation (this is the specific test-quality failure mode
— asserting only HTTP status, not business outcome — the harness exists to catch; see
`architecture-principles/SKILL.md`).

## Data model changes

None. Reuses `Activity`, `ActivityStatus`, and the existing `withStatus(...)` copy method. Two new
DTOs only: a bulk request shape and a bulk response shape (exact record layout is the Generator's
call within `coding-conventions/SKILL.md`'s guidance to model partial-failure responses as a typed
DTO wrapping per-item outcomes, not a boolean).

## Event bus interaction

Reuses the existing `ACTIVITY_UPDATED` `DomainEventType` — no new event type is introduced. One
`ACTIVITY_UPDATED` event is published per item that *succeeds*; items that fail publish nothing.
This mirrors `ActivityService.update`'s existing publish call, just invoked once per successful
item in the batch rather than once for a single request.

## Per-item failure modes (each must map to an existing `AppError`, not a raw throwable)

- Unknown id, or id belongs to a different store than the caller → `NotFoundError` (same rule
  `getById` already applies).
- Activity is already `DONE` or `CANCELLED` (closed) → `ConflictError` (same rule `update`
  already applies).
- Requested `status` is not `DONE` or `BLOCKED` → `ValidationError`, since this endpoint's scope
  is narrower than the general-purpose PATCH.

## Out of scope

- No new `DomainEventType` — reuses `ACTIVITY_UPDATED`.
- No change to `priority`, `category`, or `assigneeId` in the same call — bulk-status only
  changes `status`. Reassignment stays on the existing single-item `PATCH /api/activities/{id}`
  (see `SAMPLE_REQUESTS.md`'s reassignment example).
- No change to the existing single-item `PATCH /api/activities/{id}` endpoint's behavior or
  contract.
- No pagination or batch-size limit on the request list.

## Sprint decomposition

One sprint. See `sprint-1-contract.md` for the full acceptance criteria.

| Sprint | Scope |
|---|---|
| 1 | `PATCH /api/activities/bulk-status`: DTOs, service method, route, tests |

STATUS: AWAITING APPROVAL
