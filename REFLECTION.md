# Reflection — Sprint 1 Demonstration Run

## What the harness did well

The Generator/Evaluator loop passed on the first iteration, with a weighted score of 100%
across all three evaluation dimensions (`sprint-1-evaluator-feedback.md`), and every automated
hard gate — checkstyle, `mvn test` (including all 9 `ModuleBoundaryTest` ArchUnit rules), and
JaCoCo coverage — passed without a single manual fix. That is the harness working as designed:
the skill files (`architecture-principles`, `coding-conventions`, `how-to-test`) gave the
Generator enough concrete, StoreOps-specific context that it never reached for a raw throwable,
never imported across a module boundary, and never wrote a test that only checked an HTTP status
code — the exact three failure modes from the client's prior experiment (Section 2 of the
capstone spec) that motivated this whole exercise.

The `generator-summary.md` self-check also earned its place: the Generator caught its own test
bug (an alert-count assertion using an absolute count instead of a delta, which failed against
noise from its own test fixtures) and documented the fix honestly in "Known gaps" *before* the
Evaluator ever ran — exactly the self-correction the harness is meant to encourage, not something
imposed from outside.

## Where it fell short

Two real limitations, both already flagged in `sprint-1-run-log.md`:

1. **The escalation path is untested.** This sprint passed on iteration 1, so the 3-iteration
   retry loop and the `.harness/output/escalation.md` handoff — the part of `CLAUDE.md` most
   directly answering the "Non-Deterministic Systems Design" competency — never actually ran.
   Everything about it is designed, nothing about it is empirically verified.
2. **Event-bus publication is verified indirectly.** `bulkUpdateReportsMixedOutcomes` proves
   `ACTIVITY_UPDATED` was published only for successful items by counting the `alerts` module's
   downstream side effect, not by asserting on `EventBus.publish` directly — because no test in
   this codebase has a test double for `EventBus` (see Design Brief Section D, decision 2). It's
   a real, working check, but a future change to `AlertEventSubscriber` could silently break the
   proxy without the underlying bug being what actually failed.

## One concrete improvement

Add a test-scoped `EventBus` double (a `@Primary` `@TestConfiguration` bean that records published
events, swapped in only for tests that need it) and update `how-to-test/SKILL.md` to prefer it
over the alerts-side-effect proxy for any future sprint that touches event publication. This
directly revisits the decision documented in Design Brief Section D ("indirect event-publication
verification") — accepting the trade-off was the right call to ship this sprint without inventing
new test infrastructure, but it's the first thing I'd change before a second sprint adds another
event-bus-dependent feature, rather than letting the indirection compound.
