package uk.gov.hmcts.cp.notification.task;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import uk.gov.hmcts.cp.notification.command.SendEmailCommand;
import uk.gov.hmcts.cp.notification.time.Clock;
import uk.gov.hmcts.cp.taskmanager.domain.ExecutionInfo;
import uk.gov.hmcts.cp.taskmanager.domain.ExecutionStatus;

import java.io.StringReader;

@Component
public class CpTaskFactory {
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CpTaskFactory(final ObjectMapper objectMapper, final Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ExecutionInfo createSendEmailJob(final SendEmailCommand command) {
        return ExecutionInfo.executionInfo()
                .withJobData(sendEmailJobData(command))
                .withAssignedTaskName(SendEmailTask.TASK_NAME)
                .withAssignedTaskStartTime(clock.zonedDateTime())
                .withExecutionStatus(ExecutionStatus.STARTED)
                .build();
    }

    public ExecutionInfo createCheckStatusJob(final SendEmailCommand command, final String reference) {
        return ExecutionInfo.executionInfo()
                .withJobData(checkStatusJobData(command, reference))
                .withAssignedTaskName(CheckEmailStatusTask.TASK_NAME)
                .withAssignedTaskStartTime(clock.zonedDateTime())
                .withExecutionStatus(ExecutionStatus.STARTED)
                .build();
    }

    private JsonObject sendEmailJobData(final SendEmailCommand command) {
        final String json = objectMapper.writeValueAsString(command);
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }

    private static JsonObject checkStatusJobData(final SendEmailCommand command, final String reference) {
        return Json.createObjectBuilder()
                .add(CheckEmailStatusTask.KEY_NOTIFICATION_ID, command.notificationId().toString())
                .add(CheckEmailStatusTask.KEY_REFERENCE, reference)
                .build();
    }
}
