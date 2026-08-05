package uk.gov.hmcts.cp.notification.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import uk.gov.hmcts.cp.notification.persistence.NotificationEntity;
import uk.gov.hmcts.cp.notification.persistence.NotificationRepository;
import uk.gov.hmcts.cp.notification.web.NotificationView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.notification.integration.testdata.NotificationEntityFactory.aNotificationEntity;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    private NotificationRepository notifications;

    @InjectMocks
    private NotificationQueryService queryService;

    @Nested
    class LookupById {

        @Test
        void maps_every_notification_column_when_looking_up_by_id() {
            final UUID id = UUID.randomUUID();
            final OffsetDateTime created = OffsetDateTime.parse("2026-07-01T09:00:00Z");
            final OffsetDateTime updated = OffsetDateTime.parse("2026-07-01T09:05:00Z");
            final NotificationEntity entity = aNotificationEntity()
                    .notificationId(id)
                    .notificationType("EMAIL")
                    .status("FAILED")
                    .sendToAddress("recipient@example.com")
                    .statusCode(503)
                    .errorMessage("Attachment not found")
                    .clientContext("mi-reportdata")
                    .resultQueue("ng-result-correspondence")
                    .createdAt(created)
                    .updatedAt(updated)
                    .build();
            when(notifications.findById(id)).thenReturn(Optional.of(entity));

            final NotificationView view = queryService.findById(id).orElseThrow();

            assertThat(view.notificationId()).isEqualTo(id);
            assertThat(view.notificationType()).isEqualTo("EMAIL");
            assertThat(view.status()).isEqualTo("FAILED");
            assertThat(view.sendToAddress()).isEqualTo("recipient@example.com");
            assertThat(view.statusCode()).isEqualTo(503);
            assertThat(view.errorMessage()).isEqualTo("Attachment not found");
            assertThat(view.clientContext()).isEqualTo("mi-reportdata");
            assertThat(view.resultQueue()).isEqualTo("ng-result-correspondence");
            assertThat(view.createdAt()).isEqualTo(created);
            assertThat(view.updatedAt()).isEqualTo(updated);
        }

        @Test
        void returns_empty_when_no_notification_matches_the_id() {
            final UUID id = UUID.randomUUID();
            when(notifications.findById(id)).thenReturn(Optional.empty());

            assertThat(queryService.findById(id)).isEmpty();
        }
    }

    @Nested
    class Search {

        @Test
        void searches_by_status_and_created_range_and_maps_the_matching_notifications() {
            final UUID id = UUID.randomUUID();
            final OffsetDateTime from = OffsetDateTime.parse("2026-07-01T00:00:00Z");
            final OffsetDateTime to = OffsetDateTime.parse("2026-07-02T00:00:00Z");
            final Pageable pageable = PageRequest.of(0, 5);
            final Page<NotificationEntity> matches = new PageImpl<>(
                    List.of(aNotificationEntity().notificationId(id).status("SENT").build()), pageable, 1);
            when(notifications.search(eq("SENT"), eq(from), eq(to), eq(pageable))).thenReturn(matches);

            final Page<NotificationView> result = queryService.search("SENT", from, to, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent()).singleElement()
                    .satisfies(view -> {
                        assertThat(view.notificationId()).isEqualTo(id);
                        assertThat(view.status()).isEqualTo("SENT");
                    });
        }
    }
}
