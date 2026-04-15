CREATE TABLE client_granted_permissions (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES clients (id) ON DELETE CASCADE,
    permission_code VARCHAR(80) NOT NULL,
    granted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    granted_by_client_id UUID REFERENCES clients (id) ON DELETE SET NULL,
    CONSTRAINT uq_client_permission UNIQUE (client_id, permission_code)
);

CREATE INDEX idx_client_granted_permissions_client ON client_granted_permissions (client_id);

INSERT INTO client_granted_permissions (id, client_id, permission_code, granted_at, granted_by_client_id)
SELECT gen_random_uuid(), id, 'MONITORING_LOGS_VIEW', NOW() AT TIME ZONE 'UTC', NULL FROM clients WHERE role = 'ADMIN';

INSERT INTO client_granted_permissions (id, client_id, permission_code, granted_at, granted_by_client_id)
SELECT gen_random_uuid(), id, 'MONITORING_TESTING_VIEW', NOW() AT TIME ZONE 'UTC', NULL FROM clients WHERE role = 'ADMIN';

INSERT INTO client_granted_permissions (id, client_id, permission_code, granted_at, granted_by_client_id)
SELECT gen_random_uuid(), id, 'MONITORING_TESTING_EXECUTE', NOW() AT TIME ZONE 'UTC', NULL FROM clients WHERE role = 'ADMIN';

INSERT INTO client_granted_permissions (id, client_id, permission_code, granted_at, granted_by_client_id)
SELECT gen_random_uuid(), id, 'MONITORING_SMART_PLUG_ADMIN', NOW() AT TIME ZONE 'UTC', NULL FROM clients WHERE role = 'ADMIN';
