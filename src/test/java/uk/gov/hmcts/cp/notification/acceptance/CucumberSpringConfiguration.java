package uk.gov.hmcts.cp.notification.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import uk.gov.hmcts.cp.notification.integration.AbstractIntegrationTest;

/**
 * Boots the Spring context for Cucumber acceptance scenarios by <strong>extending</strong>
 * {@link AbstractIntegrationTest} — so the acceptance suite and the sanity test share the exact same
 * setup (RANDOM_PORT web environment, {@code test} profile, shared Postgres Testcontainer) and cannot
 * drift apart. All Cucumber code (this glue + the step definitions) lives in this {@code acceptance}
 * package; discovery + glue are configured in {@code src/test/resources/junit-platform.properties}.
 */
@CucumberContextConfiguration
public class CucumberSpringConfiguration extends AbstractIntegrationTest {
}
