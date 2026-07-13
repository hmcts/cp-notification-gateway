package uk.gov.hmcts.cp.notification.integration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single wiring / sanity test: boots the full application context under the {@code test} profile
 * on a real random port (shared Postgres Testcontainer via {@link AbstractIntegrationTest}) and makes
 * real HTTP calls with {@link TestRestTemplate} — no MockMvc, so the servlet/filter chain is exercised
 * end to end.
 *
 * <p>This is also the home for technical / NFR checks — actuator health/readiness/liveness and
 * build/git info now, {@code /actuator/prometheus} and JSON-logging wiring later — grouped as
 * {@link Nested} classes rather than a separate test class per concern.
 */
@AutoConfigureTestRestTemplate
class ApplicationContextIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void the_application_context_loads() {
        assertThat(applicationContext.getBeanDefinitionCount()).isPositive();
    }

    @Nested
    class ActuatorTests {

        @Test
        @SuppressWarnings("unchecked")
        void health_reports_up_with_liveness_and_readiness_groups() {
            final ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/health", Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            final Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("status")).isEqualTo("UP");
            assertThat((List<String>) body.get("groups")).containsExactlyInAnyOrder("liveness", "readiness");
        }

        @Test
        @SuppressWarnings("unchecked")
        void info_exposes_build_and_git_metadata() {
            final ResponseEntity<Map> response = restTemplate.getForEntity("/actuator/info", Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            final Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();

            final Map<String, Object> build = (Map<String, Object>) body.get("build");
            assertThat(build).isNotNull();
            assertThat(build.get("artifact")).isEqualTo("cp-notification-gateway");
            assertThat(build.get("name")).isEqualTo("cp-notification-gateway");
            assertThat(build.get("time")).isNotNull();
            assertThat(build.get("version")).isNotNull();

            final Map<String, Object> git = (Map<String, Object>) body.get("git");
            assertThat(git).isNotNull();
            assertThat(git.get("branch")).isNotNull();
            assertThat((Map<String, Object>) git.get("commit")).containsKeys("id", "time");
        }
    }
}
