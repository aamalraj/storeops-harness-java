# Harness Design Brief — StoreOps Shift Handover Bulk Update

## Section A — Intent Decomposition

### From feature prompt to sprint contract

The feature prompt (`PROMPT.md`) asked for one thing: a bulk endpoint that transitions multiple
activities to `DONE`/`BLOCKED` in one call, with partial-failure handling and an audit trail. The
Planner (`.harness/agents/planner.agent.md`) decomposed this into exactly **one sprint**, not
several, because `sprint-decomposition/SKILL.md`'s sizing rule ties sprint boundaries to module
write-paths: this feature touches only the `activities` module's write path (routes → service →
repository) and reuses the existing `ACTIVITY_UPDATED` event type rather than introducing a new
one. Had the feature required a new event type *and* a new subscriber behavior in `alerts`, the
sizing rule would have forced a second sprint — cross-module write paths are deliberately not
allowed to share a sprint, so each half can be independently evaluated and, if necessary, retried
without re-running the other module's already-passing code.

### Making acceptance criteria testable, not subjective

Each of the five ACs in `sprint-1-contract.md` follows GIVEN/WHEN/THEN and names a concrete,
checkable outcome — an HTTP status, a specific `AppError` code, an exact count of published
events — rather than a subjective standard. This mattered in practice: AC-1's requirement that
"exactly two `ACTIVITY_UPDATED` events are published" is precise enough that the Evaluator could
give a definitive PASS/FAIL rather than a judgment call, and precise enough that when the
Generator's first test draft used the wrong counting strategy (absolute count instead of delta),
the AC itself made it obvious the test was wrong, not the feature.

### Example sprint contract entry, in full

