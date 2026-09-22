# StoreOps Development Harness — Orchestrator

This file is read automatically by Claude Code when it starts in this repository. It defines how
a feature request becomes governed, tested, standards-compliant code in `src/`, without a human
reviewing every intermediate step.

## 1. Entry point

A developer starts a harness run with a single prompt of the form:

```
@planner <feature description>
```

Example: `@planner Add shift handover bulk update — PATCH /api/activities/bulk-status`

On seeing `@planner`, read [.harness/agents/planner.agent.md](.harness/agents/planner.agent.md) and
act as the Planner described there. Do not start generating code yet.

## 2. Agents

Four agents exist, each defined in its own file. Read the referenced agent file in full before
acting as that agent — do not improvise a role from this summary alone.

| Agent | File | Invoked by |
|---|---|---|
| Planner | [.harness/agents/planner.agent.md](.harness/agents/planner.agent.md) | The developer's `@planner` prompt |
| Generator | [.harness/agents/generator.agent.md](.harness/agents/generator.agent.md) | The orchestrator, once a sprint contract exists |
| Evaluator | [.harness/agents/evaluator.agent.md](.harness/agents/evaluator.agent.md) | The orchestrator, after every Generator pass |
| Monitor | [.harness/agents/monitor.agent.md](.harness/agents/monitor.agent.md) | The orchestrator, after every sprint reaches a final verdict |

## 3. Run sequence

1. **Planner** reads the feature prompt and writes `.harness/output/spec.md` plus one
   `.harness/output/sprint-N-contract.md` per sprint. `spec.md` ends with the line
   `STATUS: AWAITING APPROVAL`.
2. **Orchestrator stops and waits.** Do not proceed past this point on your own. Print a short
   summary of `spec.md` and ask the developer to review it.
3. The developer either requests changes (loop back to step 1 with the Planner) or types
   `APPROVED`.
4. Once `APPROVED` is received, the **Generator/Evaluator loop runs autonomously**, one sprint
   contract at a time, in the order the Planner listed them:
   - Generator implements the current sprint contract, writes code under `src/`, and writes
     `.harness/output/generator-summary.md`.
   - Evaluator reviews the diff against
     [.harness/skills/architecture-principles/SKILL.md](.harness/skills/architecture-principles/SKILL.md)
     and [.harness/skills/evaluation-criteria/SKILL.md](.harness/skills/evaluation-criteria/SKILL.md),
     runs the automated checks in Section 5 below, and writes
     `.harness/output/evaluator-feedback.md` with a verdict of `PASS`, `CONDITIONAL PASS`, or
     `FAIL`.
   - The orchestrator reads the verdict (see Section 4) and decides the next action.
   - **Monitor** runs after every sprint reaches a final verdict (PASS, CONDITIONAL PASS accepted
     as final, or escalation) and archives the sprint's artefacts to `.harness/reviews/`.
5. The loop ends when every sprint contract has a final verdict, or an escalation is raised.

## 4. Routing logic

Read `evaluator-feedback.md`'s `VERDICT:` line after every Evaluator pass:

- **`VERDICT: PASS`** → run the Monitor for this sprint, then advance to the next sprint
  contract's Generator pass (or, if this was the last sprint, end the run and report completion
  to the developer).
- **`VERDICT: CONDITIONAL PASS`** → treat as PASS for the purpose of advancing, but the Monitor
  must flag it in `run-log.md` as a quality-trend signal (see
  [.harness/agents/monitor.agent.md](.harness/agents/monitor.agent.md)) — a recurring CONDITIONAL
  PASS on the same check is a candidate for a skill file fix.
- **`VERDICT: FAIL`** → increment this sprint's iteration counter. If the counter is now ≤ 3,
  send `evaluator-feedback.md` back to the Generator as its input for the next attempt (this is
  iteration N+1 of the same sprint, not a new sprint). If the counter has reached **3 and the
  verdict is still FAIL**, do not attempt a 4th iteration — escalate (Section 4a).

### 4a. Escalation

Escalation is triggered the moment a sprint's 3rd Generator/Evaluator iteration still returns
`VERDICT: FAIL`. The orchestrator:

1. Writes `.harness/output/escalation.md` containing: the sprint ID, the iteration count (3), the
   specific blocking check(s) from the last `evaluator-feedback.md` (file, line, rule violated),
   and a one-line recommendation if one is obvious (e.g. "the Generator repeatedly imports
   `alerts.service` directly — consider adding an explicit EventBus example to
   `coding-conventions/SKILL.md`").
2. Runs the Monitor for this sprint with `escalation flag: true`.
3. Stops the loop and hands control back to the developer. Do not attempt further Generator
   iterations on this sprint without new developer input.

Three iterations is a hard limit, not a target — most sprints should PASS on iteration 1 or 2 if
the skill files are doing their job.

## 5. Automated checks (run by the Evaluator, not simulated)

The Evaluator must actually run these commands and read their exit codes / output — it must not
guess at whether they would pass:

```
mvn checkstyle:check      # lint — 0 violations
mvn compile                # javac -Xlint:all -Werror — 0 warnings/errors
mvn test                   # unit + ArchUnit module-boundary tests — BUILD SUCCESS
mvn verify                 # runs jacoco:check on top of test — coverage thresholds enforced
```

`mvn verify` fails the build if line coverage drops below the thresholds configured in `pom.xml`
(service 80%, web/route 70%, common 60%, project-wide 70% — see Section 3.6 of the capstone spec).
A failing `mvn verify` is an automatic hard-gate `FAIL`; see
[.harness/skills/evaluation-criteria/SKILL.md](.harness/skills/evaluation-criteria/SKILL.md).

## 6. CI/CD relationship

`.harness/` is intentionally separate from `.github/` (see repository root). The harness's
automated checks are the **same commands** the CI pipeline would run (`mvn checkstyle:check`,
`mvn test`, `mvn verify`) — the harness does not replace CI, it runs those checks earlier and adds
an LLM-assessed layer (architecture intent, event-bus usage, layer separation) that a plain CI
pipeline cannot evaluate. A change that passes the harness should also pass CI; a change that
fails CI after passing the harness indicates a harness/CI drift that should be fixed by aligning
the Evaluator's automated-check invocations with the CI job definition, not by loosening either
one.

## 7. Context scoping strategy

Each agent invocation is a fresh context, not a continuation of the previous agent's conversation:

- The **Planner** reads only `app-context`, `architecture-principles`, and
  `sprint-decomposition`, plus the developer's feature prompt. It never reads generated code.
- The **Generator** reads only `app-context`, `architecture-principles`, `coding-conventions`,
  `how-to-test`, and the *current* sprint contract (not the full spec, not prior sprints' contracts,
  not prior evaluator feedback unless this is a retry — see below). This keeps its context
  proportional to one sprint, not the whole feature.
- On a **retry** (iteration 2 or 3 of the same sprint), the Generator additionally reads the
  single most recent `evaluator-feedback.md` — not the full history of earlier iterations. Each
  retry is scoped to "current code + latest feedback", so context does not grow with iteration
  count.
- The **Evaluator** reads only `architecture-principles`, `how-to-review`,
  `evaluation-criteria`, the sprint contract being evaluated, and `generator-summary.md` — it does
  not read the Planner's reasoning or earlier sprints' feedback.
- The **Monitor** reads only the just-completed sprint's `generator-summary.md` and
  `evaluator-feedback.md` — it does not re-read code.

This means a 5-sprint feature does not accumulate a 5x context window by sprint 5: each agent's
input size is bounded by "one sprint contract + its own skill files", regardless of how many
sprints came before.
