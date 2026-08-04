package uk.gov.hmcts.cp.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import uk.gov.hmcts.cp.notification.persistence.NotificationEntity;
import uk.gov.hmcts.cp.notification.persistence.NotificationRepository;
import uk.gov.hmcts.cp.notification.task.CpTaskFactory;
import uk.gov.hmcts.cp.notification.task.SendEmailTask;
import uk.gov.hmcts.cp.notification.time.Clock;
import uk.gov.hmcts.cp.taskmanager.domain.ExecutionInfo;
import uk.gov.hmcts.cp.taskmanager.service.ExecutionService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.notification.integration.testdata.SendEmailCommandFactory.aSendEmailCommand;

@ExtendWith(MockitoExtension.class)
class NotificationIngestionServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ExecutionService executionService;
    @Mock
    private Clock clock;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private NotificationIngestionService service;

    @BeforeEach
    void setUp() {
        service = new NotificationIngestionService(
                notificationRepository, executionService, new CpTaskFactory(objectMapper, clock), clock);
    }

    @Test
    void persists_a_queued_notification_and_enqueues_a_send_email_task() {
        final UUID id = UUID.randomUUID();
        when(notificationRepository.existsById(id)).thenReturn(false);

        service.ingest(aSendEmailCommand()
                .notificationId(id)
                .sendToAddress("user@example.com")
                .clientContext("mi-reportdata")
                .build(), "nn-result-correspondence");

        final ArgumentCaptor<NotificationEntity> saved = ArgumentCaptor.forClass(NotificationEntity.class);
        verify(notificationRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo("QUEUED");
        assertThat(saved.getValue().getNotificationType()).isEqualTo("EMAIL");
        assertThat(saved.getValue().getSendToAddress()).isEqualTo("user@example.com");
        assertThat(saved.getValue().getClientContext()).isEqualTo("mi-reportdata");
        assertThat(saved.getValue().getResultQueue()).isEqualTo("nn-result-correspondence");

        final ArgumentCaptor<ExecutionInfo> job = ArgumentCaptor.forClass(ExecutionInfo.class);
        verify(executionService).executeWith(job.capture());
        assertThat(job.getValue().getAssignedTaskName()).isEqualTo(SendEmailTask.TASK_NAME);
        assertThat(job.getValue().getJobData().toString()).contains(id.toString());
    }

    @Test
    void propagates_the_exception_when_enqueueing_the_send_email_task_fails() {
        final UUID id = UUID.randomUUID();
        when(notificationRepository.existsById(id)).thenReturn(false);
        doThrow(new IllegalStateException("enqueue failed")).when(executionService).executeWith(any());

        assertThatThrownBy(() -> service.ingest(aSendEmailCommand().notificationId(id).build(), null))
                .isInstanceOf(IllegalStateException.class);

        verify(notificationRepository).save(any(NotificationEntity.class));
    }

    @Test
    void is_an_idempotent_no_op_for_a_duplicate_notification_id() {
        final UUID id = UUID.randomUUID();
        when(notificationRepository.existsById(id)).thenReturn(true);

        service.ingest(aSendEmailCommand().notificationId(id).build(), null);

        verify(notificationRepository, never()).save(any());
        verify(executionService, never()).executeWith(any());
    }
}
