package uk.gov.hmcts.cp.notification.integration.stubs;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import uk.gov.hmcts.cp.notification.integration.stubs.support.WireMockSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Base64.getMimeDecoder;
import static java.util.Map.of;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cp.notification.integration.Fixtures.load;

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
                .willReturn(jsonResponse(201, "fixtures/gov-notify/send-email-success.json", externalReference)));
        return this;
    }

    public GovUkNotifyStubService getNotificationStatusWillReturnSuccess(final String externalReference) {
        WireMockSupport.wiremockServer().stubFor(get(urlPathEqualTo(STATUS_PATH_PREFIX + externalReference))
                .willReturn(jsonResponse(200, "fixtures/gov-notify/status-delivered.json", externalReference)));
        return this;
    }

    public GovUkNotifyStubService getNotificationStatusWillReturnPermanentFailure(final String externalReference) {
        WireMockSupport.wiremockServer().stubFor(get(urlPathEqualTo(STATUS_PATH_PREFIX + externalReference))
                .atPriority(1)
                .willReturn(jsonResponse(200, "fixtures/gov-notify/status-permanent-failure.json", externalReference)));
        return this;
    }

    public GovUkNotifyStubService sendEmailRequestMatches(final String expectedRequestJson, final byte[] expectedAttachment) {
        final List<LoggedRequest> requests = WireMockSupport.wiremockServer()
                .findAll(postRequestedFor(urlPathEqualTo(SEND_EMAIL_PATH)));
        assertThat(requests).as("exactly one send-email request reaches Gov.UK Notify").hasSize(1);

        final LoggedRequest request = requests.get(0);
        assertThat(request.getHeader("Authorization")).as("JWT bearer auth").matches("Bearer .+");

        final String body = request.getBodyAsString();
        assertThatJson(body).as("send-email request payload matches the expected contract").isEqualTo(expectedRequestJson);

        final String encodedAttachment = MAPPER.readTree(body).path("personalisation").path("material_url").path("file").asString();
        assertThat(getMimeDecoder().decode(encodedAttachment))
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
                .withBody(load(fixture, of("externalReference", externalReference)));
    }
}
