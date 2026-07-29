package uk.gov.hmcts.cp.notification.task;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import uk.gov.hmcts.cp.notification.blob.AttachmentDownloader;
import uk.gov.hmcts.cp.notification.blob.PermanentBlobException;
import uk.gov.hmcts.cp.notification.command.SendEmailCommand;
import uk.gov.hmcts.cp.notification.sender.EmailSender;
import uk.gov.hmcts.cp.notification.sender.GovNotifyException;
import uk.gov.hmcts.cp.notification.service.NotificationStatusService;
import uk.gov.hmcts.cp.notification.time.Clock;
import uk.gov.hmcts.cp.taskmanager.domain.ExecutionInfo;
import uk.gov.hmcts.cp.taskmanager.domain.ExecutionStatus;
import uk.gov.hmcts.cp.taskmanager.service.ExecutionService;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.notification.integration.testdata.SendResultFactory.aSendResult;

@ExtendWith(MockitoExtension.class)
class SendEmailTaskTest {
    private static final List<Long> LEGACY_EMAIL_RETRY_DURATIONS =
            List.of(60L, 300L, 1800L, 3600L, 7200L, 14400L);

    @Mock
    private AttachmentDownloader attachmentDownloader;
    @Mock
    private EmailSender emailSender;
    @Mock
    private NotificationStatusService statusService;
    @Mock
    private ExecutionService executionService;
    @Mock
    private Clock clock;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private SendEmailTask task;

    @Captor
    private ArgumentCaptor<byte[]> attachmentCaptor;
    @Captor
    private ArgumentCaptor<ExecutionInfo> executionInfoCaptor;

    @BeforeEach
    void setUp() {
        task = new SendEmailTask(attachmentDownloader, emailSender, statusService, executionService,
                objectMapper, new CpTaskFactory(objectMapper, clock), LEGACY_EMAIL_RETRY_DURATIONS);
    }

    @Test
    void should_preserve_the_legacy_email_retry_durations() {
        assertThat(task.getRetryDurationsInSecs()).hasValue(LEGACY_EMAIL_RETRY_DURATIONS);
    }

    @Nested
    class WhenSendingSucceeds {

        @Test
        void should_download_attachment_and_pass_it_to_the_email_sender_when_file_uri_present() {
            final UUID id = UUID.randomUUID();
            final String fileUri = "https://sa.blob.core.windows.net/mi-reportdata/report.csv";
            final byte[] bytes = "report".getBytes(StandardCharsets.UTF_8);
            when(attachmentDownloader.download(fileUri)).thenReturn(bytes);
            when(emailSender.sendEmail(any(SendEmailCommand.class), any())).thenReturn(aSendResult().build());

            task.execute(sendEmailJob(id, fileUri));

            verify(attachmentDownloader).download(fileUri);
            verify(emailSender).sendEmail(any(SendEmailCommand.class), attachmentCaptor.capture());
            assertThat(attachmentCaptor.getValue()).isEqualTo(bytes);
        }

        @Test
        void should_send_without_attachment_when_file_uri_is_blank() {
            final UUID id = UUID.randomUUID();
            when(emailSender.sendEmail(any(SendEmailCommand.class), any())).thenReturn(aSendResult().build());

            task.execute(sendEmailJob(id, ""));

            verify(attachmentDownloader, never()).download(any());
            verify(emailSender).sendEmail(any(SendEmailCommand.class), attachmentCaptor.capture());
            assertThat(attachmentCaptor.getValue()).isNull();
        }

        @Test
        void should_schedule_check_email_status_carrying_the_reference_in_job_data_and_not_on_the_row() {
            final UUID id = UUID.randomUUID();
            final String fileUri = "https://sa.blob.core.windows.net/mi-reportdata/report.csv";
            final String externalReference = "1490dab7-2b48-4a9a-9f8a-2f0d0e2e6b11";
            when(attachmentDownloader.download(fileUri)).thenReturn("report".getBytes(StandardCharsets.UTF_8));
            when(emailSender.sendEmail(any(SendEmailCommand.class), any()))
                    .thenReturn(aSendResult().reference(externalReference).build());

            final ExecutionInfo result = task.execute(sendEmailJob(id, fileUri));

            verify(executionService).executeWith(executionInfoCaptor.capture());
            final ExecutionInfo scheduled = executionInfoCaptor.getValue();
            assertThat(scheduled.getAssignedTaskName()).isEqualTo(CheckEmailStatusTask.TASK_NAME);
            assertThat(scheduled.getJobData().toString()).contains(externalReference).contains(id.toString());

            verifyNoInteractions(statusService);
            assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        }
    }

    @Nested
    class WhenSendingFails {

        @Test
        void should_mark_failed_and_complete_without_retry_when_blob_download_is_permanently_failed() {
            final UUID id = UUID.randomUUID();
            final String fileUri = "https://sa.blob.core.windows.net/mi-reportdata/missing.csv";
            when(attachmentDownloader.download(fileUri))
                    .thenThrow(new PermanentBlobException("blob 404"));

            final ExecutionInfo result = task.execute(sendEmailJob(id, fileUri));

            verify(statusService).markFailed(eq(id), any(), any());
            verify(emailSender, never()).sendEmail(any(), any());
            verify(executionService, never()).executeWith(any());

            assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        }

        @Test
        void should_mark_failed_and_complete_without_retry_when_send_fails_permanently() {
            final UUID id = UUID.randomUUID();
            when(emailSender.sendEmail(any(SendEmailCommand.class), any()))
                    .thenThrow(new GovNotifyException(400, "bad request", null));

            final ExecutionInfo result = task.execute(sendEmailJob(id, ""));

            verify(statusService).markFailed(eq(id), eq(400), eq("bad request"));
            verify(executionService, never()).executeWith(any());
            assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        }

        @Test
        void should_retry_when_send_fails_transiently() {
            final UUID id = UUID.randomUUID();
            when(emailSender.sendEmail(any(SendEmailCommand.class), any()))
                    .thenThrow(new GovNotifyException(500, "Gov.Notify unavailable", null));

            final ExecutionInfo result = task.execute(sendEmailJob(id, ""));

            verify(statusService, never()).markFailed(any(), any(), any());
            verify(executionService, never()).executeWith(any());
            assertThat(result.getExecutionStatus()).isEqualTo(ExecutionStatus.INPROGRESS);
            assertThat(result.isShouldRetry()).isTrue();
        }
    }

    private ExecutionInfo sendEmailJob(final UUID notificationId, final String fileUri) {
        return ExecutionInfo.executionInfo()
                .withJobData(commandJobData(notificationId, fileUri))
                .withAssignedTaskName(SendEmailTask.TASK_NAME)
                .withAssignedTaskStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withExecutionStatus(ExecutionStatus.STARTED)
                .build();
    }

    private static JsonObject commandJobData(final UUID notificationId, final String fileUri) {
        return Json.createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("templateId", UUID.randomUUID().toString())
                .add("sendToAddress", "user@example.com")
                .add("fileUri", fileUri)
                .build();
    }
}
