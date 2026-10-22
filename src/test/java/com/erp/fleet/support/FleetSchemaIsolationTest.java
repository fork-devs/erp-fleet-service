package com.erp.fleet.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El aislamiento por taller se verifica en la base de datos, no en el codigo.
 *
 * <p>La migracion declaraba la Row-Level Security sin forzarla, y la
 * aplicacion conectaba con el rol propietario de las tablas: en PostgreSQL el
 * propietario elude su propia politica, de modo que la politica era inerte y
 * el aislamiento no aislaba nada. Estos tests comprueban las tres cosas que lo
 * hacen efectivo:
 *
 * <ul>
 *   <li>La RLS esta habilitada <strong>y forzada</strong> en las dos tablas.</li>
 *   <li>El rol de ejecucion no puede eludir la politica.</li>
 *   <li>Con el contexto de taller establecido, la politica filtra; sin el, la
 *       consulta falla en lugar de devolver filas sin filtrar.</li>
 * </ul>
 */
class FleetSchemaIsolationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void laPoliticaEstaHabilitadaYForzadaEnLasDosTablas() {
        Integer rlsActiva = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_class c "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'public' AND c.relname IN ('customers', 'vehicles') "
                        + "AND c.relrowsecurity = TRUE",
                Integer.class);
        Integer rlsForzada = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_class c "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'public' AND c.relname IN ('customers', 'vehicles') "
                        + "AND c.relforcerowsecurity = TRUE",
                Integer.class);

        assertEquals(2, rlsActiva, "La Row-Level Security debe estar habilitada en las dos tablas");
        assertEquals(2, rlsForzada, "La Row-Level Security debe estar FORZADA en las dos tablas");
    }

    @Test
    void elRolDeEjecucionNoPuedeEludirLaPolitica() {
        Boolean bypassea = jdbcTemplate.queryForObject(
                "SELECT rolbypassrls FROM pg_roles WHERE rolname = 'erp_fleet_app'",
                Boolean.class);

        assertFalse(bypassea,
                "El rol de ejecucion no debe tener BYPASSRLS: la politica seria eludible");
    }

    @Test
    void conElContextoDeTallerLaPoliticaFiltra() throws Exception {
        UUID tallerA = UUID.randomUUID();
        UUID tallerB = UUID.randomUUID();

        insertarCliente(tallerA, "doc-a");
        insertarCliente(tallerB, "doc-b");

        try (Connection connection = EmbeddedPostgresSupport.openAppConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL app.tenant_id = '" + tallerA + "'");

                int visibles = 0;
                try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM customers")) {
                    rs.next();
                    visibles = rs.getInt(1);
                }
                assertEquals(1, visibles,
                        "El rol de ejecucion solo debe ver los clientes de su propio taller");
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void sinContextoDeTallerLaConsultaFallaEnLugarDeDevolverFilas() throws Exception {
        try (Connection connection = EmbeddedPostgresSupport.openAppConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                Exception error = null;
                try {
                    statement.executeQuery("SELECT count(*) FROM customers");
                } catch (Exception e) {
                    error = e;
                }
                assertTrue(error != null,
                        "Sin contexto de taller la consulta debe fallar de forma explicita, "
                                + "no devolver cero filas en silencio");
            } finally {
                connection.rollback();
            }
        }
    }

    private void insertarCliente(UUID tenantId, String docNumber) throws Exception {
        try (Connection connection = EmbeddedPostgresSupport.openOwnerConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO customers (tenant_id, full_name, doc_type, doc_number) "
                    + "VALUES ('" + tenantId + "', 'Cliente de prueba', 'DNI', '" + docNumber + "')");
        }
    }
}
