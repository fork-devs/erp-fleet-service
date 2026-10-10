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

ALTER TABLE customers ENABLE ROW LEVEL SECURITY;

CREATE POLICY customer_tenant_isolation ON customers
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::uuid);
