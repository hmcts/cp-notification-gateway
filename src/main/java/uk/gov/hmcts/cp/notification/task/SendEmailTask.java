package uk.gov.hmcts.cp.notification.task;

import static uk.gov.hmcts.cp.taskmanager.domain.ExecutionInfo.executionInfo;
import static uk.gov.hmcts.cp.taskmanager.domain.ExecutionStatus.COMPLETED;
import static uk.gov.hmcts.cp.taskmanager.domain.ExecutionStatus.INPROGRESS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import uk.gov.hmcts.cp.notification.blob.AttachmentDownloader;
import uk.gov.hmcts.cp.notification.blob.PermanentBlobException;
import uk.gov.hmcts.cp.notification.command.SendEmailCommand;
import uk.gov.hmcts.cp.notification.sender.EmailSender;
import uk.gov.hmcts.cp.notification.sender.GovNotifyException;
import uk.gov.hmcts.cp.notification.sender.GovNotifyFailureClassifier;
import uk.gov.hmcts.cp.notification.sender.SendResult;
import uk.gov.hmcts.cp.notification.service.NotificationStatusService;
import uk.gov.hmcts.cp.taskmanager.domain.ExecutionInfo;
import uk.gov.hmcts.cp.taskmanager.service.ExecutionService;
import uk.gov.hmcts.cp.taskmanager.service.task.ExecutableTask;
import uk.gov.hmcts.cp.taskmanager.service.task.Task;

import java.util.List;
import java.util.Optional;

@Task(SendEmailTask.TASK_NAME)
@Component
public class SendEmailTask implements ExecutableTask {
    public static final String TASK_NAME = "SEND_EMAIL";

    private static final Logger LOG = LoggerFactory.getLogger(SendEmailTask.class);

    private final AttachmentDownloader attachmentDownloader;
    private final EmailSender emailSender;
    private final NotificationStatusService statusService;
    private final ExecutionService executionService;
    private final ObjectMapper objectMapper;
    private final CpTaskFactory taskFactory;
    private final List<Long> retryDurationsSecs;

    public SendEmailTask(
            final AttachmentDownloader attachmentDownloader,
            final EmailSender emailSender,
            final NotificationStatusService statusService,
            final ExecutionService executionService,
            final ObjectMapper objectMapper,
            final CpTaskFactory taskFactory,
            @Value("${cp.notification.retry.email-durations-secs}") final List<Long> retryDurationsSecs) {
        this.attachmentDownloader = attachmentDownloader;
        this.emailSender = emailSender;
        this.statusService = statusService;
        this.executionService = executionService;
        this.objectMapper = objectMapper;
        this.taskFactory = taskFactory;
        this.retryDurationsSecs = retryDurationsSecs;
    }

    @Override
    public ExecutionInfo execute(final ExecutionInfo executionInfo) {
        final SendEmailCommand command = objectMapper.readValue(
                executionInfo.getJobData().toString(), SendEmailCommand.class);

        final byte[] attachment;
        try {
            attachment = StringUtils.hasText(command.fileUri())
                    ? attachmentDownloader.download(command.fileUri())
                    : null;
        } catch (final PermanentBlobException e) {
            LOG.warn("Permanent attachment failure for notification {} — marking FAILED, no retry",
                    command.notificationId());
            statusService.markFailed(command.notificationId(), e.getStatusCode(), e.getMessage());
            return completed(executionInfo);
        }

        final SendResult result;
        try {
            result = emailSender.sendEmail(command, attachment);
        } catch (final GovNotifyException e) {
            if (GovNotifyFailureClassifier.isTemporary(e.getHttpStatus(), e.getMessage())) {
                LOG.warn("Transient send failure for notification {} (http {}) — will retry",
                        command.notificationId(), e.getHttpStatus());
                return retry(executionInfo);
            }
            LOG.warn("Permanent send failure for notification {} (http {}) — marking FAILED",
                    command.notificationId(), e.getHttpStatus());
            statusService.markFailed(command.notificationId(), e.getHttpStatus(), e.getMessage());
            return completed(executionInfo);
        }

        executionService.executeWith(taskFactory.createCheckStatusJob(command, result.reference()));
        return completed(executionInfo);
    }

    @Override
    public Optional<List<Long>> getRetryDurationsInSecs() {
        return Optional.of(retryDurationsSecs);
    }

    private static ExecutionInfo completed(final ExecutionInfo executionInfo) {
        return executionInfo().from(executionInfo).withExecutionStatus(COMPLETED).build();
    }

    private static ExecutionInfo retry(final ExecutionInfo executionInfo) {
        return executionInfo().from(executionInfo)
                .withExecutionStatus(INPROGRESS)
                .withShouldRetry(true)
                .build();
    }
}
