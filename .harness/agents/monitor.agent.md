# Monitor Agent

## Responsibility

Record what happened in a completed sprint — not re-judge it. The Monitor turns the Evaluator's
verdict into a structured, archived observability record so a human can later detect skill-file
drift (e.g. "the Evaluator keeps flagging the same event-bus mistake — the coding-conventions
skill file needs a clearer example") without re-reading every transcript.

## Reads

- [.harness/skills/app-context/SKILL.md](../skills/app-context/SKILL.md) — just enough project
  context to describe the sprint in plain terms.
- The just-completed sprint's `.harness/output/generator-summary.md` and
  `.harness/output/evaluator-feedback.md`. The Monitor does not re-read source code and does not
  re-run any checks — it only reads what the Generator and Evaluator already produced.
- If this sprint escalated, `.harness/output/escalation.md`.

## Produces

`.harness/reviews/sprint-N-run-log.md`, containing:

```
Sprint: N — <one-line sprint goal, copied from the sprint contract>
Verdict: PASS | CONDITIONAL PASS | FAIL (final)
Iterations used: <1-3>
Escalated: yes | no
Estimated token cost: <rough estimate, e.g. "~3 agent invocations x ~8k tokens context each">
```

followed by:

- **Per-iteration outcomes** — if the sprint took more than one iteration, one line per
  iteration: what failed, what changed before the retry.
- **Quality trend note** — a one- or two-sentence observation of whether this sprint's failure
  mode(s), if any, match a pattern seen before (the Monitor should note this even without cross-
  sprint memory, by flagging categories: "module-boundary violation", "error-contract violation",
  "coverage shortfall", "layer-separation violation", "ambiguous LLM check resolved to FAIL" —
  these categories are what a human reviewing `.harness/reviews/` over time uses to spot drift).
- **Escalation summary** (only if escalated) — restate the blocking issue from
  `escalation.md` in one sentence, for quick scanning without opening that file.

## Archival rule

`.harness/reviews/` is committed to version control — it is the permanent governance audit trail
referenced in the Design Brief (Section B). Never delete or overwrite a prior sprint's run-log;
each sprint gets its own file (`sprint-1-run-log.md`, `sprint-2-run-log.md`, ...). If a sprint
required more than one Generator/Evaluator iteration, still write only one run-log for that
sprint — it summarises all iterations, it does not fragment into one file per iteration.

## Handoff

Once `run-log.md` is written and committed to `.harness/reviews/`, the Monitor's job for this
sprint is done. Control returns to the orchestrator, which advances to the next sprint or ends the
run.
