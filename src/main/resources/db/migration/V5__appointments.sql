-- =============================================================================
-- ERP FLEET SERVICE - V5: WORKSHOP APPOINTMENTS TABLE & RLS
-- =============================================================================

CREATE TABLE IF NOT EXISTS appointments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    branch_id           UUID NOT NULL,
    customer_id         UUID NOT NULL,
    vehicle_id          UUID NOT NULL,
    scheduled_start     TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_end       TIMESTAMP WITH TIME ZONE NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    notes               VARCHAR(500),
    cancellation_reason VARCHAR(255),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_appointment_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointment_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_appointments_tenant_branch ON appointments (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_appointments_tenant_vehicle_dates ON appointments (tenant_id, vehicle_id, scheduled_start, scheduled_end);
CREATE INDEX IF NOT EXISTS idx_appointments_tenant_customer ON appointments (tenant_id, customer_id);

-- Enforce Row-Level Security
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS appointment_tenant_isolation ON appointments;
CREATE POLICY appointment_tenant_isolation ON appointments
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

-- Grant permissions to execution roles if present
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'erp_fleet_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE appointments TO erp_fleet_app;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'erp_fleet_platform') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE appointments TO erp_fleet_platform;
    END IF;
END $$;
