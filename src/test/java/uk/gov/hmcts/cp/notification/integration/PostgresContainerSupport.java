package uk.gov.hmcts.cp.notification.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Single Postgres Testcontainer shared across every integration and acceptance Spring context.
 *
 * <p>Started once on class load and reused as a JVM-wide singleton (reaped at shutdown) so the
 * sanity/wiring test, the Cucumber acceptance suite and the boundary/schema tests all talk to one
 * database — the shared test setup the test strategy requires, and a single container start rather
 * than one per context.
 */
public final class PostgresContainerSupport {

    @SuppressWarnings("resource") // JVM-wide singleton; reaped by Testcontainers at shutdown
    public static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private PostgresContainerSupport() {
    }

    /** Point Spring's datasource at the shared container. */
    public static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
