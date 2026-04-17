CREATE TABLE monitoring.box_script_update_command
(
    id             UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    box_id         UUID                NOT NULL REFERENCES public.boxes (id) ON DELETE CASCADE,
    target_version VARCHAR(64)         NOT NULL,
    artifact_url   TEXT                NOT NULL,
    sha256         VARCHAR(128)        NOT NULL,
    status         VARCHAR(20)         NOT NULL,
    error_message  TEXT NULL,
    created_at     TIMESTAMPTZ         NOT NULL DEFAULT (now()),
    completed_at   TIMESTAMPTZ NULL,
    CONSTRAINT chk_box_script_update_command_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_box_script_update_cmd_box_pending
    ON monitoring.box_script_update_command (box_id, created_at DESC)
    WHERE status = 'PENDING';
