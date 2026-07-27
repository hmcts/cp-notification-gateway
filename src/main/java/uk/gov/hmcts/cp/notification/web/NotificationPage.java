package uk.gov.hmcts.cp.notification.web;

import lombok.Builder;

import java.util.List;
import org.springframework.data.domain.Page;

@Builder
public record NotificationPage(
        List<NotificationView> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static NotificationPage from(final Page<NotificationView> page) {
        return NotificationPage.builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
