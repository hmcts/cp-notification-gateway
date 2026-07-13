package uk.gov.hmcts.cp.notification.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for integration tests: boots the full application context under the {@code test} profile on a
 * real random port, backed by the shared {@link PostgresContainerSupport} Testcontainer. The sanity
 * test and the Cucumber acceptance suite both extend this base, so acceptance and sanity boot
 * identically (same web environment, profile and database) — no divergence.
 *
 * <p>This is the whole-app base. Sliced per-boundary bases (e.g. {@code AbstractPostgresIntegrationTest},
 * {@code AbstractControllerIntegrationTest}, {@code AbstractAzureServiceBusIntegrationTest},
 * {@code AbstractRestClientIntegrationTest}) are introduced as those boundaries land, each composing
 * only the relevant support class(es) — {@link PostgresContainerSupport} being the first reusable one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void datasourceProperties(final DynamicPropertyRegistry registry) {
        PostgresContainerSupport.registerProperties(registry);
    }
}
