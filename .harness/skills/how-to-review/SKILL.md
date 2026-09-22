# Skill: how-to-review

Purpose: how to read a Generator diff and produce feedback specific enough that the Generator can
act on it without human clarification. This file describes *technique*; the actual pass/fail
rules and weights are in [evaluation-criteria/SKILL.md](../evaluation-criteria/SKILL.md).

## Order of operations

1. **Run the automated checks first** (`mvn checkstyle:check`, `mvn compile`, `mvn test`,
   `mvn verify`). Read their actual output — do not predict what they would say. A tool that fails
   tells you exactly which rule and where; use that, don't re-derive it from re-reading the diff.
2. **Read `generator-summary.md`'s files-changed list** before opening any file — it tells you
   which layer each file claims to be, so you can immediately spot a mismatch (e.g. a file in
   `web/` that the summary calls "service logic").
3. **Diff-review, not full-file review.** Only the changed lines (plus enough surrounding context
   to judge a call site) need scrutiny — StoreOps' unchanged code is already governed by prior
   sprints' Evaluator passes and the fixed `ModuleBoundaryTest` suite.

## What to look for beyond the automated checks

- **Event bus misuse that ArchUnit can't catch structurally**: a new method that publishes an
  event of the wrong `DomainEventType`, or that publishes *after* a failure path instead of only
  on success, or that omits a publish where the sprint contract's acceptance criteria required
  one. ArchUnit proves "no forbidden import exists" — it cannot prove "the right event was
  published at the right time." That is this checklist's job.
- **Read-only violation in `reports`**: scan for any call from `reports` into another module's
  service method whose name implies mutation (`create`, `update`, `delete`, `save`, `add`,
  `remove`). A read that happens to be named ambiguously should be checked against that module's
  actual implementation, not assumed safe from the name alone.
- **Layer separation that compiles but is still wrong**: business logic (multi-step conditionals,
  authorisation checks) written in a route method that happens to compile because it only calls
  service methods for persistence — check *what* logic runs in the route method body, not just
  which classes it calls.
- **Test quality, not just test presence**: open at least one new test per sprint and confirm it
  asserts a business-rule-specific field or error code, not just a status code (see
  `how-to-test/SKILL.md`'s "what a test must assert" section). A green `mvn test` with weak
  assertions is a pass on the automated gate and still a finding here.

## Citing feedback

Every finding in `evaluator-feedback.md` must include: file path, line number (or method name if
the file is large enough that a line number would drift), the specific rule violated (name it
exactly as in `architecture-principles/SKILL.md`, e.g. "Rule 3 — Event bus only"), and — for a
MUST FIX — the concrete change needed, not just the problem. "Move this check into
`ActivityService`" is actionable; "logic is misplaced" is not.

## Judgement calls and the ambiguous-result rule

Some checks require judgement (is this the "right" event type for this side effect?). When
genuinely unsure after reading the relevant code:

- Do not resolve the ambiguity in the Generator's favor by default.
- Mark the specific checklist item `FAIL` with the reason `AMBIGUOUS — resolved to FAIL per
  fallback rule`, and say plainly what would resolve the ambiguity (e.g. "sprint contract does not
  specify whether a bulk update should publish one event per item or one aggregate event — needs
  Planner clarification").
- This keeps the verdict deterministic given the same evidence: a genuinely ambiguous case always
  produces the same "FAIL, needs clarification" outcome rather than depending on assessor mood.
