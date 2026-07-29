# Testing

Four test categories. **Acceptance (BDD) is orthogonal to the unit/integration pyramid** — it is not a
pyramid layer. All run under `./gradlew test`.

| Category | Location / naming | Purpose |
|---|---|---|
| **Unit** | `*Test` in `src/test/java` | Pure logic, no Spring context |
| **Integration / wiring** | `*IntegrationTest` in `…/integration/` | `@SpringBootTest` boundary tests; one context/sanity test also asserts actuator / technical-NFR behaviour |
| **Acceptance / BDD** | `…/acceptance/` + `src/test/resources/features/` | Plain Cucumber on JUnit Platform + Spring (**no Serenity**); **business scenarios only** |

## Acceptance (Cucumber) harness

Shipped in this template — plain Cucumber, no Serenity:

- **Dependencies** (`build.gradle`): `io.cucumber:cucumber-bom` (platform) + `cucumber-java`,
  `cucumber-junit-platform-engine`, `cucumber-spring`, and `org.junit.platform:junit-platform-suite`.
- **Runner** — `acceptance/AcceptanceTest` (`@Suite` + `@IncludeEngines("cucumber")` +
  `@SelectClasspathResource("features")`). **Required for Gradle:** Gradle discovers tests by class, so
  without this suite the Cucumber engine never runs. One suite runs every feature once — do not add a
  second runner.
- **Glue config** — `src/test/resources/junit-platform.properties`
  (`cucumber.glue=uk.gov.hmcts.cp.acceptance`).
- **Spring context** — `acceptance/CucumberSpringConfiguration` (`@CucumberContextConfiguration` +
  `@SpringBootTest` + `@ActiveProfiles("test")`), same setup as the integration/sanity tests.
- **Feature files** — `src/test/resources/features/*.feature`.
- Delete the shipped `example.feature` + `ExampleStepDefinitions` once you add real scenarios.

## Boundary stubs must verify the request, not just return a response

Integration and acceptance tests own both sides of every external boundary (WireMock HTTP, Azure
Service Bus, Azurite Blob). A stub that only returns a canned response proves nothing about what the
service actually sent. Every stub/verification helper **must assert the received request field by
field, where feasible**:

- **URL / path & method** — exact path including path variables (e.g. the Gov.Notify notification id
  on the status poll) and the HTTP verb / queue name.
- **Headers** — especially auth (e.g. `Authorization: Bearer <jwt>`).
- **Body** — every field the scenario controls: scalars compared exactly; encoded payloads decoded
  and compared to the source (e.g. base64 attachment == original file bytes); fields that must be
  absent asserted absent (e.g. no `email_reply_to_id` when the command carries none).
- **ASB / messaging** — when the service publishes, assert the message body and application
  properties field by field, not just that a message arrived.

Parse the captured request and assert with AssertJ; avoid loose "a call happened" checks. Reference:
`GovUkNotifyStubService.sendEmailWasCalledWith(...)` and `deliveryStatusWasPolledFor(...)`.

## Step-definition organisation (avoid the common Cucumber pitfalls)

- **No 1:1 feature→step-def file.** Organise step defs by domain concept; reuse across features.
- **Thin, low-complexity steps** — delegate to helpers/services; no `if`/`else`/loops or scenario logic
  in glue (that cyclomatic complexity is the classic maintenance overhead).
- **Share state via a Spring/Cucumber scenario-scoped bean**, never static fields.
- **Declarative Gherkin** (business language); technical translation lives in the glue.
- Prefer parameter types / data tables over regex-heavy steps; avoid conjunction ("And"-heavy) mega-steps.
