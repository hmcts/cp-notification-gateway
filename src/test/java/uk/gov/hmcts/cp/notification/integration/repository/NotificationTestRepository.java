package uk.gov.hmcts.cp.notification.integration.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import uk.gov.hmcts.cp.notification.persistence.NotificationEntity;
import uk.gov.hmcts.cp.notification.persistence.NotificationRepository;

import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationTestRepository {

    private final NotificationRepository notifications;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public NotificationTestRepository(final NotificationRepository notifications,
                                      final JdbcTemplate jdbcTemplate,
                                      final PlatformTransactionManager transactionManager) {
        this.notifications = notifications;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public NotificationEntity save(final NotificationEntity entity) {
        return notifications.saveAndFlush(entity);
    }

    public Optional<NotificationEntity> findById(final UUID id) {
        return notifications.findById(id);
    }

    public long count() {
        return notifications.count();
    }

    public void deleteAll() {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.update("DELETE FROM notification"));
    }
}
