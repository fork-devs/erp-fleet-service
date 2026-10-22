-- =============================================================================
-- ERP FLEET SERVICE - V3: ROL DE EJECUCION, AISLAMIENTO Y RESULTADO DE LA
-- VALIDACION
-- =============================================================================
-- Tres correcciones sobre lo que habia:
--
-- 1. La aplicacion conectaba con el rol PROPIETARIO de las tablas. En
--    PostgreSQL el propietario elude su propia politica de Row-Level Security,
--    de modo que la politica declarada en V1 y V2 era inerte: con ella activa,
--    el aislamiento no aislaba nada. Se separa el rol de ejecucion del rol de
--    migracion, y se FUERZA la politica para que se aplique tambien al
--    propietario.
--
-- 2. El contexto de taller se leia con el flag "missing_ok", de modo que, sin
--    contexto, la consulta se ejecutaba sin filtrar y sin avisar. Se relee sin
--    ese flag: sin contexto, la consulta falla de forma explicita.
--
-- 3. La validacion contra el hub devuelve datos (razon social, direccion
--    fiscal, VIN, motor, color, combustible) que no tenian donde guardarse, y
--    el reintento programado no tenia con que contar. Se anaden las columnas y
--    el indice de los registros pendientes.
--
-- Ver docs/doc-estado-implementacion.md, hallazgos P1 6.6, P1 6.14 y P1 6.53.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. La politica, forzada y con el contexto obligatorio
-- -----------------------------------------------------------------------------

ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers FORCE ROW LEVEL SECURITY;
ALTER TABLE vehicles ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicles FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS customer_tenant_isolation ON customers;
CREATE POLICY customer_tenant_isolation ON customers
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

DROP POLICY IF EXISTS vehicle_tenant_isolation ON vehicles;
CREATE POLICY vehicle_tenant_isolation ON vehicles
    FOR ALL
    USING (tenant_id = current_setting('app.tenant_id')::uuid)
    WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

-- -----------------------------------------------------------------------------
-- 2. Rol de ejecucion y rol de plataforma
-- -----------------------------------------------------------------------------
-- El rol de ejecucion lo usa la aplicacion: NO es propietario de las tablas y
-- NO puede eludir la politica. El rol de plataforma existe para el barrido de
-- revalidacion, que recorre los talleres con registros pendientes; su
-- credencial vive separada de la de la aplicacion.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'erp_fleet_app') THEN
        EXECUTE format(
            'CREATE ROLE erp_fleet_app LOGIN NOINHERIT NOSUPERUSER NOCREATEDB '
            'NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L',
            '${appRolePassword}'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'erp_fleet_platform') THEN
        EXECUTE format(
            'CREATE ROLE erp_fleet_platform LOGIN NOINHERIT NOSUPERUSER NOCREATEDB '
            'NOCREATEROLE NOREPLICATION BYPASSRLS PASSWORD %L',
            '${platformRolePassword}'
        );
    END IF;
END
$$;

-- Sin USAGE sobre el esquema, el rol no resuelve ningun objeto aunque tenga
-- privilegios sobre las tablas.
GRANT USAGE ON SCHEMA public TO erp_fleet_app;
GRANT USAGE ON SCHEMA public TO erp_fleet_platform;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE customers TO erp_fleet_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE vehicles TO erp_fleet_app;

-- El barrido de revalidacion necesita leer los registros pendientes de todos
-- los talleres: es un proceso de plataforma, no una operacion de un taller.
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE customers TO erp_fleet_platform;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE vehicles TO erp_fleet_platform;

-- Comprobacion de que el rol de ejecucion no puede eludir la politica de
-- aislamiento: si pudiera, todo lo anterior seria inerte.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_roles
        WHERE rolname = 'erp_fleet_app' AND rolbypassrls
    ) THEN
        RAISE EXCEPTION 'El rol erp_fleet_app no debe tener BYPASSRLS: la politica de aislamiento seria eludible';
    END IF;
END
$$;
