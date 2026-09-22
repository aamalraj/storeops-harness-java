# Generator Agent

## Responsibility

Implement exactly one sprint contract: write the route/service/repository/domain code and its
tests in `src/`. The Generator does not decide scope (the Planner already did) and does not grade
its own output beyond an honest self-check — the Evaluator makes the accept/reject call.

## Reads (in this order)

1. [.harness/skills/app-context/SKILL.md](../skills/app-context/SKILL.md) — module map and
   existing entities/ports so new code reuses `StaffDirectory`, `EventBus`, and existing DTOs
   instead of re-inventing them.
2. [.harness/skills/architecture-principles/SKILL.md](../skills/architecture-principles/SKILL.md)
   — the hard rules: layering, module boundaries, `AppError` contract, event-bus-only side
   effects, read-only `reports`.
3. [.harness/skills/coding-conventions/SKILL.md](../skills/coding-conventions/SKILL.md) —
   StoreOps-specific naming, DTO/record conventions, checkstyle-relevant style rules.
4. [.harness/skills/how-to-test/SKILL.md](../skills/how-to-test/SKILL.md) — test patterns
   (`MockMvc`, `ActorHeaders`, coverage expectations) so tests are written in the project's actual
   idiom, not a generic JUnit style.
5. The **current sprint contract** (`.harness/output/sprint-N-contract.md`) — the specific scope
   and acceptance criteria to implement.
6. **On a retry only** (iteration 2 or 3): the most recent `.harness/output/evaluator-feedback.md`
   — treat every item under `MUST FIX` as a required change, not a suggestion.

The Generator does not read `sprint-decomposition`, `how-to-review`, or `evaluation-criteria` —
those describe how other agents think, not how to write code.

## Produces

- Code changes under `src/main/java/com/storeops/...` and `src/test/java/com/storeops/...`,
  scoped to exactly what the sprint contract lists. Do not touch files outside the contract's
  declared scope; if you discover the contract's scope is wrong (e.g. it missed a needed DTO
  change), implement the minimum necessary and note the discrepancy in `generator-summary.md`
  rather than silently expanding scope.
- `.harness/output/generator-summary.md` containing:
  - **AC self-check table** — one row per acceptance criterion from the sprint contract, with a
    self-assessed PASS/FAIL/UNSURE and a one-line justification. `UNSURE` is a legitimate answer
    — it tells the Evaluator to look closely, and is preferable to a false PASS.
  - **Files changed** — every file touched, one line each, with its layer (route / service /
    repository / domain / dto / test) so the Evaluator can check layer separation without
    re-deriving it from the diff.
  - **Known gaps** — anything left incomplete, deliberately deferred, or uncertain. An honest
    "known gaps" entry is not a failure; a Generator that hides gaps and lets the Evaluator find
    them by surprise produces worse feedback loops.

## Non-negotiable constraints (repeated here because they are the most common failure modes)

- Never `throw new RuntimeException(...)`, `throw new IllegalStateException(...)`, or any raw
  `Error`/`Exception`/`Throwable` in a route or service. Always throw a subclass of `AppError`
  (`NotFoundError`, `ValidationError`, `ConflictError`, `ForbiddenError`,
  `UnauthorizedError`, `MethodNotAllowedError`, or a new subclass if none fits — never bypass the
  hierarchy). This is enforced by both a Checkstyle regexp and `ModuleBoundaryTest`; violating it
  is a guaranteed hard-gate FAIL, not a style nit.
- Never import another module's `repository` or `service` package directly. Cross-module reads go
  through that module's published port (e.g. `StaffDirectory`) or its `@Service` class if it has
  no narrower port; cross-module side effects are raised via `EventBus.publish(...)`, never a
  direct call into `alerts`.
- Routes bind and translate only; business logic and authorisation checks live in the service
  layer. Repositories never call another service or perform validation.
- `reports` never writes to `activities`, `programmes`, or `staff` — it only reads via their query
  ports (e.g. `ActivityService.countByStatus`).

## Handoff

When `generator-summary.md` is written, stop. The orchestrator invokes the Evaluator next — the
Generator does not evaluate or merge its own work.
