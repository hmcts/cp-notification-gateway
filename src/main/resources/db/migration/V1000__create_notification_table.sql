-- NG-S01 / AC-001 — notification view table (direct-to-DB; no eventstore, no Liquibase).
-- Versioned at V1000 to sit clear of cp-task-manager's own migrations (db/taskmanager V1, V2),
-- which are merged into this same Flyway run by TaskManagerFlywayAutoConfiguration (AC-001a).
CREATE TABLE notification (
    notification_id   UUID PRIMARY KEY,
    notification_type TEXT NOT NULL DEFAULT 'EMAIL',
    status            TEXT NOT NULL,
    send_to_address   TEXT,
    status_code       INT,
    error_message     TEXT,
    client_context    TEXT,
    -- Result-event routing target: the inbound ASB `ReplyTo` message property, persisted on ingest
    -- and read back at the terminal hop (FR-007) to route notification-sent/-failed. Nullable —
    -- absent for the fire-and-forget mi-reportdata MVP. NOT threaded through job_data (see NG-S03).
    result_queue      TEXT,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL
);
