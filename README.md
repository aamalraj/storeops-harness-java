# StoreOps REST API

Retail store operations management API — Java 17, Spring Boot 3.3, Maven.

This is a **structural stub**: every module, layer, route, error type and boundary rule is in place
and exercised by tests, but the business logic inside the services is deliberately minimal. Storage
is in-memory; there is no database.

## Running it

A Maven wrapper is committed, so no local Maven install is needed:

```bash
./mvnw verify          # lint + type check + tests
./mvnw spring-boot:run # serves on http://localhost:8080
```

`./mvnw verify` runs, in order: Checkstyle (`validate`), `javac -Xlint:all -Werror` (`compile`),
then JUnit 5 + MockMvc and the ArchUnit boundary rules (`test`). Any lint violation, compiler
warning or boundary breach fails the build.

## Authentication

There is no login: an upstream gateway is assumed to have authenticated the caller and to forward
identity headers, which `ActorArgumentResolver` turns into an `Actor`. A request without them is
rejected with 401 before the route body runs.

| Header        | Required | Notes                                                        |
| ------------- | -------- | ------------------------------------------------------------ |
| `X-User-Id`   | yes      | staff id, used for ownership checks                          |
| `X-Store-Id`  | yes      | scopes every query and mutation                             |
| `X-Region-Id` | no       | used by region reports                                       |
| `X-User-Role` | no       | `ASSOCIATE` (default), `SUPERVISOR`, `STORE_MANAGER`, `REGION_MANAGER` |

Seeded staff (`InMemoryStaffRepository`): `staff-1` store manager, `staff-2` supervisor and
`staff-3` associate at `store-1`; `staff-4` associate at `store-2`.

```bash
curl -s localhost:8080/api/activities \
  -H 'X-User-Id: staff-3' -H 'X-Store-Id: store-1' -H 'X-User-Role: ASSOCIATE'
```

## Endpoints

The nine endpoints from the specification:

| Method + Path                       | Module     | Description                                        |
| ----------------------------------- | ---------- | -------------------------------------------------- |
| `GET /api/activities`               | activities | List activities (optional `programme`, `status`)   |
| `POST /api/activities`              | activities | Create a new activity                              |
| `GET /api/activities/{id}`          | activities | Get activity by id                                 |
| `PATCH /api/activities/{id}`        | activities | Update status, priority, category, assignee        |
| `DELETE /api/activities/{id}`       | activities | Delete an activity (owner or store manager only)   |
| `GET /api/programmes`               | programmes | List programmes for the authenticated store        |
| `POST /api/programmes`              | programmes | Create a new programme                            |
| `POST /api/programmes/{id}/members` | programmes | Add a staff member to a programme                  |
| `GET /api/alerts`                   | alerts     | Get alerts for the authenticated user              |

The specification asks for five modules each having a Routes → Service → Repository stack, but lists
no endpoint for staff or reports. Rather than ship two modules with an empty route layer, each has
been given the obvious read route. **These three are additions beyond the specified nine** and are
marked as such in their Javadoc, so they are easy to drop:

| Method + Path                | Module  | Description                               |
| ---------------------------- | ------- | ----------------------------------------- |
| `GET /api/staff`             | staff   | List staff for the authenticated store    |
| `GET /api/reports/store`     | reports | Metrics for a store                       |
| `GET /api/reports/region`    | reports | Roll-up across a region (managers only)   |

## Layout

```
com.storeops
├── common/            shared infrastructure, depended on by every module
│   ├── auth/          Actor, ActorRole, @AuthenticatedActor, ActorArgumentResolver
│   ├── error/         AppError hierarchy + ApiExceptionHandler
│   ├── events/        EventBus, DomainEvent, DomainEventType
│   ├── util/          Enums (string → enum with typed 400s)
│   └── web/           WebConfig
├── activities/        api · domain · dto · repository · service · web
├── programmes/        api · domain · dto · repository · service · web
├── staff/             api · domain · dto · repository · service · web
├── alerts/            domain · dto · repository · service · web
└── reports/           domain · repository · service · web
```

