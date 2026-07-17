package uk.gov.hmcts.cp.notification.messaging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import uk.gov.hmcts.cp.notification.command.SendEmailCommand;
import uk.gov.hmcts.cp.notification.integration.config.AsbConsumerSliceConfig;
import uk.gov.hmcts.cp.notification.integration.stubs.ASBTestClient;
import uk.gov.hmcts.cp.notification.integration.stubs.support.ServiceBusContainerSupport;
import uk.gov.hmcts.cp.notification.service.NotificationIngestionService;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.cp.notification.integration.stubs.ASBTestClient.anAsbTestClient;
import static uk.gov.hmcts.cp.notification.integration.testdata.SendEmailCommandFactory.aSendEmailCommand;

@SpringBootTest(classes = AsbConsumerSliceConfig.class)
@ActiveProfiles("test")
class SendEmailConsumerIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @DynamicPropertySource
    static void serviceBusProperties(final DynamicPropertyRegistry registry) {
        ServiceBusContainerSupport.registerProperties(registry, ServiceBusContainerSupport.SLICE_COMMAND_QUEUE);
    }

    @MockitoBean
    private NotificationIngestionService ingestionService;

    private final ASBTestClient asb = anAsbTestClient(ServiceBusContainerSupport.SLICE_COMMAND_QUEUE);

    @AfterEach
    void cleanUp() {
        asb.purgeDeadLetterQueue();
    }

    @Nested
    class Delegation {

        @Test
        void consumes_a_valid_command_delegating_to_ingestion_with_reply_to_and_completes_the_message() {
            final UUID id = UUID.randomUUID();

            asb.sendToCommandQueue(aSendEmailCommand()
                    .notificationId(id)
                    .sendToAddress("user@example.com")
                    .clientContext("mi-reportdata")
                    .build(), "nn-result-correspondence");

            final ArgumentCaptor<SendEmailCommand> command = ArgumentCaptor.forClass(SendEmailCommand.class);
            verify(ingestionService, timeout(TIMEOUT.toMillis()))
                    .ingest(command.capture(), eq("nn-result-correspondence"));
            assertThat(command.getValue().notificationId()).isEqualTo(id);
            assertThat(command.getValue().sendToAddress()).isEqualTo("user@example.com");
            assertThat(command.getValue().clientContext()).isEqualTo("mi-reportdata");
        }

        @Test
        void delegates_with_no_reply_to_when_the_message_carries_none() {
            final UUID id = UUID.randomUUID();

            asb.sendToCommandQueue(aSendEmailCommand().notificationId(id).build());

            verify(ingestionService, timeout(TIMEOUT.toMillis())).ingest(any(SendEmailCommand.class), isNull());
        }
    }

    @Nested
    class DeadLettering {

        @Test
        void dead_letters_and_does_not_delegate_an_unparseable_message() {
            asb.sendToCommandQueue("this is not json");

            await().atMost(TIMEOUT).until(() -> asb.peekDeadLetter() != null);

            verifyNoInteractions(ingestionService);
        }

        @Test
        void dead_letters_and_does_not_delegate_a_command_that_fails_validation() {
            asb.sendToCommandQueue("{\"sendToAddress\":\"user@example.com\"}");

            await().atMost(TIMEOUT).until(() -> asb.peekDeadLetter() != null);

            verifyNoInteractions(ingestionService);
        }
    }

    @Nested
    class Redelivery {

        @Test
        void abandons_and_redelivers_the_message_when_ingestion_fails() {
            doThrow(new IllegalStateException("ingestion failed")).when(ingestionService).ingest(any(), any());

            asb.sendToCommandQueue(aSendEmailCommand().notificationId(UUID.randomUUID()).build());

            verify(ingestionService, timeout(TIMEOUT.toMillis()).atLeast(2)).ingest(any(), any());
            await().atMost(TIMEOUT).until(() -> asb.peekDeadLetter() != null);
        }
    }
}
