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

-- La Row-Level Security se HABILITA y se FUERZA. FORCE por si solo no la
-- habilita: solo obliga a que la politica se aplique tambien al propietario de
-- la tabla, que es el rol que la eludia. Habilitarla y forzarla son dos
-- sentencias distintas, y hacen falta las dos.
ALTER TABLE vehicles ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicles FORCE ROW LEVEL SECURITY;

-- El contexto de taller se lee SIN el flag "missing_ok": si no esta
-- establecido, la consulta falla de forma explicita en lugar de devolver
-- cero filas en silencio. El aislamiento no se relaja calladamente.
DROP POLICY IF EXISTS vehicle_tenant_isolation ON vehicles;

CREATE POLICY vehicle_tenant_isolation ON vehicles
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);
