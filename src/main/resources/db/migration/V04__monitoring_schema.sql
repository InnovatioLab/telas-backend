CREATE SCHEMA IF NOT EXISTS monitoring;

CREATE TABLE monitoring.box_heartbeat
(
    id               UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    box_id           UUID                NOT NULL UNIQUE REFERENCES public.boxes (id) ON DELETE CASCADE,
    last_seen_at     TIMESTAMPTZ         NOT NULL,
    reported_version VARCHAR(64) NULL,
    metadata_json    JSONB NULL,
    updated_at       TIMESTAMPTZ         NOT NULL DEFAULT (now())
);

CREATE INDEX idx_box_heartbeat_last_seen ON monitoring.box_heartbeat (last_seen_at);

CREATE TABLE monitoring.check_definitions
(
    id               UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    check_type       VARCHAR(50)         NOT NULL,
    interval_seconds INTEGER             NOT NULL CHECK (interval_seconds > 0),
    enabled          BOOLEAN             NOT NULL DEFAULT TRUE,
    box_id           UUID NULL REFERENCES public.boxes (id) ON DELETE CASCADE,
    monitor_id       UUID NULL REFERENCES public.monitors (id) ON DELETE CASCADE,
    params_json      JSONB NULL,
    created_at       TIMESTAMPTZ         NOT NULL DEFAULT (now()),
    CONSTRAINT chk_check_definitions_type CHECK (check_type IN (
        'KASA_PLUG', 'HEARTBEAT_STALE', 'BOX_TELEMETRY'
        ))
);

CREATE INDEX idx_check_definitions_enabled ON monitoring.check_definitions (enabled) WHERE enabled = TRUE;

CREATE TABLE monitoring.check_runs
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    check_definition_id  UUID            NOT NULL REFERENCES monitoring.check_definitions (id) ON DELETE CASCADE,
    started_at           TIMESTAMPTZ     NOT NULL DEFAULT (now()),
    finished_at          TIMESTAMPTZ NULL,
    success              BOOLEAN NULL,
    error_message        TEXT NULL,
    metadata_json        JSONB NULL
);

CREATE INDEX idx_check_runs_definition_started ON monitoring.check_runs (check_definition_id, started_at DESC);

CREATE TABLE monitoring.incidents
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_type VARCHAR(50)     NOT NULL,
    severity      VARCHAR(20)     NOT NULL,
    box_id        UUID NULL REFERENCES public.boxes (id) ON DELETE SET NULL,
    monitor_id    UUID NULL REFERENCES public.monitors (id) ON DELETE SET NULL,
    opened_at     TIMESTAMPTZ     NOT NULL DEFAULT (now()),
    closed_at     TIMESTAMPTZ NULL,
    details_json  JSONB NULL,
    CONSTRAINT chk_incidents_type CHECK (incident_type IN (
        'BOX_UNREACHABLE', 'MONITOR_OFF', 'POWER_LOSS', 'DISPLAY_PIPELINE_DOWN', 'HEARTBEAT_STALE', 'OTHER'
        )),
    CONSTRAINT chk_incidents_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'))
);

CREATE INDEX idx_incidents_opened ON monitoring.incidents (opened_at DESC);
CREATE INDEX idx_incidents_box ON monitoring.incidents (box_id) WHERE box_id IS NOT NULL;
CREATE INDEX idx_incidents_monitor ON monitoring.incidents (monitor_id) WHERE monitor_id IS NOT NULL;

CREATE TABLE monitoring.application_logs
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT (now()),
    level           VARCHAR(20)     NOT NULL,
    message         TEXT            NOT NULL,
    source          VARCHAR(50)     NOT NULL,
    correlation_id  VARCHAR(64) NULL,
    stack_trace     TEXT NULL,
    endpoint        VARCHAR(255) NULL,
    client_id       UUID NULL REFERENCES public.clients (id) ON DELETE SET NULL,
    metadata_json   JSONB NULL,
    CONSTRAINT chk_application_logs_level CHECK (level IN ('TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR')),
    CONSTRAINT chk_application_logs_source CHECK (source IN ('API', 'WORKER', 'BOX'))
);

CREATE INDEX idx_application_logs_created_at ON monitoring.application_logs (created_at DESC);

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'telas_app') THEN
            CREATE ROLE telas_app NOLOGIN;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'telas_monitoring_worker') THEN
            CREATE ROLE telas_monitoring_worker NOLOGIN;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'telas_readonly') THEN
            CREATE ROLE telas_readonly NOLOGIN;
        END IF;
    END
$$;

GRANT USAGE ON SCHEMA monitoring TO telas_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA monitoring TO telas_app;

GRANT USAGE ON SCHEMA monitoring TO telas_monitoring_worker;
GRANT SELECT ON monitoring.check_definitions TO telas_monitoring_worker;
GRANT SELECT, INSERT, UPDATE ON monitoring.box_heartbeat TO telas_monitoring_worker;
GRANT SELECT, INSERT, UPDATE ON monitoring.check_runs TO telas_monitoring_worker;
GRANT SELECT, INSERT, UPDATE ON monitoring.incidents TO telas_monitoring_worker;
GRANT SELECT, INSERT ON monitoring.application_logs TO telas_monitoring_worker;

GRANT USAGE ON SCHEMA monitoring TO telas_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA monitoring TO telas_readonly;

ALTER DEFAULT PRIVILEGES IN SCHEMA monitoring GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO telas_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA monitoring GRANT SELECT ON TABLES TO telas_readonly;

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_auth_members m
                                JOIN pg_roles r ON r.oid = m.roleid
                                JOIN pg_roles u ON u.oid = m.member
                       WHERE r.rolname = 'telas_app'
                         AND u.rolname = current_user) THEN
            GRANT telas_app TO CURRENT_USER;
        END IF;
    END
$$;

COMMENT ON SCHEMA monitoring IS 'Monitoramento, incidentes e logs operacionais; roles telas_app, telas_monitoring_worker, telas_readonly.';
