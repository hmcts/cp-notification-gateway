package uk.gov.hmcts.cp.notification.integration.stubs;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;

import java.time.Duration;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.notification.integration.stubs.ASBTestClient.anAsbTestClient;

/**
 * Stands in for a future originator context consuming its ReplyTo result queue. Fluent reader over the
 * ASB emulator so tests assert the received result event field-by-field (subject = event name, body =
 * golden-master payload) rather than merely that something arrived.
 */
public final class ResultQueueStubService {

    private static final Duration RECEIVE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration SILENCE_WINDOW = Duration.ofSeconds(10);

    private final ASBTestClient queue;
    private ServiceBusReceivedMessage received;

    private ResultQueueStubService(final String queueName) {
        this.queue = anAsbTestClient(queueName);
    }

    public static ResultQueueStubService aResultQueue(final String queueName) {
        return new ResultQueueStubService(queueName);
    }

    public ResultQueueStubService receivesResultEvent() {
        received = queue.receiveMessage(RECEIVE_TIMEOUT);
        assertThat(received).as("a result event should have been published to the reply queue").isNotNull();
        return this;
    }

    public ResultQueueStubService withSubject(final String expectedEventName) {
        assertThat(received.getSubject())
                .as("result event ASB subject carries the golden-master event name")
                .isEqualTo(expectedEventName);
        return this;
    }

    public ResultQueueStubService conformingTo(final String schemaFile) {
        uk.gov.hmcts.cp.notification.integration.stubs.support.ResultEventSchema
                .assertConformsTo(schemaFile, body());
        return this;
    }

    public ResultQueueStubService withField(final String jsonPath, final Object expectedValue) {
        assertThatJson(body()).node(jsonPath).isEqualTo(expectedValue);
        return this;
    }

    public void drain() {
        while (queue.receiveMessage(Duration.ofMillis(500)) != null) {
            // discard any residual events left by a previous scenario
        }
    }

    public void receivesNoResultEvent() {
        assertThat(queue.receiveMessage(SILENCE_WINDOW))
                .as("no result event should be published when the notification has no reply queue")
                .isNull();
    }

    public String body() {
        return received.getBody().toString();
    }
}
