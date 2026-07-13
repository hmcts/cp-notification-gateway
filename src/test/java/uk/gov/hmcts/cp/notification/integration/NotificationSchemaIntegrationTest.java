package uk.gov.hmcts.cp.notification.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PROVISIONAL (NG-S01 schema enabler). Asserts the single Flyway run creates the notification schema
 * (AC-001) and that cp-task-manager's Flyway auto-config co-locates the {@code jobs} table in the same
 * run (AC-001a). There is no repository / boundary class yet — that arrives in NG-S02 — so these
 * schema assertions have no boundary integration test to live in.
 *
 * <p>TODO(NG-S02): fold these assertions into {@code NotificationRepositoryIntegrationTest} (the
 * repository boundary test) and delete this provisional test — do not let provisional tests
 * accumulate. Tagged {@code provisional} per the test strategy's bootstrapping release valve.
 */
@Tag("provisional")
class NotificationSchemaIntegrationTest extends AbstractIntegrationTest {

    private static final String COLUMNS_SQL =
            "SELECT column_name FROM information_schema.columns "
                    + "WHERE table_schema = 'public' AND table_name = ?";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void notification_table_has_exactly_the_ac001_columns() {
        assertThat(columnsOf("notification")).containsExactlyInAnyOrder(
                "notification_id",
                "notification_type",
                "status",
                "send_to_address",
                "status_code",
                "error_message",
                "client_context",
                "result_queue",
                "created_at",
                "updated_at");
    }

    @Test
    void jobs_table_is_co_located_by_task_manager_autoconfig() {
        // cp-task-manager owns this schema; assert presence plus the lock columns the gap analysis relies on.
        assertThat(columnsOf("jobs")).contains("job_id", "worker_id", "worker_lock_time");
    }

    private List<String> columnsOf(final String table) {
        return jdbcTemplate.queryForList(COLUMNS_SQL, String.class, table)
                .stream()
                .map(c -> c.toLowerCase(Locale.ROOT))
                .toList();
    }
}
