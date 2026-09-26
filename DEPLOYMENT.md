# Deployment

## Target

**Cloud — AWS EC2**, running the application as a Docker container built from this repository's
[Dockerfile](Dockerfile) and started via [docker-compose.yml](docker-compose.yml). This satisfies
Section 3.4's deployment requirement via the cloud path rather than a purely local Docker run.

## Prerequisites

- A running EC2 instance (any Linux AMI with a package manager) reachable over the internet.
- Inbound security group rule allowing `TCP 8080` from the tester's IP/range — the application
  listens on `8080` (see `server.port` in `application.yml` and the `ports: 8080:8080` mapping in
  `docker-compose.yml`); nothing sits in front of it as a reverse proxy for this deployment.
- `git` and Docker Engine with the Compose plugin installed on the instance.

## Steps taken

1. Launched an EC2 instance and opened inbound `TCP 8080` on its security group.
2. SSH'd in and installed `git` and Docker (Docker Engine + the `docker compose` plugin).
3. Cloned this repository onto the instance:
   ```bash
   git clone <repository-url>
   cd storeops-harness-java
   ```
4. Built and started the container:
   ```bash
   docker compose up --build
   ```
   This runs the same multi-stage `Dockerfile` used for local Docker — the build stage runs
   `./mvnw verify` (checkstyle, compile, unit + ArchUnit tests, JaCoCo coverage check) before the
   runtime image is even produced, so a build that failed any harness gate would never have
   reached this point.
5. Verified the deployed application by exercising the harness-generated
   `PATCH /api/activities/bulk-status` endpoint from outside AWS, using Postman against the
   instance's public DNS name on port 8080.

## Evidence

**Request** — `PATCH http://ec2-184-195-60-39.compute-1.amazonaws.com:8080/api/activities/bulk-status`,
body containing four activity updates (two to `DONE`, two to `BLOCKED`):

![Bulk update request](demo/screenshots/Bulk%20Update%20Input%20Request%20prefilled.png)

**Response** — `200 OK`, 85 ms, 606 B — all four items succeeded with their requested status:

![Bulk update response](demo/screenshots/Bulk%20Update%20Success%20Output.png)

```json
{
  "results": [
    { "id": "0783fa50-0272-4b2e-8372-934139645528", "succeeded": true, "status": "DONE" },
    { "id": "7aab9ac0-d362-468c-a4af-7d59d7ac728d", "succeeded": true, "status": "BLOCKED" },
    { "id": "74223ac5-749f-4eda-bae5-6ef0583a6c81", "succeeded": true, "status": "DONE" },
    { "id": "9f2d22a0-98f1-468d-8db5-f56cac16efdb", "succeeded": true, "status": "BLOCKED" }
  ]
}
```

This confirms the sprint's demonstration feature ([sprint-1-contract.md](.harness/reviews/sprint-1-contract.md), AC-4 — all-success batch) working end-to-end against the deployed instance, not just locally.

## Notes and follow-ups

- This EC2 + `docker compose` path was chosen for simplicity over the ECS Fargate task
  definition also drafted in this repo ([deploy/ecs-task-definition.json](deploy/ecs-task-definition.json))
  — it gets the container running fastest, at the cost of ECS's task-level health checks and
  auto-restart-on-failure. Worth a look if this needs to run unattended for longer than a demo
  session.
- The instance's public IP/DNS changes if it's stopped and restarted (EC2's default behavior
  unless an Elastic IP is attached) — not an issue for this one-time demonstration, but worth
  knowing before relying on the same URL again later.
