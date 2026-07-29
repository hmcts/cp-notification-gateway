package uk.gov.hmcts.cp.notification.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.hmcts.cp.notification.persistence.NotificationEntity;
import uk.gov.hmcts.cp.notification.persistence.NotificationRepository;
import uk.gov.hmcts.cp.notification.time.Clock;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationStatusService {
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private static final Logger LOG = LoggerFactory.getLogger(NotificationStatusService.class);

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Transactional
    public void markSent(final UUID notificationId) {
        final NotificationEntity notification = getById(notificationId);
        notification.setStatus(STATUS_SENT);
        notification.setUpdatedAt(now());
        notificationRepository.save(notification);
        LOG.info("Notification {} marked SENT", notificationId);
    }

    @Transactional
    public void markFailed(final UUID notificationId, final Integer statusCode, final String errorMessage) {
        final NotificationEntity notification = getById(notificationId);
        notification.setStatus(STATUS_FAILED);
        notification.setStatusCode(statusCode);
        notification.setErrorMessage(errorMessage);
        notification.setUpdatedAt(now());
        notificationRepository.save(notification);
        LOG.warn("Notification {} marked FAILED (statusCode={})", notificationId, statusCode);
    }

    private NotificationEntity getById(final UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException(
                        "No notification row for id " + notificationId));
    }

    private OffsetDateTime now() {
        return clock.offsetDateTime();
    }
}
