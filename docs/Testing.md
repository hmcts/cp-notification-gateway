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

## Step-definition organisation (avoid the common Cucumber pitfalls)

- **No 1:1 feature→step-def file.** Organise step defs by domain concept; reuse across features.
- **Thin, low-complexity steps** — delegate to helpers/services; no `if`/`else`/loops or scenario logic
  in glue (that cyclomatic complexity is the classic maintenance overhead).
- **Share state via a Spring/Cucumber scenario-scoped bean**, never static fields.
- **Declarative Gherkin** (business language); technical translation lives in the glue.
- Prefer parameter types / data tables over regex-heavy steps; avoid conjunction ("And"-heavy) mega-steps.
