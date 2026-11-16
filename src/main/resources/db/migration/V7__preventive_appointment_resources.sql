CREATE TABLE workshop_bays (
    tenant_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    number INTEGER NOT NULL CHECK (number > 0),
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (tenant_id, branch_id, number)
);

ALTER TABLE workshop_bays ENABLE ROW LEVEL SECURITY;
ALTER TABLE workshop_bays FORCE ROW LEVEL SECURITY;
CREATE POLICY workshop_bays_tenant ON workshop_bays
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

ALTER TABLE appointments
    ADD COLUMN bay_number INTEGER,
    ADD COLUMN technician_id UUID,
    ADD CONSTRAINT appointment_resources_pair CHECK ((bay_number IS NULL) = (technician_id IS NULL)),
    ADD CONSTRAINT appointment_bay_fk FOREIGN KEY (tenant_id, branch_id, bay_number)
        REFERENCES workshop_bays (tenant_id, branch_id, number);

CREATE INDEX idx_appointments_bay_dates ON appointments (tenant_id, branch_id, bay_number, scheduled_start, scheduled_end)
    WHERE bay_number IS NOT NULL;
CREATE INDEX idx_appointments_technician_dates ON appointments (tenant_id, technician_id, scheduled_start, scheduled_end)
    WHERE technician_id IS NOT NULL;

CREATE TABLE appointment_email_outbox (
    appointment_id UUID PRIMARY KEY REFERENCES appointments (id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    recipient VARCHAR(254) NOT NULL,
    scheduled_start TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ,
    stopped_at TIMESTAMPTZ
);

ALTER TABLE appointment_email_outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointment_email_outbox FORCE ROW LEVEL SECURITY;
CREATE POLICY appointment_email_tenant ON appointment_email_outbox
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

CREATE INDEX idx_appointment_email_pending ON appointment_email_outbox (next_attempt)
    WHERE sent_at IS NULL AND stopped_at IS NULL;

GRANT SELECT, INSERT, UPDATE ON workshop_bays TO erp_fleet_app;
GRANT INSERT ON appointment_email_outbox TO erp_fleet_app;
GRANT SELECT, UPDATE ON appointment_email_outbox TO erp_fleet_platform;
