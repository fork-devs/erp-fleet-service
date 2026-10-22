CREATE TABLE IF NOT EXISTS customers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    full_name         VARCHAR(255) NOT NULL,
    email             VARCHAR(255),
    phone             VARCHAR(20),
    doc_type          VARCHAR(10) NOT NULL,
    doc_number        VARCHAR(20) NOT NULL,
    validation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_customer_tenant_doc UNIQUE (tenant_id, doc_number)
);

-- La Row-Level Security se HABILITA y se FUERZA. FORCE por si solo no la
-- habilita: solo obliga a que la politica se aplique tambien al propietario de
-- la tabla, que es el rol que la eludia. Habilitarla y forzarla son dos
-- sentencias distintas, y hacen falta las dos.
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers FORCE ROW LEVEL SECURITY;

-- El contexto de taller se lee SIN el flag "missing_ok": si no esta
-- establecido, la consulta falla de forma explicita en lugar de devolver
-- cero filas en silencio. El aislamiento no se relaja calladamente.
DROP POLICY IF EXISTS customer_tenant_isolation ON customers;

CREATE POLICY customer_tenant_isolation ON customers
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);
