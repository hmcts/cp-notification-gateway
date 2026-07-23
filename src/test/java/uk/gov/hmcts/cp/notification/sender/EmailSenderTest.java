package uk.gov.hmcts.cp.notification.sender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.cp.notification.integration.testdata.SendEmailCommandFactory.aSendEmailCommand;
import static uk.gov.hmcts.cp.notification.integration.testdata.SendResultFactory.aSendResult;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private EmailClientFactory senderFactory;
    @Mock
    private EmailClient emailClient;

    @InjectMocks
    private EmailSender emailSender;

    @Captor
    private ArgumentCaptor<SendEmailRequest> requestCaptor;

    @Test
    void routes_the_request_built_from_the_command_and_attachment_to_the_selected_client() {
        final UUID id = UUID.randomUUID();
        final byte[] attachment = "report".getBytes(StandardCharsets.UTF_8);
        when(senderFactory.selectFor(any(SendEmailRequest.class))).thenReturn(emailClient);
        when(emailClient.send(any(SendEmailRequest.class))).thenReturn(aSendResult().reference("ref-1").build());

        final SendResult result = emailSender.sendEmail(
                aSendEmailCommand().notificationId(id).sendToAddress("user@example.com").build(), attachment);

        verify(senderFactory).selectFor(requestCaptor.capture());
        assertThat(requestCaptor.getValue().notificationId()).isEqualTo(id);
        assertThat(requestCaptor.getValue().emailAddress()).isEqualTo("user@example.com");
        assertThat(requestCaptor.getValue().attachment()).isEqualTo(attachment);
        assertThat(result.reference()).isEqualTo("ref-1");
    }

    @Test
    void sends_a_request_with_no_attachment_when_none_is_supplied() {
        final UUID id = UUID.randomUUID();
        when(senderFactory.selectFor(any(SendEmailRequest.class))).thenReturn(emailClient);
        when(emailClient.send(any(SendEmailRequest.class))).thenReturn(aSendResult().build());

        emailSender.sendEmail(aSendEmailCommand().notificationId(id).build(), null);

        verify(senderFactory).selectFor(requestCaptor.capture());
        assertThat(requestCaptor.getValue().attachment()).isNull();
    }
}
