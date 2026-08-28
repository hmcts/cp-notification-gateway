# [NG-S01] Service scaffold and notification/jobs schema

## User story
As a **developer on cp-gov-uk-notify-gateway**,
I want **the service scaffolded from the HMCTS Spring Boot template with the `notification` table and
the co-located cp-task-manager `jobs` table migrated in one Flyway run**,
so that **every subsequent story has a buildable, correctly-migrated, house-style-conformant foundation
to build on**.

## Background
This is the walking-skeleton / enabler story for the whole service. It has no independent business
value to an external actor, but it is a genuine prerequisite for every other story: the ASB consumer,
the send/poll tasks, the query API, and the CI pipeline all assume the scaffold and schema exist.
FR-001 carries no dedicated AC of its own (requirements.md notes it is "exercised by the Milestone 1
harness"); it is verified transitively by every later story's tests passing against this foundation.

## Acceptance criteria
- [ ] AC-001: Given a clean database, when Flyway runs, then the `notification` table is created with
  columns `notification_id` (UUID PK), `notification_type` (TEXT, default `EMAIL`), `status` (TEXT, no
  CHECK), `send_to_address` (TEXT, nullable), `status_code` (INT), `error_message` (TEXT),
  `client_context` (TEXT, nullable), `result_queue` (TEXT, nullable — the inbound ASB `ReplyTo`
  persisted on ingest for terminal-hop result routing, FR-007/NG-S03; distinct from the Gov.Notify
  email `replyToAddress`), and non-null audit timestamps `created_at`/`updated_at`
  (`TIMESTAMP WITH TIME ZONE`) — and no eventstore/Liquibase objects are created.
- [ ] AC-001a: Given a clean database and the cp-task-manager library on the classpath, when the
  service starts, then its Flyway auto-config (`TaskManagerFlywayAutoConfiguration`,
  `taskmanager.schema.enabled`) merges `classpath:db/taskmanager` (`V1__create_jobs_table.sql`) into
  the single Flyway run and the `jobs` table is created in the same datasource — no manual
  `spring.flyway.locations` entry required — so a `notification` INSERT and a `jobs` enqueue can later
  commit in one local transaction.
- [ ] Scaffold-only check (FR-001, no dedicated AC): the service builds and starts from the
  `service-hmcts-crime-springboot-template` scaffold (Spring Boot 4.x, Java 25+, Gradle, actuator) with
  the required dependencies declared (ASB SDK, Blob SDK, azure-identity, cp-task-manager library,
  Flyway, Postgres driver, Gov.Notify client) and no build/deprecation warnings from the template.

## NFR links
- NFR-011 (Tech-stack conformance): Spring Boot 4.x, Java 25+, Gradle, Flyway, Postgres 16,
  `uk.gov.hmcts.cp.*`, scaffolded from the HMCTS template.
- NFR-013 (Coding in the open): public repo from day one, `hmcts` GitHub team ownership, Conventional
  Commits, no direct commits to `main`.
- NFR-010 (Atomicity): this story creates the schema precondition (co-located `jobs` table) that later
  enables the one-local-transaction ingest guarantee; the transactional behaviour itself is verified in
  NG-S02.

## Out of scope for this story
- Any business logic (ASB consumer, sender, tasks, query API) — later stories.
- Environment/production Postgres provisioning (Terraform/IaC, ops) — NG-S09 (FR-016).
- The integration test harness itself (Testcontainers/ASB emulator/Azurite/WireMock) — folded as a
  cross-cutting Definition-of-Done concern across NG-S02, NG-S03, NG-S10, NG-S11, NG-S12 (see
  `_index.md`), not owned by this story.

## Definition of done
- [ ] Code reviewed and approved.
- [ ] Repo scaffolded via the `springboot-service-from-template` skill; no hand-rolled
  build/Dockerfile/logback (per NFR-011 Hard rules).
- [ ] AC-001 and AC-001a covered by an automated Flyway/startup test (unit + integration).
- [ ] No critical or high Snyk findings introduced.
- [ ] Deployed to and verified on sandbox (local Testcontainers Postgres).
- [ ] Repo is public under an `hmcts`-owned GitHub team with admin, before first merge (NFR-013).

## Notes / open questions
- **FR/AC traceability:** FR-001 (no dedicated AC), FR-002 → AC-001, AC-001a.
- **Jira story ticket: [PEG-3372](https://tools.hmcts.net/jira/browse/PEG-3372)** — created and linked to
  epic PEG-3350 (labels `claude-generated`, `needs-review`).
- **ADR flag:** Stage 2 (`architecture-design.md`, Follow-ups) recommends drafting ADR
  *"Direct-to-DB + cp-task-manager replaces CQRS/Event-Sourcing for notification-notify"* — this
  service-wide pattern decision is foundational to this story and should be drafted alongside it
  (skill: `skills/adr-template.md`).
- No open questions blocking this story.
