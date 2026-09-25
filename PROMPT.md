# Demonstration Run — Feature Prompt

This is the exact prompt used to invoke the harness for the demonstration run required by
Section 5.5 / 6.3 of the capstone spec. It was typed as-is at the Claude Code prompt in this
repository, with `CLAUDE.md` present at the repo root.

```
@planner Add shift handover bulk update — PATCH /api/activities/bulk-status allowing outgoing
shift staff to mark multiple activities as DONE or BLOCKED in a single request, with partial
failure handling and an audit trail per updated activity (an ACTIVITY_UPDATED event published for
each activity that succeeds, none for activities that fail).
```

## Why this feature

Chosen from the suggested features in Section 3.4 of the capstone spec. It is scoped to a single
module (`activities`), so it can be demonstrated as one sprint without needing a second sprint for
a cross-module event subscriber — while still exercising every architecture rule the harness is
built to enforce:

- **Layer separation** — new logic lives in `ActivityService`, not `ActivityRoutes`.
- **Error contract** — per-item failures use existing `AppError` subclasses (`NotFoundError`,
  `ConflictError`), never a raw throwable.
- **Event bus only** — each successful update publishes `ACTIVITY_UPDATED` via `EventBus`, the
  same path `ActivityService.update` already uses; no new coupling to `alerts`.
- **Partial failure handling** — the exact test-quality failure mode (Section 2) this harness
  exists to catch: a response that reports per-item success/failure rather than an all-or-nothing
  status.

See `.harness/output/spec.md` and `.harness/output/sprint-1-contract.md` for the Planner's
decomposition of this prompt, and `.harness/reviews/` for the archived Generator/Evaluator/Monitor
output from the run.
