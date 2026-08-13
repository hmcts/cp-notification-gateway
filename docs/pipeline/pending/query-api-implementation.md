# Query-API implementation approach — options and decision (NG-S04 / FR-009)

**Status:** ✅ **Decision made — Option B (pragmatic interim), no ADR** (per coordinator instruction).
Recorded here in `docs/pipeline/pending/` alongside the auth decision
([`query-api-authentication.md`](./query-api-authentication.md)). This document governs *how* the read
(query) REST API added in NG-S04 is built; it does **not** cover authentication (NG-S14).

## 1. The tension

The HMCTS Modern-by-Default standard is **OpenAPI contract-first via a companion `api-*` repo**,
confirmed against the estate:

- **`cp-court-list-publishing-service`** (reference impl): the spec is a *separate* published artifact
  (`uk.gov.hmcts.cp:api-cp-crime-court-list-publisher:0.3.2`), consumed as a dependency; the controller
  **implements the generated interface** — `class CourtListPublishController implements
  CourtListPublishApi`, methods `@Override` the generated `uk.gov.hmcts.cp.openapi.*` types, with **no
  hand-written `@GetMapping`**. springdoc is deliberately absent (Boot 4 compatibility).
- **`service-hmcts-crime-springboot-template`** (canonical template): ships the contract-first
  convention as a `validateApiSpecVersions` gate over a `configurations.apiSpec` dependency, enforced in
  `ci-released.yml`. Contract and implementation live in **separate repos**.
- **`cp-notification-gateway`** is already committed to this pattern in its own design docs
  (`architecture-design.md`, `NG-S04.md` — "API-first, per Q6" → companion `api-notification-gateway`
  repo). The `apispec-validation.gradle` + `ci-released.yml` gate is pre-seeded from the template.

**But the repo is greenfield for the read API**: no query controller, no `apiSpec` dependency wired into
`build.gradle`, and **no `api-notification-gateway` repo exists yet**. The NG-S04 A-TDD test suite that
was just authored assumes a **hand-written** `NotificationQueryController` and a **local
`NotificationView` DTO** — which diverges from the contract-first standard (where the controller would
`implements <generated NotificationApi>` and use generated model DTOs).

## 2. Options

### Option A — Contract-first now (standard-correct)
Create the `api-notification-gateway` OpenAPI spec repo first (spec-before-service, via
`springboot-api-from-template`), wire `apiSpec` into `build.gradle`, then rework the NG-S04 web-layer
tests to target the generated interface + generated models.
- **Pros:** fully aligned with the HMCTS standard and the already-made Q6 decision; no rework debt; the
  `validateApiSpecVersions` gate is satisfied honestly.
- **Cons:** largest up-front effort; blocks NG-S04 implementation on standing up a second repo and
  publishing its first artifact; more moving parts before any endpoint code exists.

### Option B — Hand-written controller as a pragmatic interim (CHOSEN)
Keep the current hand-written-controller test scaffolding; implement `NotificationQueryController` with
its own `@GetMapping`s and a local `NotificationView` DTO. Defer the companion `api-notification-gateway`
spec + generated-interface migration to a follow-up.
- **Pros:** unblocks NG-S04 immediately; no dependency on a not-yet-existing repo/artifact; the existing
  A-TDD suite stands unchanged.
- **Cons:** deviates from the API-first standard and the Q6 decision; creates migration debt (the
  controller must later be re-pointed at the generated interface, and the local DTO retired); the
  `apiSpec` gate remains unwired.

### Option C — Generated-interface tests without the repo yet
Fix the tests to `implements` a generated interface now, assuming the `api-notification-gateway` spec is
authored alongside implementation.
- **Pros:** tests pin the standard-correct contract shape from day one.
- **Cons:** half-measure — the generated interface/artifact does not exist, so the build cannot resolve
  it; effectively forces Option A's repo work anyway, without its clean sequencing.

## 3. Decision

**Option B is chosen.** NG-S04 will ship the read API with a hand-written controller and a local view
DTO, keeping the existing A-TDD suite. **No ADR is raised** for this deviation (explicit coordinator
instruction).

### Migration debt to track (follow-up, not blocking NG-S04)
- Stand up `api-notification-gateway` (OpenAPI spec, per Q6) and publish its first artifact.
- Re-point `NotificationQueryController` at the generated `NotificationApi` interface; retire the local
  `NotificationView` in favour of the generated model DTO.
- Wire `apiSpec` into `build.gradle` so `validateApiSpecVersions` (already in `ci-released.yml`) is
  satisfied.

## 4. Security posture (interim) — decided separately

NG-S04 now ships the endpoint **open (permit-all, unauthenticated)** as an interim until NG-S14 delivers
authentication (a deny-all endpoint could not be exercised/verified on sandbox and delivered no value).
This is implemented as an **explicit `permitAll()` security configuration marked interim-until-NG-S14**
(not a silent absence of security), so NG-S14's swap to real auth is a one-file change. The
OFFICIAL-SENSITIVE PII exposure this creates is a **consciously accepted interim risk**, valid only
while the endpoint stays behind the trusted network boundary and NG-S14 precedes any external exposure
(full rationale + mitigation in `NG-S04.md` and `query-api-authentication.md`). Consequence for tests:
**no deny-all/denial test belongs to NG-S04** — that behaviour is NG-S14's; NG-S04's tests exercise the
served (open) journey.

## 5. BDD scope note

Because the served query journey is now deployable in NG-S04 (open interim), a **positive happy-path
acceptance scenario** for the query API belongs here straightforwardly (an operator retrieves a
notification record and finds it by status/date). The auth/deny scenario belongs to NG-S14.

## 6. Contract & validation (test-time + runtime)

The query API's contract is an **authored OpenAPI spec kept in-repo at
`contracts/openapi/notification-gateway.openapi.yaml`** (no code generation — the controller is
hand-written per §3). It may be published to clients later, at which point it seeds the
`api-notification-gateway` repo. Two enforcement layers keep the hand-written controller honest until
runtime spec-based validation is adopted:

- **Test-time conformance** — the controller's real requests/responses are validated against the spec
  with `com.atlassian.oai:swagger-request-validator-mockmvc` (`openApi().isValid(spec)`), so any drift
  between `NotificationView` / status codes / params and the spec fails the build. This is what
  substitutes for the guarantee a generated interface would have given.
- **Runtime input validation (interim)** — Jakarta Bean Validation (`spring-boot-starter-validation`,
  `@Validated` + `@Min`/`@Max`/`@Pattern`, type binding for UUID and date-time) on the controller's
  path/query params, **mirroring the spec's parameter schemas** (`page >= 0`, `1 <= size <= 200`,
  ISO-8601 dates, allowed `status`). This is a stopgap until **runtime OpenAPI request validation**
  replaces the hand-written constraints; keep the Jakarta constraints and the spec parameter schemas in
  lockstep so the two enforcement layers never disagree.

## References (evidence)
- `cp-court-list-publishing-service`: `build.gradle` (`api-cp-crime-court-list-publisher` dependency),
  `src/main/java/uk/gov/hmcts/cp/controllers/CourtListPublishController.java` (`implements
  CourtListPublishApi`).
- `service-hmcts-crime-springboot-template`: `gradle/apispec-validation.gradle`,
  `.github/workflows/ci-released.yml`.
- `cp-notification-gateway`: `docs/pipeline/architecture-design.md`,
  `docs/pipeline/user-stories/NG-S04.md`, `gradle/apispec-validation.gradle`,
  `.github/workflows/ci-released.yml`, `contracts/` (async message schemas only — not the REST spec).