```
### AC-1: Mixed-outcome bulk update reports per-item results, not an all-or-nothing status

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

This single AC drove both the service implementation (`ActivityService.applyBulkStatusItem`'s
per-item try/catch) and its corresponding test (`ActivityRoutesTest.bulkUpdateReportsMixedOutcomes`)
directly — nothing in the implementation needed interpretation beyond what the AC already stated.

## Section B — Governance Framework

### Skill file strategy

| File | Shared by | What it encodes that is StoreOps-specific |
|---|---|---|
| `app-context` | All 4 agents | The actual module map, entity fields, existing endpoints, event catalogue — not "REST API conventions" in general |
| `architecture-principles` | All 4 agents | The five numbered rules mapped 1:1 to the client's four prior failure modes (Section 2), each naming the exact ArchUnit rule or checkstyle regexp that enforces it |
| `coding-conventions`, `how-to-test` | Generator | StoreOps' actual record/DTO patterns, the exact `Enums.parse` idiom, the exact JaCoCo thresholds per layer |
| `how-to-review`, `evaluation-criteria` | Evaluator | The three weighted dimensions, their hard gates, and the fallback rule for ambiguous LLM judgment calls |
| `sprint-decomposition` | Planner | The module-scoped sizing rule and the GIVEN/WHEN/THEN testability bar |

`app-context` and `architecture-principles` are shared across every agent deliberately: the
Planner needs to know the module boundaries to avoid proposing a sprint that crosses one, the
Generator needs them to avoid violating one, and the Evaluator needs them to check for violations
— splitting this into per-agent copies would risk drift between what each agent believes the
rules are.

### The `.harness/reviews/` audit trail

Each sprint's `spec.md`, `sprint-N-contract.md`, `generator-summary.md`, `evaluator-feedback.md`,
and `run-log.md` are committed to `.harness/reviews/` (working files in `.harness/output/` are
gitignored — only the final, decided artifacts are permanent). Anyone with repository access —
not just the developer who ran the sprint — can read the full chain: what was asked for, what was
built, what verdict it received and why, and what the Monitor flagged for trend purposes. A
recurring quality issue surfaces exactly the way `sprint-1-run-log.md` already demonstrates for
this run: the Monitor tags a category ("indirect event verification") even on a clean PASS: if a
later sprint's run-log tags the same category again, that is the concrete signal — visible to
anyone reading `.harness/reviews/` in sequence, no dashboard required — that the relevant skill
file needs a clearer example or a new hard gate, not that the Evaluator is being inconsistent.

### One skill file rule, and what breaks without it

`architecture-principles/SKILL.md`, Rule 3: *"If a change in one module should cause a side
effect visible from another module... that side effect is raised by calling
`eventBus.publish(...)` — never by the publishing module calling into the other module's service
or repository directly."* Without this rule, `ActivityService.bulkUpdateStatus` would have the
same option a prior AI-generated attempt took (Section 2, failure mode 4): call
`AlertService`/`NotificationService` directly to raise the audit trail entry. That would make
`activities` depend on `alerts`' internal implementation — a change to how alerts are stored or
delivered would then force a change in every module that raises one, exactly the tight coupling
`ModuleBoundaryTest.ALERTS_IS_REACHED_ONLY_VIA_THE_EVENT_BUS` exists to make structurally
impossible, not just discouraged.

## Section C — Non-Determinism Strategy

### Dimensions and weights

`evaluation-criteria/SKILL.md` defines three dimensions: **Architecture compliance (45%)**,
**Correctness & test quality (35%)**, **Code quality (20%)**. Architecture compliance carries the
largest weight because three of the four client-flagged failure modes are architecture
violations, not test or style issues — a harness that scored architecture lightly would let back
in the exact problems that justified building it. Correctness & test quality is weighted second
because the fourth failure mode (tests asserting status codes without business-rule checks) is a
distinct testing-discipline problem that needs its own dimension, so a Generator can't compensate
for weak tests with clean architecture or vice versa.

### Hard gates and why each cannot be a soft check

| Hard gate | Failure mode it prevents | Why not a soft check |
|---|---|---|
| `ModuleBoundaryTest` (9 ArchUnit rules) via `mvn test` | Cross-module repository imports, direct alerts/staff access | An LLM reviewer under context pressure can rate a borderline import "acceptable" — this is exactly what already happened once (Section 2); a compiled rule cannot be talked out of failing |
| Raw-throwable Checkstyle regexp | Raw `Error`/`RuntimeException` bypassing the `AppError` contract | `throw new IllegalStateException(...)` is idiomatic Java in general, wrong for this codebase specifically — needs a mechanical, un-arguable gate, not a style preference |
| `mvn test` (non-ArchUnit failures) + JaCoCo via `mvn verify` | Untested or under-tested business logic reaching `main` | A test suite either passes or it doesn't; there is no meaningful "soft" version of "most of the tests pass" for a harness whose purpose is shipping without manual review |
| `mvn checkstyle:check` (all rules) | Style/consistency drift | Lowest weight of the four, but still binary and free to check — a score-only treatment would let small violations always net out to an acceptable average |

### From variable Generator output to a definitive verdict — this sprint's actual run

Sprint 1 is the concrete example, not a hypothetical: all three hard gates held (checkstyle 0
violations, `mvn test` 63/63 including all `ModuleBoundaryTest` rules, JaCoCo thresholds met), so
the Evaluator moved to weighted scoring. Each dimension's checklist came back 4/4 (see
`sprint-1-evaluator-feedback.md`) — including a spot-check where the Evaluator independently
re-ran `mvn test` against the Generator's *first* draft (absolute alert count) and confirmed it
actually failed, then re-ran it against the fixed version (delta count) and confirmed it passed —
before accepting the Generator's own "Known gaps" narrative as accurate rather than trusting it
blindly. `score = 0.45×100 + 0.35×100 + 0.20×100 = 100` → **PASS**, deterministically: any other
run producing the same three hard-gate results and the same four checklist answers per dimension
would produce the same 100 and the same PASS, regardless of which LLM session evaluated it.

### Escalation path

Per `CLAUDE.md` Section 4a, escalation triggers the moment a sprint's **3rd** Generator/Evaluator
iteration still returns `VERDICT: FAIL`. It did not trigger this sprint — PASS came on iteration
1 — so the path below is documented, not yet empirically exercised (see `REFLECTION.md`). On
trigger, the orchestrator writes `.harness/output/escalation.md` naming the sprint, the iteration
count (always 3), the specific blocking check(s) with file/line detail from the last
`evaluator-feedback.md`, and a one-line recommendation when one is obvious; runs the Monitor with
`escalation flag: true`; and stops, handing control back to the developer — the harness does not
attempt a 4th automated iteration under any circumstance.

## Section D — Architectural Decisions

### Decision 1: Reuse `ACTIVITY_UPDATED` instead of adding a new event type

**Alternatives considered:** add a dedicated `BULK_STATUS_CHANGED` event type so bulk-originated
updates are distinguishable from single-item ones in the event log.
**Rationale:** the `alerts` module's only consumer (`AlertEventSubscriber`) treats every
`ACTIVITY_UPDATED` identically regardless of origin, and the sprint's own sizing rule
(`sprint-decomposition/SKILL.md`) explicitly discourages introducing new event surface unless a
consumer needs to react differently — no consumer here does.
**Assumption this depends on:** if a future feature needs to distinguish "updated via bulk
handover" from "updated one at a time" in a downstream consumer, this decision would need
revisiting; nothing in the current `alerts` behavior requires that distinction today.

### Decision 2: Verify event publication indirectly, via the `alerts` side effect

**Alternatives considered:** build a test-scoped `EventBus` double (a recording stub) so
`bulkUpdateReportsMixedOutcomes` could assert on `EventBus.publish` calls directly.
**Rationale:** no existing test in this codebase spies on `EventBus` — introducing that
infrastructure for one sprint would be new test-architecture, not just a new test, and the
indirect check (counting `alerts` records tied to a specific `sourceAggregateId`) is a genuine,
working verification, not a placeholder.
**Assumption this depends on:** `AlertEventSubscriber`'s current behavior — turning every
`ACTIVITY_UPDATED` into exactly one alert — remains true. If a future sprint changes that
subscriber (e.g., to deduplicate or batch alerts), this test would need to change even though the
bulk-update feature itself didn't. Flagged explicitly in `REFLECTION.md` as the first thing to
revisit before a second event-bus-dependent sprint.

### Decision 3: Deploy to EC2 + `docker compose` rather than the drafted ECS Fargate task

**Alternatives considered:** register the ECS Fargate task definition already drafted in
`deploy/ecs-task-definition.json` and run the demonstration there instead.
**Rationale:** EC2 + `docker compose up --build` was the fastest path to a publicly reachable URL
for this one-time demonstration — clone the repo, one command, done — using the exact same
`Dockerfile` (and therefore the exact same `mvn verify` gate) either deployment path would use.
**Assumption this depends on:** this is a short-lived demo instance, not something that needs
unattended reliability. ECS's task-level health checks and auto-restart-on-failure (see
`deploy/README.md`) matter once this needs to run without anyone watching it — the task
definition is kept in the repo specifically so that path is a config change, not a rewrite, when
that assumption stops holding.
