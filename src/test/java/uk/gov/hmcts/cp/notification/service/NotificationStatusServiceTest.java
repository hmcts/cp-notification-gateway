package uk.gov.hmcts.cp.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.hmcts.cp.notification.persistence.NotificationEntity;
import uk.gov.hmcts.cp.notification.persistence.NotificationRepository;
import uk.gov.hmcts.cp.notification.time.Clock;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.notification.integration.testdata.NotificationEntityFactory.aNotificationEntity;

@ExtendWith(MockitoExtension.class)
class NotificationStatusServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private Clock clock;

    private NotificationStatusService service;

    @BeforeEach
    void setUp() {
        service = new NotificationStatusService(notificationRepository, clock);
    }

    @Test
    void marks_a_notification_sent() {
        final UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id))
                .thenReturn(Optional.of(aNotificationEntity().notificationId(id).build()));

        service.markSent(id);

        final ArgumentCaptor<NotificationEntity> saved = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo("SENT");
    }

    @Test
    void marks_a_notification_failed_with_status_code_and_error_message() {
        final UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id))
                .thenReturn(Optional.of(aNotificationEntity().notificationId(id).build()));

        service.markFailed(id, 500, "provider rejected the send");

        final ArgumentCaptor<NotificationEntity> saved = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(saved.getValue().getStatusCode()).isEqualTo(500);
        assertThat(saved.getValue().getErrorMessage()).isEqualTo("provider rejected the send");
    }

    @Test
    void fails_when_the_notification_row_is_missing() {
        final UUID id = UUID.randomUUID();
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markSent(id)).isInstanceOf(IllegalStateException.class);
    }
}
