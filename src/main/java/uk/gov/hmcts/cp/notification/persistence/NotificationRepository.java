package uk.gov.hmcts.cp.notification.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    // Nullable filters via coalesce against the NOT NULL status/created_at columns: each bind parameter
    // is used once in a typed context, avoiding Postgres' "could not determine data type" on a bare
    // "(:param is null ...)" guard where the null-check placeholder has no type context.
    @Query("select n from NotificationEntity n where "
            + "n.status = coalesce(:status, n.status) and "
            + "n.createdAt >= coalesce(:createdFrom, n.createdAt) and "
            + "n.createdAt <= coalesce(:createdTo, n.createdAt)")
    Page<NotificationEntity> search(@Param("status") String status,
                                    @Param("createdFrom") OffsetDateTime createdFrom,
                                    @Param("createdTo") OffsetDateTime createdTo,
                                    Pageable pageable);
}
