# Skill: evaluation-criteria

Purpose: the authoritative definition of how a Generator pass becomes `PASS` / `CONDITIONAL PASS`
/ `FAIL`. Three weighted dimensions, each with a hard gate. Hard gates are deterministic tool
results (checkstyle, ArchUnit via `mvn test`, JaCoCo via `mvn verify`) wherever possible, so the
verdict does not depend on LLM assessor mood on repeat runs of the same code.

## Dimensions

| Dimension | Weight | Hard gate |
|---|---|---|
| 1. Architecture compliance | 45% | `mvn test` reports zero failures in `com.storeops.architecture.ModuleBoundaryTest`, **and** `mvn checkstyle:check` reports zero violations of the raw-throwable regexp rule |
| 2. Correctness & test quality | 35% | `mvn test` reports zero failures outside `ModuleBoundaryTest` (i.e. all unit/route tests pass), **and** `mvn verify` reports JaCoCo coverage at or above every threshold in `pom.xml` |
| 3. Code quality | 20% | `mvn checkstyle:check` reports zero violations of any rule (including, but not limited to, the raw-throwable rule already covered by Dimension 1) |

Weights sum to 100%.

### Why these three, for StoreOps specifically

Architecture compliance carries the largest weight because three of the four client-flagged
failure modes (cross-module repository imports, raw `Error` throws, direct writes bypassing the
event bus) are architecture violations, not code-quality or coverage issues — a harness that
scores architecture lightly would let the exact problems that justified building it back in.
Correctness & test quality carries the second-largest weight because the fourth failure mode
(tests asserting status codes without business-rule checks) is a testing discipline problem, not
an architecture one, and needs its own weighted dimension so a Generator can't compensate for
weak tests with clean architecture or vice versa. Code quality is real but lower-stakes here —
Checkstyle failures are cosmetic/consistency issues, not the kind of defect that reaches
production and causes an incident.

## Why each hard gate cannot be a soft check

- **ArchUnit module-boundary failure (Dim 1)**: a soft check ("try to avoid cross-module
  imports") is exactly what already failed in the client's prior AI-coding experiment — an LLM
  reviewer under token pressure or mid-context can rate a borderline import as "acceptable". The
  ArchUnit suite is compiled, not prompted; it cannot be talked out of failing.
- **Raw-throwable Checkstyle violation (Dim 1)**: same reasoning — this is the single most common
  failure mode in AI-generated Java for this kind of project (reaching for
  `throw new IllegalStateException(...)` is idiomatic Java, but wrong for this codebase's error
  contract), so it needs a mechanical, un-arguable gate.
- **Test failure / coverage shortfall (Dim 2)**: a test suite either passes or it doesn't, and
  coverage either meets the threshold or it doesn't — there is no meaningful "soft" version of
  "most of the tests pass" for a governed harness whose whole purpose is that Generator output
  ships without manual review.
- **Checkstyle violation (Dim 3)**: deliberately the *lowest*-weighted hard gate, but still a hard
  gate rather than folded into a score, because "no violations" is binary and free to check — a
  score-only treatment would let an accumulating pile of small violations always net out to an
  acceptable weighted average, which defeats having a linter at all.

## Dimension 1 — Architecture compliance (45%) checklist

Hard gate (see above) must hold, or this dimension scores 0 and the sprint is an automatic `FAIL`
regardless of Dimensions 2–3.

Beyond the hard gate, score the following (each binary, equal weight within the dimension):

- [ ] Every new cross-module side effect is raised via `EventBus.publish`, never a direct
  service/repository call into another module (LLM-assessed — see `how-to-review/SKILL.md`).
- [ ] No module outside `staff` reaches `staff.service`, `staff.repository`, or `staff.web` (also
  covered by ArchUnit, checked again here as a diff-level sanity check for new code specifically).
- [ ] `reports` module changes (if any) contain no call to a mutating method on another module's
  service (LLM-assessed).
- [ ] Route methods in the diff contain no business logic or authorisation checks — those live in
  the service layer (LLM-assessed).

## Dimension 2 — Correctness & test quality (35%) checklist

Hard gate must hold, or this dimension scores 0.

- [ ] Every acceptance criterion in the sprint contract has at least one test that would fail if
  the feature were reverted (not just a test that happens to pass).
- [ ] At least one test per new/changed endpoint asserts a specific response field value or
  `AppError` code, not only an HTTP status code.
- [ ] Multi-item/bulk endpoints have a mixed-outcome (partial success/failure) test.
- [ ] `generator-summary.md`'s AC self-check table is accurate — spot-check at least one row
  against the actual test/code; a self-check that claims PASS on a criterion with no
  corresponding test is a finding against this checklist item, independent of whether the
  criterion happens to be met by accident.

## Dimension 3 — Code quality (20%) checklist

Hard gate must hold, or this dimension scores 0.

- [ ] New domain records have Javadoc with `@param` per field (see `coding-conventions`).
- [ ] New enum values parsed via `Enums.parse`/`Enums.parseOptional`, never `Enum.valueOf`
  directly in a service.
- [ ] Constructor injection only, no field `@Autowired`.
- [ ] Naming matches existing module conventions (no abbreviations not already in the codebase).

## Verdict rule (deterministic given the same check results)

1. Any hard gate (Dim 1, 2, or 3) fails → **`FAIL`**. No further scoring needed; report the
   specific tool output that triggered it.
2. All hard gates hold. Compute `score = 0.45 × dim1 + 0.35 × dim2 + 0.20 × dim3`, where each
   dimension's score is `(checklist items passed) / (checklist items total)`, expressed as a
   percentage.
   - `score ≥ 90` → **`PASS`**
   - `75 ≤ score < 90` → **`CONDITIONAL PASS`** — advance to the next sprint, but the Monitor must
     record which checklist items failed as a quality-trend note (see `monitor.agent.md`); two
     `CONDITIONAL PASS` verdicts in a row on the same checklist item across different sprints is a
     signal the relevant skill file needs a clearer example, not that the bar should be lowered.
   - `score < 75` → **`FAIL`**.

### Worked example (variable Generator output → definitive verdict)

Suppose a sprint's Generator output: passes `mvn checkstyle:check` and `mvn test` (including
`ModuleBoundaryTest`) and `mvn verify` (coverage thresholds met) — both hard gates in Dim 1 and
Dim 2 hold, and the Dim 3 hard gate holds. Dimension 1 checklist: 3 of 4 items pass (the new
endpoint correctly avoids cross-module imports and keeps logic in the service layer, but the
Evaluator judges one new event as publishing the wrong `DomainEventType` for the described side
effect) → Dim 1 = 75%. Dimension 2 checklist: all 4 items pass → Dim 2 = 100%. Dimension 3: 3 of 4
pass (naming is inconsistent in one new class) → Dim 3 = 75%.

`score = 0.45×75 + 0.35×100 + 0.20×75 = 33.75 + 35 + 15 = 83.75` → **`CONDITIONAL PASS`**. The
sprint advances, but `evaluator-feedback.md` names the wrong-event-type finding and the naming
inconsistency as MUST FIX items for whichever future sprint touches that code again, and the
Monitor logs "event-type selection" as a quality-trend flag.

## Fallback rule for ambiguous LLM-assessed checks

If, after reading the relevant code, the Evaluator genuinely cannot determine whether an
LLM-assessed checklist item passes (see `how-to-review/SKILL.md`'s "judgement calls" section),
that item is scored as **failed**, with the reason recorded as ambiguous rather than as a
concrete violation. This keeps the same evidence always producing the same score — an ambiguous
result never silently resolves to a pass.
