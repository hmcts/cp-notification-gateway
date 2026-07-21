# ADR 001 — Eager Testcontainers startup as a session phase

**Status:** Accepted

## Context

The integration and acceptance suites share Testcontainers (Postgres, MSSQL, the Azure
Service Bus emulator, Azurite) and a WireMock server, held in `static` fields and started
lazily from `static {}` blocks in `integration/stubs/support/*`. They start once per JVM, on
first reference — so the startup cost lands on whichever test class first touches a container
and inflates that one class's timing in `build/reports/tests`, making per-class numbers
misleading as the suite grows.

## Decision

Start the containers in an explicit bootstrap phase via a JUnit Platform
`LauncherSessionListener` (`EagerContainerStartupListener`, registered through
`META-INF/services`). On `launcherSessionOpened` — before test discovery — it force-initialises
the support classes, so their startup is attributed to session setup rather than a test class.

It is **gated** on the `cp.test.eagerContainers` system property, set only on the Gradle
`integrationTest` task. The listener eager-starts only when the property is present; otherwise
it no-ops and the existing lazy `static`-init fallback applies. The listener also records its
elapsed startup to `build/container-startup.seconds`, which `testTimingReport` surfaces as a
`containers` line.

## Consequences

- Container startup shows as its own `containers` line in the timing report, no longer charged
  to a test class. This is a **reporting/attribution** improvement — wall-clock is unchanged.
- `./gradlew build` / `check` / `integrationTest` set the flag on the task, so eager startup
  applies automatically (including transitively via `build`).
- An IntelliJ single-test run sets no flag, so it keeps today's lazy on-demand behaviour and
  only starts the containers that test needs — no regression.
- **Constraint:** IT and AT must stay in the single `integrationTest` task / one JVM. Splitting
  them into separate `Test` tasks forks separate JVMs, and the `static` containers live per
  JVM — they would restart per task, breaking the start-once/shared-container model. (Unit
  tests stay in their own `test` task, which needs no containers.)

## Alternatives considered

- **Testcontainers reuse** (`withReuse(true)` + `testcontainers.reuse.enable=true`): rejected —
  reuse disables Ryuk, leaving stale containers/volumes/ports, so teardown becomes unreliable
  for this suite.
- **Unconditional listener** (no flag): rejected — it would fire for IntelliJ single-test runs
  too (IntelliJ uses the JUnit Platform launcher), spinning up all containers for a test that
  needs one.
