package uk.gov.hmcts.cp.notification.web;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.hmcts.cp.notification.service.NotificationQueryService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.cp.notification.integration.Fixtures.load;
import static uk.gov.hmcts.cp.notification.integration.testdata.NotificationViewFactory.aNotificationView;

@WebMvcTest(NotificationQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class NotificationQueryControllerIntegrationTest {

    private static final UUID NOTIFICATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationQueryService queryService;

    @Nested
    class LookupById {

        @Test
        void returns_all_columns_of_the_notification_for_a_persisted_id() throws Exception {
            when(queryService.findById(NOTIFICATION_ID)).thenReturn(Optional.of(aNotificationView().build()));

            final String body = mockMvc.perform(get("/notifications/{id}", NOTIFICATION_ID))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThatJson(body).isEqualTo(load("fixtures/query/notification-by-id.json"));
        }

        @Test
        void returns_not_found_when_no_notification_matches_the_id() throws Exception {
            when(queryService.findById(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/notifications/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Search {

        @Test
        void returns_matching_notifications_delegating_the_filters_and_paging_to_the_query_service() throws Exception {
            when(queryService.search(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(aNotificationView().build())));

            final String body = mockMvc.perform(get("/notifications")
                            .param("status", "FAILED")
                            .param("createdFrom", "2026-07-01T00:00:00Z")
                            .param("createdTo", "2026-07-02T00:00:00Z")
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            final ArgumentCaptor<String> statusFilter = ArgumentCaptor.forClass(String.class);
            final ArgumentCaptor<OffsetDateTime> createdFrom = ArgumentCaptor.forClass(OffsetDateTime.class);
            final ArgumentCaptor<OffsetDateTime> createdTo = ArgumentCaptor.forClass(OffsetDateTime.class);
            final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            org.mockito.Mockito.verify(queryService)
                    .search(statusFilter.capture(), createdFrom.capture(), createdTo.capture(), pageable.capture());

            assertThat(statusFilter.getValue()).isEqualTo("FAILED");
            assertThat(createdFrom.getValue()).isEqualTo(OffsetDateTime.parse("2026-07-01T00:00:00Z"));
            assertThat(createdTo.getValue()).isEqualTo(OffsetDateTime.parse("2026-07-02T00:00:00Z"));
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
            assertThatJson(body).node("content[0].notificationId").isEqualTo(NOTIFICATION_ID.toString());
        }
    }

    @Nested
    class ReadOnly {

        @Test
        void rejects_write_requests_against_the_query_api() throws Exception {
            mockMvc.perform(post("/notifications")).andExpect(status().isMethodNotAllowed());
            mockMvc.perform(put("/notifications/{id}", NOTIFICATION_ID)).andExpect(status().isMethodNotAllowed());
            mockMvc.perform(patch("/notifications/{id}", NOTIFICATION_ID)).andExpect(status().isMethodNotAllowed());
            mockMvc.perform(delete("/notifications/{id}", NOTIFICATION_ID)).andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(queryService);
        }
    }

    @Nested
    class SearchParameterValidation {

        @Test
        void rejects_a_negative_page_index() throws Exception {
            mockMvc.perform(get("/notifications").param("page", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejects_a_page_size_above_the_maximum() throws Exception {
            mockMvc.perform(get("/notifications").param("size", "201"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejects_a_malformed_created_from_timestamp() throws Exception {
            mockMvc.perform(get("/notifications").param("createdFrom", "not-a-date"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejects_an_unknown_status_value() throws Exception {
            mockMvc.perform(get("/notifications").param("status", "BANANA"))
                    .andExpect(status().isBadRequest());
        }
    }
}
