# Skill: coding-conventions

Purpose: StoreOps-specific style and structure so Generator output looks like it was written by
the same team that wrote the rest of the codebase, and passes Checkstyle/javac on the first try.

## Domain and DTO records

- Domain types are `record`s in `<module>/domain/`, immutable, with `with*` copy methods for any
  field that changes over the entity's lifecycle (see `Activity.withStatus`,
  `Activity.withPriority`). Never add a setter or a mutable field.
- Every domain record gets a Javadoc block with a one-sentence class summary and an `@param` line
  per field — see `Activity` for the pattern. This is required, not optional decoration:
  reviewers and future agents rely on it to understand a field's meaning without reading every
  call site.
- Request/response DTOs live in `<module>/dto/`, are also records, and use `From`/`from(...)`
  static factory methods to map a domain record to a response DTO (see `ActivityResponse.from`).
  Request DTOs validate with `jakarta.validation` annotations (`@NotBlank`, etc.) — validation
  messages should be specific enough to surface directly in `ErrorResponse.details`.

## Enums

- Enum values are `UPPER_SNAKE_CASE`, no numeric ordinal reliance anywhere in business logic.
- Parse an enum from a request string using `common.util.Enums.parse(...)` (required, throws
  `ValidationError` naming the valid options) or `Enums.parseOptional(...)` (returns
  `Optional.empty()` for null/blank input) — never call `Enum.valueOf(...)` directly in a service,
  since that throws a raw `IllegalArgumentException` and violates the error contract.

## Services

- Constructor-inject every collaborator (`repository`, any cross-module port, `EventBus`) —
  no field injection, no `@Autowired` on fields.
- Every mutating method that changes state visible to another module ends with a `publish(...)`
  call to `EventBus`, using a small private `publish(...)` helper (see `ActivityService.publish`)
  rather than repeating the `DomainEvent.of(...)` construction inline in every method.
- Ownership/authorisation checks (`actor.owns(...)`, `actor.role().isManager()`) happen in the
  service method, before the mutation, and raise `ForbiddenError` — never in the route, never
  after the repository call.
- Partial-failure operations (e.g. a bulk update over a list of IDs) must not let one bad ID abort
  the whole batch silently or corrupt state; process each item independently, collect
  per-item outcomes, and return a response that reports success/failure per ID rather than a
  single all-or-nothing status. Model this as a small response DTO (e.g. `BulkUpdateResult`
  wrapping a list of per-item outcomes), not a boolean.

## Routes

- One `@RestController` per module, `@RequestMapping("/api/<module>")` at the class level, method
  names matching HTTP semantics (`list`, `create`, `getById`, `update`, `delete`) — see
  `ActivityRoutes` for the pattern.
- `@AuthenticatedActor Actor actor` is always the first parameter.
- Route Javadoc is a one-line `@GetMapping`/`@PostMapping`/etc. summary in the format
  `"<METHOD> <path> — <what it does>"` — matches the existing style so `generator-summary.md`'s
  file list is self-explanatory.

## Naming

- Package-private where possible; only `@RestController`, `@Service`, `@Component`, and public
  interfaces/records that cross a module boundary need to be `public`.
- No abbreviations that aren't already in the codebase (`Programme` not `Prog`, `Repository` not
  `Repo` in type names — `repo` as a local variable name is fine).

## Checkstyle and javac gates the Generator must satisfy without being told twice

- No trailing whitespace, file must end with a newline, no tabs, lines ≤120 chars.
- No star imports, no unused imports.
- `-Xlint:all -Werror` is active — an unchecked/deprecation warning fails the build exactly like
  an error. Do not suppress with `@SuppressWarnings` unless the warning is a genuine false
  positive; if so, comment why.
