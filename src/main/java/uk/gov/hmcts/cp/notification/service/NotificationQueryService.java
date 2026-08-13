package uk.gov.hmcts.cp.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import uk.gov.hmcts.cp.notification.persistence.NotificationEntity;
import uk.gov.hmcts.cp.notification.persistence.NotificationRepository;
import uk.gov.hmcts.cp.notification.web.NotificationView;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationQueryService {

    private final NotificationRepository notifications;

    public NotificationQueryService(final NotificationRepository notifications) {
        this.notifications = notifications;
    }

    public Optional<NotificationView> findById(final UUID notificationId) {
        return notifications.findById(notificationId).map(this::toView);
    }

    public Page<NotificationView> search(final String status,
                                         final OffsetDateTime createdFrom,
                                         final OffsetDateTime createdTo,
                                         final Pageable pageable) {
        return notifications.search(status, createdFrom, createdTo, pageable).map(this::toView);
    }

    private NotificationView toView(final NotificationEntity entity) {
        return NotificationView.builder()
                .notificationId(entity.getNotificationId())
                .notificationType(entity.getNotificationType())
                .status(entity.getStatus())
                .sendToAddress(entity.getSendToAddress())
                .statusCode(entity.getStatusCode())
                .errorMessage(entity.getErrorMessage())
                .clientContext(entity.getClientContext())
                .resultQueue(entity.getResultQueue())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
