package uk.gov.hmcts.cp.notification.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.cp.notification.service.NotificationQueryService;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/notifications")
public class NotificationQueryController {

    private final NotificationQueryService queryService;

    public NotificationQueryController(final NotificationQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationView> getNotificationById(@PathVariable final UUID notificationId) {
        return queryService.findById(notificationId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public NotificationPage searchNotifications(
            @RequestParam(required = false)
            @Pattern(regexp = "QUEUED|SENT|FAILED") final String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime createdTo,
            @RequestParam(defaultValue = "0") @Min(0) final int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) final int size) {
        return NotificationPage.from(
                queryService.search(status, createdFrom, createdTo, PageRequest.of(page, size)));
    }
}
