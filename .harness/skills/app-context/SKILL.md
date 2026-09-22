# Skill: app-context

Purpose: orient any agent to what StoreOps actually is, before it reads architecture rules or
writes code. This file is shared by all four agents.

## What StoreOps is

A REST API for retail store operations management, built with Spring Boot 3 / Java 17, Maven,
in-memory storage (no database). Package root: `com.storeops`.

## Modules and what they own

| Module | Package | Owns | Key types |
|---|---|---|---|
| `activities` | `com.storeops.activities` | Operational tasks at a store | `Activity`, `ActivityStatus` (PLANNED, IN_PROGRESS, BLOCKED, DONE, CANCELLED), `ActivityPriority`, `ActivityCategory` |
| `programmes` | `com.storeops.programmes` | Store programmes and their staff membership | `Programme`, `ProgrammeMember`, `ProgrammeRole`, `ProgrammeStatus` |
| `staff` | `com.storeops.staff` | Staff identity — **read-only for every other module** | `StaffMember`; exposed only through the `StaffDirectory` port |
| `alerts` | `com.storeops.alerts` | In-app alerts triggered by domain events — **reached only via the EventBus, never by direct call** | `Alert`, `AlertSeverity` |
| `reports` | `com.storeops.reports` | Cross-module read aggregation — **never writes to any other module** | `StoreMetrics`, `RegionMetrics` |

Each module (except `reports`, which has no repository of its own domain, and `staff`'s narrow
port) follows the same internal layout:

```
<module>/
  domain/       records + enums (no framework dependency)
  dto/          request/response shapes for the web layer
  repository/   persistence port + in-memory implementation
  service/      business logic, the only layer allowed to throw AppError or use EventBus
  web/          @RestController classes — HTTP binding only
  api/          (activities, programmes, staff) the narrow query port other modules may call
```

## Cross-module access today

- `activities.service.ActivityService` depends on `staff.api.StaffDirectory` (read) and
  `common.events.EventBus` (publish).
- `alerts.service.AlertEventSubscriber` subscribes to `EventBus` events published by `activities`
  and `programmes` — it is the *only* class that knows both the event catalogue and the alerts
  domain.
- `reports` reads via each module's service-level query methods (e.g.
  `ActivityService.countByStatus(storeId)`, `ActivityService.countOpen(storeId)`) — not their
  repositories.

## Existing endpoints (do not duplicate; extend or add alongside)

| Method | Path | Module |
|---|---|---|
| GET | `/api/activities` | activities |
| POST | `/api/activities` | activities |
| GET | `/api/activities/{id}` | activities |
| PATCH | `/api/activities/{id}` | activities |
| DELETE | `/api/activities/{id}` | activities |
| GET | `/api/programmes` | programmes |
| POST | `/api/programmes` | programmes |
| POST | `/api/programmes/{id}/members` | programmes |
| GET | `/api/alerts` | alerts |

## Auth model

Every route resolves an `Actor` (`userId`, `storeId`, `regionId`, `role`) via the
`@AuthenticatedActor` argument resolver, backed by request headers (see
`common.auth.ActorArgumentResolver` and, in tests, `support.ActorHeaders`). `ActorRole` is
`ASSOCIATE < SUPERVISOR < STORE_MANAGER < REGION_MANAGER`; `role.isManager()` is true for
`STORE_MANAGER` and `REGION_MANAGER`. Services scope every query/mutation to `actor.storeId()`
unless a cross-store operation is explicitly part of the feature.

## Event catalogue

`common.events.DomainEventType`: `ACTIVITY_CREATED`, `ACTIVITY_UPDATED`, `ACTIVITY_DELETED`,
`PROGRAMME_CREATED`, `PROGRAMME_MEMBER_ADDED`. Adding a new cross-module side effect means adding
a new enum value here and a `publish(...)` call in the owning module's service — never a new
direct dependency on `alerts`.
