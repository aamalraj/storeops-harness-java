# Skill: how-to-test

Purpose: how tests are written in this codebase, and what "done" means for coverage — so
Generator-written tests pass `mvn verify`'s JaCoCo gate on the first try, and actually assert
business rules rather than just HTTP status codes (one of the four failure modes this harness
exists to prevent).

## Test types and where they live

- **Route-level tests** — `@SpringBootTest @AutoConfigureMockMvc`, one test class per module's
  `web` controller, mirroring `src/main/java` under `src/test/java` (e.g.
  `activities/web/ActivityRoutesTest.java`). Drive requests through `MockMvc`, assert on
  `status()` **and** on response body fields via `jsonPath(...)` — see
  `ActivityRoutesTest.createsActivity` for the pattern of asserting both the status code and the
  actual field values that prove the business rule fired correctly.
- Use `com.storeops.support.ActorHeaders` to authenticate requests
  (`ActorHeaders.associate()`, `.manager()`, `.of(userId, storeId, role)`) instead of constructing
  headers inline — this keeps actor setup consistent and DRY across test files.
- `ModuleBoundaryTest` under `src/test/java/com/storeops/architecture/` is a fixed, shared
  ArchUnit suite — the Generator must never weaken, delete, or add `@Disabled` to any rule in it
  to make a sprint pass. If a legitimate new rule is needed for the sprint's feature, add a new
  `@ArchTest` rule; never touch the existing ones.

## What a test must assert (not just what it must run)

A test that only checks `status().isOk()` is not sufficient — it passed the "tests asserted HTTP
status but not business rule compliance" failure mode this harness is built to catch. Every test
for a new endpoint or behavior must also assert at least one of:

- A specific response field value that only a correct implementation would produce (e.g. a bulk
  update test must assert *which* items succeeded and *which* failed, not just that the endpoint
  returned 200).
- The correct `AppError` `code` in the error body for a failure case (`jsonPath("$.code")`), not
  just the HTTP status — two different `AppError` subclasses can share a status code.
- Where relevant, that a `DomainEvent` of the expected type was published (inject a test
  `EventBus` spy/stub, or assert the downstream effect it causes — e.g. that `alerts` service
  state reflects the expected side effect — for features that add a new cross-module trigger).

## Coverage thresholds (enforced by `mvn verify` → JaCoCo, see `pom.xml`)

| Scope (path pattern) | Minimum line coverage |
|---|---|
| `**/service/**` | 80% |
| `**/web/**` | 70% |
| `**/common/**` | 60% |
| Whole project | 70% |

`mvn verify` fails the build if any rule is unmet — this is a hard gate, not a suggestion (see
[evaluation-criteria/SKILL.md](../evaluation-criteria/SKILL.md)). New service methods, in
particular, need direct or route-level test coverage of every branch that changes behavior
(success path, each validation failure, each `AppError` case) to keep the service-layer bar met.

## Partial-failure and multi-item operations

For any endpoint that operates on multiple items in one request (e.g. a bulk status update),
write at least three tests: all-succeed, all-fail, and mixed (some succeed, some fail) — a mixed
case is the one most likely to be missing and is exactly the case the "no partial failure
handling" failure mode targets.
