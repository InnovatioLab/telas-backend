ALTER TABLE clients
    ADD COLUMN inactive_by_client_id UUID REFERENCES clients (id);

CREATE INDEX idx_clients_inactive_by ON clients (inactive_by_client_id);
