CREATE TABLE IF NOT EXISTS vehicles (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    customer_id       UUID NOT NULL,
    plate             VARCHAR(10) NOT NULL,
    brand             VARCHAR(100),
    model             VARCHAR(100),
    year              INTEGER,
    vin               VARCHAR(50),
    validation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_vehicle_tenant_plate UNIQUE (tenant_id, plate)
);

ALTER TABLE vehicles ENABLE ROW LEVEL SECURITY;

CREATE POLICY vehicle_tenant_isolation ON vehicles
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::uuid);
