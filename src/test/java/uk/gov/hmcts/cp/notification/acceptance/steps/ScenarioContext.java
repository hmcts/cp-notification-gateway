package uk.gov.hmcts.cp.notification.acceptance.steps;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@ScenarioScope
public class ScenarioContext {

    private String notificationId;
    private String templateId;
    private String externalReference;
    private String commandJson;
    private String replyQueue;
    private byte[] attachmentBytes;
    private String recordedStatus;
    private OffsetDateTime recordedCreatedAt;
    private String lastResponseBody;

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(final String notificationId) {
        this.notificationId = notificationId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final String templateId) {
        this.templateId = templateId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(final String externalReference) {
        this.externalReference = externalReference;
    }

    public String getCommandJson() {
        return commandJson;
    }

    public void setCommandJson(final String commandJson) {
        this.commandJson = commandJson;
    }

    public String getReplyQueue() {
        return replyQueue;
    }

    public void setReplyQueue(final String replyQueue) {
        this.replyQueue = replyQueue;
    }

    public byte[] getAttachmentBytes() {
        return attachmentBytes;
    }

    public void setAttachmentBytes(final byte[] attachmentBytes) {
        this.attachmentBytes = attachmentBytes;
    }

    public String getRecordedStatus() {
        return recordedStatus;
    }

    public void setRecordedStatus(final String recordedStatus) {
        this.recordedStatus = recordedStatus;
    }

    public OffsetDateTime getRecordedCreatedAt() {
        return recordedCreatedAt;
    }

    public void setRecordedCreatedAt(final OffsetDateTime recordedCreatedAt) {
        this.recordedCreatedAt = recordedCreatedAt;
    }

    public String getLastResponseBody() {
        return lastResponseBody;
    }

    public void setLastResponseBody(final String lastResponseBody) {
        this.lastResponseBody = lastResponseBody;
    }
}
