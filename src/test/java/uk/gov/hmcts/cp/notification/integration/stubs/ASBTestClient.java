package uk.gov.hmcts.cp.notification.integration.stubs;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.time.Duration;
import uk.gov.hmcts.cp.notification.command.SendEmailCommand;
import uk.gov.hmcts.cp.notification.integration.stubs.support.ServiceBusContainerSupport;

import static uk.gov.hmcts.cp.notification.integration.stubs.support.ServiceBusContainerSupport.aDeadLetterReceiver;
import static uk.gov.hmcts.cp.notification.integration.stubs.support.ServiceBusContainerSupport.aServiceBusSenderClientBuilder;

public final class ASBTestClient {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private ASBTestClient() {
    }

    public static ASBTestClient anAsbTestClient() {
        return new ASBTestClient();
    }

    public ASBTestClient sendToCommandQueue(final SendEmailCommand command) {
        return sendToCommandQueue(MAPPER.writeValueAsString(command));
    }

    public ASBTestClient sendToCommandQueue(final SendEmailCommand command, final String replyTo) {
        return sendToCommandQueue(MAPPER.writeValueAsString(command), replyTo);
    }

    public ASBTestClient sendToCommandQueue(final String body) {
        return sendToCommandQueue(body, null);
    }

    public ASBTestClient sendToCommandQueue(final String body, final String replyTo) {
        final ServiceBusMessage message = new ServiceBusMessage(body);
        if (replyTo != null) {
            message.setReplyTo(replyTo);
        }
        try (ServiceBusSenderClient sender = aServiceBusSenderClientBuilder().buildClient()) {
            sender.sendMessage(message);
        }
        return this;
    }

    public ASBTestClient purgeDeadLetterQueue() {
        try (ServiceBusReceiverClient dlq = aDeadLetterReceiver()
                .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
                .buildClient()) {
            long drained;
            do {
                drained = dlq.receiveMessages(50, Duration.ofSeconds(1)).stream().count();
            } while (drained > 0);
        }
        return this;
    }

    public ServiceBusReceivedMessage peekDeadLetter() {
        try (ServiceBusReceiverClient dlq = aDeadLetterReceiver().buildClient()) {
            return dlq.peekMessage();
        }
    }
}
