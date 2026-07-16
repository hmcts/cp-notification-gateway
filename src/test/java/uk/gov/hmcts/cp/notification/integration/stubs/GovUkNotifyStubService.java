package uk.gov.hmcts.cp.notification.integration.stubs;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import uk.gov.hmcts.cp.notification.command.SendEmailCommand;
import uk.gov.hmcts.cp.notification.integration.Fixtures;
import uk.gov.hmcts.cp.notification.integration.stubs.support.WireMockSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

public final class GovUkNotifyStubService {
    private static final String SEND_EMAIL_PATH = "/v2/notifications/email";
    private static final String STATUS_PATH_PREFIX = "/v2/notifications/";

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private GovUkNotifyStubService() {
    }

    public static GovUkNotifyStubService aGovUkNotifyService() {
        return new GovUkNotifyStubService();
    }

    public static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("cp.notification.govnotify.base-url", WireMockSupport::baseUrl);

        registry.add("cp.notification.govnotify.api-key",
                () -> "cpngtest-00000000-0000-0000-0000-000000000000-11111111-1111-1111-1111-111111111111");
    }

    public GovUkNotifyStubService sendEmailNotificationWillReturnSuccess(final String externalReference) {
        WireMockSupport.wiremockServer().stubFor(post(urlPathEqualTo(SEND_EMAIL_PATH))
                .willReturn(jsonResponse(201, "gov-notify/send-email-success.json", externalReference)));
        return this;
    }

    public GovUkNotifyStubService getNotificationStatusWillReturnSuccess(final String externalReference) {
        WireMockSupport.wiremockServer().stubFor(get(urlPathEqualTo(STATUS_PATH_PREFIX + externalReference))
                .willReturn(jsonResponse(200, "gov-notify/status-delivered.json", externalReference)));
        return this;
    }

    public GovUkNotifyStubService sendEmailWasCalledWith(final SendEmailCommand command, final byte[] expectedAttachment) {
        final List<LoggedRequest> requests = WireMockSupport.wiremockServer()
                .findAll(postRequestedFor(urlPathEqualTo(SEND_EMAIL_PATH)));
        assertThat(requests).as("exactly one send-email request reaches Gov.UK Notify").hasSize(1);

        final LoggedRequest request = requests.get(0);
        assertThat(request.getHeader("Authorization")).as("JWT bearer auth").matches("Bearer .+");

        final JsonNode body = MAPPER.readTree(request.getBodyAsString());
        assertThat(body.path("email_address").asString()).isEqualTo(command.sendToAddress());
        assertThat(body.path("template_id").asString()).isEqualTo(command.templateId().toString());
        assertThat(body.path("reference").asString()).isEqualTo(command.notificationId().toString());
        assertThat(body.has("email_reply_to_id")).as("no reply-to sent when the command carries none").isFalse();

        final JsonNode materialUrl = body.path("personalisation").path("material_url");
        assertThat(materialUrl.has("file")).as("attachment carried under personalisation.material_url").isTrue();
        assertThat(materialUrl.path("is_csv").asBoolean()).isTrue();
        assertThat(Base64.getMimeDecoder().decode(materialUrl.path("file").asString()))
                .as("attachment bytes delivered to Gov.UK Notify").isEqualTo(expectedAttachment);
        return this;
    }

    public GovUkNotifyStubService deliveryStatusWasPolledFor(final String externalReference) {
        WireMockSupport.wiremockServer().verify(getRequestedFor(urlPathEqualTo(STATUS_PATH_PREFIX + externalReference))
                .withHeader("Authorization", matching("Bearer .+")));
        return this;
    }

    public GovUkNotifyStubService emailNotificationWasNotSent() {
        assertThat(WireMockSupport.wiremockServer().findAll(postRequestedFor(urlPathEqualTo(SEND_EMAIL_PATH))))
                .as("no send-email request should have reached Gov.UK Notify")
                .isEmpty();
        return this;
    }

    private static ResponseDefinitionBuilder jsonResponse(
            final int status, final String fixture, final String externalReference) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(Fixtures.load(fixture, Map.of("externalReference", externalReference)));
    }
}
