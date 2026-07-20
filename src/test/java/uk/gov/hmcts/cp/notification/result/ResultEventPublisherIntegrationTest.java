package uk.gov.hmcts.cp.notification.result;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static uk.gov.hmcts.cp.notification.integration.stubs.ResultQueueStubService.aResultQueue;
import static uk.gov.hmcts.cp.notification.integration.stubs.support.ResultEventSchema.NOTIFICATION_FAILED;
import static uk.gov.hmcts.cp.notification.integration.stubs.support.ResultEventSchema.NOTIFICATION_SENT;
import static uk.gov.hmcts.cp.notification.integration.stubs.support.ServiceBusContainerSupport.SLICE_RESULT_QUEUE;
import static uk.gov.hmcts.cp.notification.integration.stubs.support.ServiceBusContainerSupport.getConnectionString;
import static uk.gov.hmcts.cp.notification.integration.testdata.ResultEventFactory.aNotificationFailedEvent;
import static uk.gov.hmcts.cp.notification.integration.testdata.ResultEventFactory.aNotificationSentEvent;

class ResultEventPublisherIntegrationTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final ResultEventPublisher publisher =
            new ResultEventPublisher(getConnectionString(), "", MAPPER);

    @Nested
    class Publishing {

        @Test
        void publishes_a_notification_sent_event_to_the_named_reply_queue() {
            final UUID id = UUID.randomUUID();

            publisher.publish(SLICE_RESULT_QUEUE, aNotificationSentEvent()
                    .notificationId(id)
                    .sendToAddress("recipient@example.com")
                    .clientContext("mi-reportdata")
                    .build());

            aResultQueue(SLICE_RESULT_QUEUE)
                    .receivesResultEvent()
                    .withSubject(NotificationSentEvent.EVENT_NAME)
                    .conformingTo(NOTIFICATION_SENT)
                    .withField("notificationId", id.toString())
                    .withField("sendToAddress", "recipient@example.com")
                    .withField("clientContext", "mi-reportdata");
        }

        @Test
        void publishes_a_notification_failed_event_to_the_named_reply_queue() {
            final UUID id = UUID.randomUUID();

            publisher.publish(SLICE_RESULT_QUEUE, aNotificationFailedEvent()
                    .notificationId(id)
                    .errorMessage("provider rejected the send")
                    .statusCode(400)
                    .clientContext("mi-reportdata")
                    .build());

            aResultQueue(SLICE_RESULT_QUEUE)
                    .receivesResultEvent()
                    .withSubject(NotificationFailedEvent.EVENT_NAME)
                    .conformingTo(NOTIFICATION_FAILED)
                    .withField("notificationId", id.toString())
                    .withField("errorMessage", "provider rejected the send")
                    .withField("statusCode", 400);
        }
    }

    @Nested
    class Gating {

        @ParameterizedTest
        @NullAndEmptySource
        void does_not_publish_when_the_notification_has_no_reply_queue(final String noReplyQueue) {
            publisher.publish(noReplyQueue, aNotificationSentEvent().build());

            aResultQueue(SLICE_RESULT_QUEUE).receivesNoResultEvent();
        }
    }
}