Per module: `web` (Routes) → `service` → `repository`, with `domain`/`dto` as data and `api` holding
the read-only ports other modules are allowed to use.

## Module boundary rules

The four rules are enforced by `ModuleBoundaryTest` (ArchUnit), so a breach fails `./mvnw test`
instead of needing a reviewer to notice it. Each rule was verified by deliberately breaking it.

**No circular imports.** `slices().should().beFreeOfCycles()` over `com.storeops.*`. The dependency
graph is a DAG: `reports → {activities.api, programmes.api}`, `{activities, programmes} → staff.api`,
everything → `common`, and `alerts → common` only.

**Notifications via event bus only.** No class outside `com.storeops.alerts` may reference it at
all. Activities and programmes publish a `DomainEvent`; `AlertEventSubscriber` picks it up and
creates the alert. `AlertRoutesTest` proves the wiring end to end — nothing in it touches the alerts
module, so an alert can only appear if the bus delivered the event.

**Staff is read-only for other modules.** Outside callers get `StaffDirectory`, which has query
methods only; `staff.service`, `staff.repository` and `staff.web` are off limits, and a further rule
asserts the port itself never grows a mutating method.

**Layering.** Routes are never depended on, services never import routes, and repositories are only
reached from their own module's service layer.

Note that the layer patterns are written as `com.storeops.*.web..`, not `..web..` — a bare `..web..`
also matches `org.springframework.web`, which makes the layering rule fail on every route class.

## Error handling

`AppError` is the abstract base: a stable `code`, a `message` and the `statusCode` to answer with.
Subclasses are `ValidationError` (400), `UnauthorizedError` (401), `ForbiddenError` (403),
`NotFoundError` (404), `MethodNotAllowedError` (405) and `ConflictError` (409).

Services and routes never throw a raw throwable. That is enforced twice: a Checkstyle regex rejects
`throw new RuntimeException(...)` and friends anywhere in the source, and an ArchUnit rule fails any
class in a service or route package that *constructs* a throwable outside the `AppError` hierarchy.

`ApiExceptionHandler` renders every failure in one shape, and folds framework faults into it so they
cannot surface as a 500 — malformed JSON and unbindable parameters become 400, an unmapped path 404,
a wrong verb 405. The catch-all logs the exception and returns an opaque `INTERNAL_ERROR`; no stack
trace or internal message ever reaches the caller.

```json
{
  "code": "NOT_FOUND",
  "message": "Activity 'abc' was not found",
  "status": 404,
  "details": [],
  "path": "/api/activities/abc",
  "timestamp": "2026-09-21T10:15:30.123Z"
}
```

Enum-valued request fields are taken as strings and parsed in the service, so an unknown value gives
a 400 naming the accepted values rather than a Jackson failure.

## Tests

56 tests: MockMvc route tests per module (happy path, validation, scope, ownership, conflict), the
error-contract suite, a context smoke test asserting all five modules contribute three layers, and
the nine ArchUnit boundary rules.

In-memory repositories are shared across tests in a Spring context, so tests are written to be
order-independent: they assert on their own records via unique ids rather than on global counts.

## Known stubs

Called out so they are not mistaken for finished work:

- **Storage** is `ConcurrentHashMap`; nothing survives a restart.
- **Authentication** trusts request headers. Replacing `ActorArgumentResolver` is the only change
  needed — no route or service reads a header directly.
- **Event delivery** is synchronous and in-process; a failing subscriber is logged and skipped.
- **Business logic** is minimal by design: status transitions are unconstrained beyond a
  closed-record check, alert routing and severity are placeholders, and report figures are
  recomputed per request with no caching.
- **Region membership** in `InMemoryStoreDirectoryRepository` is a hardcoded single region.
