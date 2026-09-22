# Skill: architecture-principles

Purpose: the non-negotiable StoreOps rules. Every rule here maps to one of the four failure modes
the client's standards team flagged in a prior AI-generated-code experiment (direct cross-module
repository imports, raw `Error` throws, tests that check HTTP status but not business rules, and
notifications written directly to sibling repositories instead of via the event bus). This file
is shared by all four agents — Planner, Generator, and Evaluator must all reason about the same
rules.

## Rule 1 — Layer separation: Routes → Service → Repository, never skipped or reversed

- `web` (routes) classes only bind HTTP input, call exactly one service method, and map the
  result to a DTO. No business logic, no validation beyond `@Valid` bean annotations, no direct
  repository access.
- `service` classes hold all business logic, authorisation checks, and are the only layer allowed
  to throw `AppError` subclasses or call `EventBus.publish(...)`.
- `repository` interfaces/implementations do data access only. They never call another service,
  never perform validation, never construct an `AppError`.
- Enforced automatically by `ModuleBoundaryTest` (`ROUTES_ARE_NOT_DEPENDED_ON`,
  `SERVICES_DO_NOT_DEPEND_ON_ROUTES`, `REPOSITORIES_ARE_REACHED_THROUGH_SERVICES`,
  `DOMAIN_DOES_NOT_DEPEND_ON_LAYERS`).

## Rule 2 — Module boundary: no cross-module repository or service-internals imports

- A module may depend on another module's **published port** only:
  - `staff.api.StaffDirectory` — the only sanctioned way to read staff data. No module outside
    `com.storeops.staff` may import `staff.service`, `staff.repository`, or `staff.web`.
  - A target module's own `@Service` class, for a read-only lookup (e.g. `reports` calling
    `ActivityService.countByStatus`) — never that module's `repository` package directly.
- `alerts` is reached by **no other module, ever** — not even its service class. The only
  sanctioned path in is `EventBus.publish(...)`; `alerts.service.AlertEventSubscriber` is the
  sole subscriber.
- Enforced automatically by `ModuleBoundaryTest`
  (`ALERTS_IS_REACHED_ONLY_VIA_THE_EVENT_BUS`, `STAFF_IS_READ_ONLY_FOR_OTHER_MODULES`,
  `STAFF_PORT_EXPOSES_ONLY_QUERIES`, `MODULES_ARE_ACYCLIC`).

## Rule 3 — Event bus only for cross-module side effects

- If a change in one module should cause a side effect visible from another module (an alert, a
  report refresh), that side effect is raised by calling
  `eventBus.publish(DomainEvent.of(TYPE, entityId, storeId, actorId, payload))` — never by the
  publishing module calling into the other module's service or repository directly.
- New side effects require a new `DomainEventType` enum value (in `common.events`) plus a
  subscriber registration in the consuming module's `@PostConstruct register()` method (see
  `AlertEventSubscriber` for the pattern) — not a new direct call.
- What breaks without this rule: the publishing module (e.g. `activities`) would need to depend
  on the consuming module's internals (`alerts.service.AlertService`), recreating exactly the
  tight coupling `ModuleBoundaryTest.ALERTS_IS_REACHED_ONLY_VIA_THE_EVENT_BUS` exists to prevent —
  a change to how alerts are stored would then force a change in every publisher.

## Rule 4 — Error contract: only `AppError` subclasses, never a raw throwable

- Services and routes must never write `throw new RuntimeException(...)`,
  `throw new IllegalStateException(...)`, `throw new IllegalArgumentException(...)`, or any raw
  `Exception`/`Error`/`Throwable`.
- Use an existing subclass of `com.storeops.common.error.AppError`: `NotFoundError`,
  `ValidationError`, `ConflictError`, `ForbiddenError`, `UnauthorizedError`,
  `MethodNotAllowedError`. If none fits, add a new `AppError` subclass with its own `code` and
  `statusCode` — do not fall back to a raw throwable to avoid adding one.
- Enforced automatically two ways: a Checkstyle `RegexpSingleline` rule bans the raw-throw pattern
  at lint time, and `ModuleBoundaryTest.SERVICES_AND_ROUTES_RAISE_ONLY_APP_ERRORS` bans
  constructing a non-`AppError` throwable at test time. A raw throw fails the build twice over —
  there is no path for it to reach `main` undetected.
- `ApiExceptionHandler` is the single place that renders any escaped exception as an HTTP
  response; do not add another `@ExceptionHandler` class or a per-route try/catch that returns a
  response body directly.

## Rule 5 — `reports` is read-only

- `reports.service.ReportService` (and anything added to the `reports` module) may call other
  modules' query methods but must never call a mutating method (`create`, `update`, `delete`,
  `save`, `add...`) on any other module's service, and must never construct or persist another
  module's domain entity.
- If a new report needs to trigger a side effect (e.g. a `REGIONAL_ROLLUP` record), that is done
  by `reports` publishing its own domain event via `EventBus`, not by writing into another
  module's repository.

## What the Evaluator checks automatically vs. by judgement

| Rule | Automated check | LLM-assessed check |
|---|---|---|
| Layer separation | `ModuleBoundaryTest` (4 ArchUnit rules) via `mvn test` | Generator summary's declared layer per file matches actual file location |
| Module boundary | `ModuleBoundaryTest` (4 ArchUnit rules) via `mvn test` | — |
| Event bus only | — (ArchUnit can prove *no import exists*, not *why* an event was published) | Evaluator reads the diff for any new cross-module call and confirms it is a service/EventBus call, not a repository import |
| Error contract | Checkstyle regexp (`mvn checkstyle:check`) + `ModuleBoundaryTest` via `mvn test` | — |
| Read-only reports | — | Evaluator scans `reports` module diff for any call to a non-query method on another module's service |

See [evaluation-criteria/SKILL.md](../evaluation-criteria/SKILL.md) for how these checks combine
into a verdict.
