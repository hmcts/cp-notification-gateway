package uk.gov.hmcts.cp.notification.messaging;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import uk.gov.hmcts.cp.notification.command.SendEmailCommand;
import uk.gov.hmcts.cp.notification.service.NotificationIngestionService;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class SendEmailConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(SendEmailConsumer.class);

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final NotificationIngestionService ingestionService;

    public void processMessage(final ServiceBusReceivedMessageContext context) {
        final String messageId = context.getMessage().getMessageId();
        final String body = context.getMessage().getBody().toString();

        final SendEmailCommand command;
        try {
            command = objectMapper.readValue(body, SendEmailCommand.class);
        } catch (final JacksonException e) {
            LOG.warn("Dead-lettering unparseable message {}: {}", messageId, e.getMessage());
            context.deadLetter();
            return;
        }

        final Set<ConstraintViolation<SendEmailCommand>> violations = validator.validate(command);
        if (!violations.isEmpty()) {
            LOG.warn("Dead-lettering invalid message {}: {}", messageId, violations);
            context.deadLetter();
            return;
        }

        try {
            ingestionService.ingest(command, context.getMessage().getReplyTo());
            context.complete();
        } catch (final Exception e) {
            LOG.error("Transient failure processing message {} — abandoning for redelivery", messageId, e);
            context.abandon();
        }
    }

    public void processError(final ServiceBusErrorContext context) {
        LOG.error("Service Bus error on entity {}", context.getEntityPath(), context.getException());
    }
}
