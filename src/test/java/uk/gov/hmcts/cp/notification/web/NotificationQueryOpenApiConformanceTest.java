package uk.gov.hmcts.cp.notification.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.hmcts.cp.notification.service.NotificationQueryService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.cp.notification.integration.testdata.NotificationViewFactory.aNotificationView;

@WebMvcTest(NotificationQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class NotificationQueryOpenApiConformanceTest {

    private static final String SPEC = "contracts/openapi/notification-gateway.openapi.yaml";
    private static final UUID NOTIFICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationQueryService queryService;

    @Test
    void the_lookup_response_conforms_to_the_published_contract() throws Exception {
        when(queryService.findById(NOTIFICATION_ID)).thenReturn(Optional.of(aNotificationView().build()));

        mockMvc.perform(get("/notifications/{id}", NOTIFICATION_ID))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void the_not_found_response_conforms_to_the_published_contract() throws Exception {
        when(queryService.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/notifications/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void the_search_response_conforms_to_the_published_contract() throws Exception {
        when(queryService.search(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(aNotificationView().build())));

        mockMvc.perform(get("/notifications")
                        .param("status", "SENT")
                        .param("createdFrom", "2026-07-01T00:00:00Z")
                        .param("createdTo", "2026-07-02T00:00:00Z")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SPEC));
    }
}
