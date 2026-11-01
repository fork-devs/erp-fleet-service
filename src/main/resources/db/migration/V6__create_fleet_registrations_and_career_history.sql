-- =============================================================================
-- ERP FLEET SERVICE - V6: CUSTOMER & EMPLOYEE REGISTRATIONS AND CAREER HISTORY
-- =============================================================================

-- 1. Customer Registrations (Branch linking)
CREATE TABLE IF NOT EXISTS customer_registrations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    branch_id           UUID NOT NULL,
    customer_id         UUID NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_cust_reg_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_cust_reg_tenant_branch ON customer_registrations (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_cust_reg_tenant_customer ON customer_registrations (tenant_id, customer_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_cust_reg_tenant_branch_cust_active
    ON customer_registrations (tenant_id, branch_id, customer_id)
    WHERE status = 'ACTIVE';

-- Enforce Row-Level Security on customer_registrations
ALTER TABLE customer_registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_registrations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS customer_registrations_tenant_isolation ON customer_registrations;
CREATE POLICY customer_registrations_tenant_isolation ON customer_registrations
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

-- 2. Employee Registrations (Hierarchical Staff Onboarding per Branch)
CREATE TABLE IF NOT EXISTS employee_registrations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    branch_id           UUID NOT NULL,
    employee_id         UUID NOT NULL,
    speciality          VARCHAR(100) NOT NULL,
    speciality_name     VARCHAR(255) NOT NULL,
    salary              NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    role                VARCHAR(50) NOT NULL DEFAULT 'ROLE_EMPLOYEE',
    status              VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    rejection_reason    VARCHAR(500),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    approved_at         TIMESTAMP WITH TIME ZONE,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT chk_emp_reg_status CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL', 'REJECTED', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_emp_reg_tenant_branch ON employee_registrations (tenant_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_emp_reg_tenant_employee ON employee_registrations (tenant_id, employee_id);
CREATE INDEX IF NOT EXISTS idx_emp_reg_tenant_status ON employee_registrations (tenant_id, status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_emp_reg_tenant_branch_emp_active
    ON employee_registrations (tenant_id, branch_id, employee_id)
    WHERE status IN ('ACTIVE', 'PENDING_APPROVAL');

-- Enforce Row-Level Security on employee_registrations
ALTER TABLE employee_registrations ENABLE ROW LEVEL SECURITY;
ALTER TABLE employee_registrations FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS employee_registrations_tenant_isolation ON employee_registrations;
CREATE POLICY employee_registrations_tenant_isolation ON employee_registrations
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

-- 3. Staff Career History (Chronological Auditing)
CREATE TABLE IF NOT EXISTS staff_career_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    branch_id           UUID NOT NULL,
    employee_id         UUID NOT NULL,
    speciality          VARCHAR(100) NOT NULL,
    speciality_name     VARCHAR(255) NOT NULL,
    salary              NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    role                VARCHAR(50) NOT NULL DEFAULT 'ROLE_EMPLOYEE',
    status              VARCHAR(30) NOT NULL,
    change_reason       VARCHAR(255),
    changed_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_staff_history_tenant_branch_emp ON staff_career_history (tenant_id, branch_id, employee_id, changed_at ASC);

-- Enforce Row-Level Security on staff_career_history
ALTER TABLE staff_career_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE staff_career_history FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS staff_career_history_tenant_isolation ON staff_career_history;
CREATE POLICY staff_career_history_tenant_isolation ON staff_career_history
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

-- 4. Grant permissions to application and platform roles if present
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'erp_fleet_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE customer_registrations TO erp_fleet_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE employee_registrations TO erp_fleet_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE staff_career_history TO erp_fleet_app;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'erp_fleet_platform') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE customer_registrations TO erp_fleet_platform;
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE employee_registrations TO erp_fleet_platform;
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE staff_career_history TO erp_fleet_platform;
    END IF;
END $$;
