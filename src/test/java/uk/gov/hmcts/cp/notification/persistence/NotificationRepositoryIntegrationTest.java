package uk.gov.hmcts.cp.notification.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import uk.gov.hmcts.cp.notification.integration.stubs.support.PostgresContainerSupport;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.notification.integration.testdata.NotificationEntityFactory.aNotificationEntity;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class NotificationRepositoryIntegrationTest {

    @DynamicPropertySource
    static void datasourceProperties(final DynamicPropertyRegistry registry) {
        PostgresContainerSupport.registerProperties(registry);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/taskmanager");
    }

    @Autowired
    private NotificationRepository notifications;

    @Test
    void saves_and_reads_back_a_notification() {
        final UUID id = UUID.randomUUID();

        notifications.saveAndFlush(aNotificationEntity()
                .notificationId(id)
                .sendToAddress("recipient@example.com")
                .clientContext("mi-reportdata")
                .resultQueue("nn-result-mi-reportdata")
                .build());

        final NotificationEntity reloaded = notifications.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo("QUEUED");
        assertThat(reloaded.getNotificationType()).isEqualTo("EMAIL");
        assertThat(reloaded.getSendToAddress()).isEqualTo("recipient@example.com");
        assertThat(reloaded.getClientContext()).isEqualTo("mi-reportdata");
        assertThat(reloaded.getResultQueue()).isEqualTo("nn-result-mi-reportdata");
    }

    @Test
    void persists_a_notification_with_only_the_required_fields() {
        final UUID id = UUID.randomUUID();

        notifications.saveAndFlush(aNotificationEntity()
                .notificationId(id)
                .sendToAddress(null)
                .build());

        final NotificationEntity reloaded = notifications.findById(id).orElseThrow();
        assertThat(reloaded.getSendToAddress()).isNull();
        assertThat(reloaded.getClientContext()).isNull();
        assertThat(reloaded.getResultQueue()).isNull();
    }
}
